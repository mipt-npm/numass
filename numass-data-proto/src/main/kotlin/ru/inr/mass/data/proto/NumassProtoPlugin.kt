package ru.inr.mass.data.proto

import hep.dataforge.context.AbstractPlugin
import hep.dataforge.context.Context
import hep.dataforge.context.PluginFactory
import hep.dataforge.context.PluginTag
import hep.dataforge.io.IOPlugin
import hep.dataforge.meta.Meta
import kotlin.reflect.KClass

public class NumassProtoPlugin : AbstractPlugin() {
    val io by require(IOPlugin)
    override val tag: PluginTag get() = Companion.tag

    public companion object : PluginFactory<NumassProtoPlugin> {
        override fun invoke(meta: Meta, context: Context): NumassProtoPlugin = NumassProtoPlugin()
        override val tag: PluginTag = PluginTag("numass-proto", group = "ru.inr.mass")
        override val type: KClass<out NumassProtoPlugin> = NumassProtoPlugin::class
    }
}