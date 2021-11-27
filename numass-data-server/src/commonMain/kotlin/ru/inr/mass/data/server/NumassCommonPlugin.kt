package ru.inr.mass.data.server

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.PluginFactory
import space.kscience.dataforge.context.PluginTag
import space.kscience.dataforge.meta.Meta
import space.kscience.visionforge.Vision
import space.kscience.visionforge.VisionBase
import space.kscience.visionforge.VisionGroupBase
import space.kscience.visionforge.VisionPlugin
import space.kscience.visionforge.plotly.PlotlyPlugin
import kotlin.reflect.KClass

public class NumassCommonPlugin(meta: Meta) : VisionPlugin(meta) {
    override val tag: PluginTag get() = Companion.tag

    public val plotlyPlugin: PlotlyPlugin by require(PlotlyPlugin)

    override val visionSerializersModule: SerializersModule get() = numassSerializersModule

    public companion object : PluginFactory<NumassCommonPlugin> {
        override val tag: PluginTag = PluginTag("numass.common", "ru.inr.mass")
        override val type: KClass<NumassCommonPlugin> = NumassCommonPlugin::class
        override fun invoke(meta: Meta, context: Context): NumassCommonPlugin = NumassCommonPlugin()

        private val numassSerializersModule = SerializersModule {
            polymorphic(Vision::class) {
                subclass(VisionBase.serializer())
                subclass(VisionGroupBase.serializer())
                subclass(VisionOfNumassHv.serializer())
                subclass(VisionOfNumassPoint.serializer())
                subclass(VisionOfNumassHv.serializer())
                subclass(VisionOfNumassSet.serializer())
            }
        }
    }
}