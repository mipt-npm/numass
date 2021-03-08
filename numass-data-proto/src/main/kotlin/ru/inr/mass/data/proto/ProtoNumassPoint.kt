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

import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import okio.ByteString
import org.slf4j.LoggerFactory
import ru.inr.mass.data.api.*
import space.kscience.dataforge.io.Envelope
import space.kscience.dataforge.meta.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.Inflater
import kotlin.time.Duration
import kotlin.time.milliseconds
import kotlin.time.nanoseconds

/**
 * Protobuf based numass point
 * Created by Alexander Nozik on 09.07.2017.
 */
internal class ProtoNumassPoint(
    override val meta: Meta,
    private val protoBuilder: () -> Point,
) : NumassPoint {

    val point by lazy(protoBuilder)

    override fun flowBlocks() = point.channels.flatMap { channel ->
        channel.blocks
            .map { block -> ProtoNumassBlock(channel.id.toInt(), block, this) }
            .sortedBy { it.startTime }
    }.asFlow()

    override suspend fun getChannels(): Map<Int, NumassBlock> =
        point.channels.groupBy { it.id.toInt() }.mapValues { entry ->
            MetaBlock(entry.value.flatMap { it.blocks }.map { ProtoNumassBlock(entry.key, it, this) })
        }

    override val voltage: Double get() = meta["external_meta.HV1_value"].double ?: super.voltage

    override val index: Int get() = meta["external_meta.point_index"].int ?: super.index

    override val startTime: Instant
        get() = meta["start_time"].long?.let {
            Instant.fromEpochMilliseconds(it)
        } ?: Instant.DISTANT_PAST

    override suspend fun getLength(): Duration = meta["acquisition_time"].double?.let {
        (it * 1000).toLong().milliseconds
    } ?: super.getLength()

    override fun toString(): String = "ProtoNumassPoint(index = ${index}, hv = $voltage)"

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
                    readBytes()
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
                Point.ADAPTER.decode(it)
            }
            return proto?.let { ProtoNumassPoint(envelope.meta) { it } }
        }
    }
}


public class ProtoNumassBlock(
    override val channel: Int,
    private val block: Point.Channel.Block,
    private val parent: NumassPoint? = null,
) : NumassBlock {

    override val startTime: Instant
        get() {
            val nanos = block.time
            val seconds = Math.floorDiv(nanos, 1e9.toInt().toLong())
            val reminder = (nanos % 1e9).toInt()
            return Instant.fromEpochSeconds(seconds, reminder.toLong())
        }

    override suspend fun getLength(): Duration = when {
        block.length > 0 -> block.length.nanoseconds
        parent?.meta["acquisition_time"] != null ->
            (parent?.meta["acquisition_time"].double ?: 0.0 * 1000).milliseconds
        else -> {
            LoggerFactory.getLogger(javaClass)
                .error("No length information on block. Trying to infer from first and last events")
            val times = runBlocking { events.map { it.timeOffset }.toList() }
            val nanos = (times.maxOrNull()!! - times.minOrNull()!!)
            nanos.nanoseconds
        }
    }

    override val events: Flow<NumassEvent>
        get() = if (block.events != null) {
            val events = block.events
            val amplitudes = events.amplitudes
            val times = events.times

            if (times.size != amplitudes.size) {
                LoggerFactory.getLogger(javaClass)
                    .error("The block is broken. Number of times is ${times.size} and number of amplitudes is ${amplitudes.size}")
            }

            amplitudes.zip(times) { amp, time ->
                NumassEvent(amp.toShort(), time, this)
            }.asFlow()

        } else {
            emptyFlow()
        }

    private fun ByteString.toShortArray(): ShortArray {
        val shortBuffer = asByteBuffer().asShortBuffer()
        return if (shortBuffer.hasArray()) {
            shortBuffer.array()
        } else {
            ShortArray(shortBuffer.limit()) { shortBuffer.get(it) }
        }
    }

    override val frames: Flow<NumassFrame>
        get() {
            val tickSize = block.bin_size.nanoseconds
            return block.frames.asFlow().map { frame ->
                val time = startTime.plus(frame.time, DateTimeUnit.NANOSECOND)
                val frameData = frame.data_
                NumassFrame(time, tickSize, frameData.toShortArray())
            }
        }
}