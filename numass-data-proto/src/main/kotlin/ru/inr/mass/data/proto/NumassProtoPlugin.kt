package ru.inr.mass.data.proto

import space.kscience.dataforge.context.AbstractPlugin
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.PluginFactory
import space.kscience.dataforge.context.PluginTag
import space.kscience.dataforge.io.EnvelopeFormatFactory
import space.kscience.dataforge.io.IOPlugin
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name
import kotlin.reflect.KClass

public class NumassProtoPlugin : AbstractPlugin() {
    public val io: IOPlugin by require(IOPlugin)
    override val tag: PluginTag get() = Companion.tag

    override fun content(target: String): Map<Name, Any> {
        return if(target== EnvelopeFormatFactory.ENVELOPE_FORMAT_TYPE){
            mapOf(TaggedNumassEnvelopeFormat.name to TaggedNumassEnvelopeFormat)
        } else{
            super.content(target)
        }
    }

    public companion object : PluginFactory<NumassProtoPlugin> {
        override fun invoke(meta: Meta, context: Context): NumassProtoPlugin = NumassProtoPlugin()
        override val tag: PluginTag = PluginTag("numass-proto", group = "ru.inr.mass")
        override val type: KClass<out NumassProtoPlugin> = NumassProtoPlugin::class
    }
}
