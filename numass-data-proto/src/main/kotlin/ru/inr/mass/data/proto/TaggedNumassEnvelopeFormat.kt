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
import hep.dataforge.io.*
import hep.dataforge.meta.Meta
import hep.dataforge.meta.get
import hep.dataforge.meta.string
import hep.dataforge.names.Name
import hep.dataforge.names.plus
import hep.dataforge.names.toName
import kotlinx.io.*
import java.util.*


internal class TaggedNumassEnvelopeFormat(private val io: IOPlugin) : EnvelopeFormat {

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

        val meta: Meta = metaFormat.readObject(input.limit(tag.metaSize.toInt()))

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

        val meta: Meta = metaFormat.readObject(input.limit(tag.metaSize.toInt()))


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

            val metaFormatName = meta["name"].string?.toName() ?: JsonMetaFormat.name
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

        override fun peekFormat(io: IOPlugin, input: Input): EnvelopeFormat? {
            return try {
                input.preview {
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


///**
// * An envelope type for legacy numass tags. Reads legacy tag and writes DF02 tags
// */
//object NumassEnvelopeType : EnvelopeFormatFactory {
//
//    override val code: Int = DefaultEnvelopeType.DEFAULT_ENVELOPE_CODE
//
//    override val name: String = "numass"
//
//    override fun description(): String = "Numass legacy envelope"
//
//    /**
//     * Read as legacy
//     */
//    override fun getReader(properties: Map<String, String>): EnvelopeReader {
//        return NumassEnvelopeReader()
//    }
//
//    /**
//     * Write as default
//     */
//    override fun getWriter(properties: Map<String, String>): EnvelopeWriter {
//        return DefaultEnvelopeWriter(this, MetaType.resolve(properties))
//    }
//
//    class LegacyTag : EnvelopeTag() {
//        override val startSequence: ByteArray
//            get() = LEGACY_START_SEQUENCE
//
//        override val endSequence: ByteArray
//            get() = LEGACY_END_SEQUENCE
//
//        /**
//         * Get the length of tag in bytes. -1 means undefined size in case tag was modified
//         *
//         * @return
//         */
//        override val length: Int
//            get() = 30
//
//        /**
//         * Read leagscy version 1 tag without leading tag head
//         *
//         * @param buffer
//         * @return
//         * @throws IOException
//         */
//        override fun readHeader(buffer: ByteBuffer): Map<String, Value> {
//            val res = HashMap<String, Value>()
//
//            val type = buffer.getInt(2)
//            res[Envelope.TYPE_PROPERTY] = Value.of(type)
//
//            val metaTypeCode = buffer.getShort(10)
//            val metaType = MetaType.resolve(metaTypeCode)
//
//            if (metaType != null) {
//                res[Envelope.META_TYPE_PROPERTY] = metaType.name.parseValue()
//            } else {
//                LoggerFactory.getLogger(EnvelopeTag::class.java).warn("Could not resolve meta type. Using default")
//            }
//
//            val metaLength = Integer.toUnsignedLong(buffer.getInt(14))
//            res[Envelope.META_LENGTH_PROPERTY] = Value.of(metaLength)
//            val dataLength = Integer.toUnsignedLong(buffer.getInt(22))
//            res[Envelope.DATA_LENGTH_PROPERTY] = Value.of(dataLength)
//            return res
//        }
//    }
//
//    private class NumassEnvelopeReader : DefaultEnvelopeReader() {
//        override fun newTag(): EnvelopeTag {
//            return LegacyTag()
//        }
//    }
//
//    companion object {
//        val INSTANCE = NumassEnvelopeType()
//
//        val LEGACY_START_SEQUENCE = byteArrayOf('#'.toByte(), '!'.toByte())
//        val LEGACY_END_SEQUENCE = byteArrayOf('!'.toByte(), '#'.toByte(), '\r'.toByte(), '\n'.toByte())
//
//        /**
//         * Replacement for standard type infer to include legacy type
//         *
//         * @param path
//         * @return
//         */
//        fun infer(path: Path): EnvelopeType? {
//            return try {
//                FileChannel.open(path, StandardOpenOption.READ).use {
//                    val buffer = it.map(FileChannel.MapMode.READ_ONLY, 0, 6)
//                    when {
//                        //TODO use templates from appropriate types
//                        buffer.get(0) == '#'.toByte() && buffer.get(1) == '!'.toByte() -> INSTANCE
//                        buffer.get(0) == '#'.toByte() && buffer.get(1) == '!'.toByte() &&
//                                buffer.get(4) == 'T'.toByte() && buffer.get(5) == 'L'.toByte() -> TaglessEnvelopeType.INSTANCE
//                        buffer.get(0) == '#'.toByte() && buffer.get(1) == '~'.toByte() -> DefaultEnvelopeType.INSTANCE
//                        else -> null
//                    }
//                }
//            } catch (ex: Exception) {
//                LoggerFactory.getLogger(EnvelopeType::class.java).warn("Could not infer envelope type of file {} due to exception: {}", path, ex)
//                null
//            }
//
//        }
//
//    }
//
//}
