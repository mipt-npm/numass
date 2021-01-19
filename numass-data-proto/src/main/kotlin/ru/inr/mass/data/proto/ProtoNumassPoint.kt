/*
 * Copyright  2018 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ru.inr.mass.data.proto

import hep.dataforge.context.Context
import hep.dataforge.io.Envelope
import hep.dataforge.io.io
import hep.dataforge.io.readEnvelopeFile
import hep.dataforge.meta.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.io.asInputStream
import kotlinx.io.readByteArray
import org.slf4j.LoggerFactory
import ru.inr.mass.data.api.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.zip.Inflater

/**
 * Protobuf based numass point
 * Created by Alexander Nozik on 09.07.2017.
 */
public class ProtoNumassPoint(
    override val meta: Meta,
    private val protoBuilder: () -> NumassProto.Point,
) : NumassPoint {

    private val proto: NumassProto.Point get() = protoBuilder()

    override val blocks: List<NumassBlock>
        get() = proto.channelsList
            .flatMap { channel ->
                channel.blocksList
                    .map { block -> ProtoBlock(channel.id.toInt(), block, this) }
                    .sortedBy { it.startTime }
            }

    override val channels: Map<Int, NumassBlock>
        get() = proto.channelsList.groupBy { it.id.toInt() }.mapValues { entry ->
            MetaBlock(entry.value.flatMap { it.blocksList }.map { ProtoBlock(entry.key, it, this) })
        }

    override val voltage: Double get() = meta["external_meta.HV1_value"].double ?: super.voltage

    override val index: Int get() = meta["external_meta.point_index"].int ?: super.index

    override val startTime: Instant
        get() = meta["start_time"].long?.let {
            Instant.ofEpochMilli(it)
        } ?: super.startTime

    override val length: Duration
        get() = meta["acquisition_time"].double?.let {
            Duration.ofMillis((it * 1000).toLong())
        } ?: super.length


    public companion object {


        /**
         * Get valid data stream utilizing compression if it is present
         */
        private fun <R> Envelope.useData(block: (InputStream) -> R): R? = when {
            data == null -> null
            meta["compression"].string == "zlib" -> {
                //TODO move to new type of data
                val inflater = Inflater()

                val array: ByteArray = data?.read {
                    readByteArray()
                } ?: ByteArray(0)

                inflater.setInput(array)
                val bos = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (!inflater.finished()) {
                    val size = inflater.inflate(buffer)
                    bos.write(buffer, 0, size)
                }
                val unzippeddata = bos.toByteArray()
                inflater.end()
                ByteArrayInputStream(unzippeddata).use(block)
            }
            else -> {
                data?.read {
                    block(asInputStream())
                }
            }
        }

        public fun fromEnvelope(envelope: Envelope): ProtoNumassPoint? {
            val proto = envelope.useData {
                NumassProto.Point.parseFrom(it)
            }
            return proto?.let { ProtoNumassPoint(envelope.meta) { it } }
        }

        public fun fromFile(context: Context, path: Path): ProtoNumassPoint? {
            val envelope = context.io.readEnvelopeFile(path) ?: error("Envelope could not be read from $path")
            return fromEnvelope(envelope)
        }

        public fun fromFile(context: Context, path: String): ProtoNumassPoint? {
            return fromFile(context,Path.of(path))
        }

        public fun ofEpochNanos(nanos: Long): Instant {
            val seconds = Math.floorDiv(nanos, 1e9.toInt().toLong())
            val reminder = (nanos % 1e9).toInt()
            return Instant.ofEpochSecond(seconds, reminder.toLong())
        }
    }
}

public class ProtoBlock(
    override val channel: Int,
    private val block: NumassProto.Point.Channel.Block,
    public val parent: NumassPoint? = null,
) : NumassBlock {

    override val startTime: Instant
        get() = ProtoNumassPoint.ofEpochNanos(block.time)

    override val length: Duration = when {
        block.length > 0 -> Duration.ofNanos(block.length)
        parent?.meta["acquisition_time"] != null ->
            Duration.ofMillis((parent?.meta["acquisition_time"].double ?: 0.0 * 1000).toLong())
        else -> {
            LoggerFactory.getLogger(javaClass)
                .error("No length information on block. Trying to infer from first and last events")
            val times = runBlocking { events.map { it.timeOffset }.toList() }
            val nanos = (times.maxOrNull()!! - times.minOrNull()!!)
            Duration.ofNanos(nanos)
        }
    }

    override val events: Flow<NumassEvent>
        get() = if (block.hasEvents()) {
            val events = block.events
            if (events.timesCount != events.amplitudesCount) {
                LoggerFactory.getLogger(javaClass)
                    .error("The block is broken. Number of times is ${events.timesCount} and number of amplitudes is ${events.amplitudesCount}")
            }
            (0..events.timesCount).asFlow()
                .map { i -> NumassEvent(events.getAmplitudes(i).toShort(), events.getTimes(i), this) }
        } else {
            emptyFlow<NumassEvent>()
        }


    override val frames: Flow<NumassFrame>
        get() {
            val tickSize = Duration.ofNanos(block.binSize)
            return block.framesList.asFlow().map { frame ->
                val time = startTime.plusNanos(frame.time)
                val frameData = frame.data.asReadOnlyByteBuffer()
                NumassFrame(time, tickSize, frameData.asShortBuffer())
            }
        }
}