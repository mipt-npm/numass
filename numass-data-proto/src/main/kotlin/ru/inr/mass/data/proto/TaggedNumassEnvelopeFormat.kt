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

import io.ktor.utils.io.core.*
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.io.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.plus
import java.util.*


public class TaggedNumassEnvelopeFormat(private val io: IOPlugin) : EnvelopeFormat {

    private fun Tag.toBinary() = Binary(24) {
        writeRawString(START_SEQUENCE)
        writeRawString("DFNU")
        writeShort(metaFormatKey)
        writeUInt(metaSize)
        writeUInt(dataSize.toUInt())
        writeRawString(END_SEQUENCE)
    }

    override fun writeEnvelope(
        output: Output,
        envelope: Envelope,
        metaFormatFactory: MetaFormatFactory,
        formatMeta: Meta,
    ) {
        error("Don't write legacy formats")
//        val metaFormat = metaFormatFactory.invoke(formatMeta, io.context)
//        val metaBytes = metaFormat.toBinary(envelope.meta)
//        val actualSize: ULong = (envelope.data?.size ?: 0).toULong()
//        val tag = Tag(metaFormatFactory.key, metaBytes.size.toUInt() + 2u, actualSize)
//        output.writeBinary(tag.toBinary())
//        output.writeBinary(metaBytes)
//        output.writeRawString("\r\n")
//        envelope.data?.let {
//            output.writeBinary(it)
//        }
//        output.flush()
    }

    /**
     * Read an envelope from input into memory
     *
     * @param input an input to read from
     * @param formats a collection of meta formats to resolve
     */
    override fun readObject(input: Input): Envelope {
        val tag = input.readTag()

        val metaFormat = io.resolveMetaFormat(tag.metaFormatKey)
            ?: error("Meta format with key ${tag.metaFormatKey} not found")

        val meta: Meta = metaFormat.readObject(input.readBinary(tag.metaSize.toInt()))

        val data = input.readBinary(tag.dataSize.toInt())

        return SimpleEnvelope(meta, data)
    }

    override fun readPartial(input: Input): PartialEnvelope {
        val tag = input.readTag()

        val metaFormat = if (tag.metaFormatKey == 1.toShort()) {
            JsonMetaFormat
        } else {
            io.resolveMetaFormat(tag.metaFormatKey)
                ?: error("Meta format with key ${tag.metaFormatKey} not found")
        }

        val meta: Meta = metaFormat.readObject(input.readBinary(tag.metaSize.toInt()))


        return PartialEnvelope(meta, 30u + tag.metaSize, tag.dataSize)
    }

    private data class Tag(
        val metaFormatKey: Short,
        val metaSize: UInt,
        val dataSize: ULong,
    )

    override fun toMeta(): Meta = Meta {
        IOFormat.NAME_KEY put name.toString()
    }

    public companion object : EnvelopeFormatFactory {
        private const val START_SEQUENCE = "#!"
        private const val END_SEQUENCE = "!#\r\n"

        override val name: Name = super.name + "numass"

        override fun invoke(meta: Meta, context: Context): EnvelopeFormat {
            val io = context.io

            val metaFormatName = meta["name"].string?.let { Name.parse(it) } ?: JsonMetaFormat.name
            //Check if appropriate factory exists
            io.metaFormatFactories.find { it.name == metaFormatName } ?: error("Meta format could not be resolved")

            return TaggedNumassEnvelopeFormat(io)
        }

        private fun Input.readTag(): Tag {
            val start = readRawString(2)
            if (start != START_SEQUENCE) error("The input is not an envelope")
            val versionString = readRawString(4)
            val junk1 = readInt()
            val metaFormatKey = readShort()
            val junk2 = readShort()
            val metaLength = readUInt()
            val dataLength: ULong = readULong()
            val end = readRawString(4)
            if (end != END_SEQUENCE) error("The input is not an envelope")
            return Tag(metaFormatKey, metaLength, dataLength)
        }

        override fun peekFormat(io: IOPlugin, binary: Binary): EnvelopeFormat? = try {
            binary.read {
                val header = readRawString(30)
                if (header.startsWith(START_SEQUENCE) && header.endsWith(END_SEQUENCE)) {
                    TaggedNumassEnvelopeFormat(io)
                } else {
                    null
                }
            }
        } catch (ex: Exception) {
            null
        }

        private val default by lazy { invoke() }

        override fun readPartial(input: Input): PartialEnvelope =
            default.run { readPartial(input) }

        override fun writeEnvelope(
            output: Output,
            envelope: Envelope,
            metaFormatFactory: MetaFormatFactory,
            formatMeta: Meta,
        ): Unit = default.run {
            writeEnvelope(
                output,
                envelope,
                metaFormatFactory,
                formatMeta
            )
        }

        override fun readObject(input: Input): Envelope = default.readObject(input)
    }
}