package ru.inr.mass.data.server

import kotlinx.html.dom.append
import kotlinx.html.js.*
import org.w3c.dom.Element
import space.kscience.dataforge.context.AbstractPlugin
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.PluginFactory
import space.kscience.dataforge.context.PluginTag
import space.kscience.dataforge.meta.Meta
import space.kscience.plotly.models.LineShape
import space.kscience.plotly.models.ScatterMode
import space.kscience.plotly.plot
import space.kscience.plotly.scatter
import space.kscience.visionforge.ElementVisionRenderer
import space.kscience.visionforge.Vision
import kotlin.reflect.KClass

public class NumassJsPlugin : AbstractPlugin(), ElementVisionRenderer {
    public val numassCommon: NumassCommonPlugin by require(NumassCommonPlugin)
    private val plotly = numassCommon.plotlyPlugin

    override val tag: PluginTag get() = Companion.tag

    override fun rateVision(vision: Vision): Int = when (vision) {
        is VisionOfNumassHv, is VisionOfNumassPoint, is VisionOfNumassSet -> ElementVisionRenderer.DEFAULT_RATING
        else -> ElementVisionRenderer.ZERO_RATING
    }

    override fun render(element: Element, vision: Vision, meta: Meta) {
        when (vision) {
            is VisionOfNumassHv -> element.append {
                h1 { +"HV" }
                //TODO add title
                table {
                    th {
                        td { +"Time" }
                        td { +"Value" }
                        td { +"channel" }
                    }
                    vision.forEach { entry ->
                        tr {
                            td { +entry.timestamp.toString() }
                            td { +entry.value.toString() }
                            td { +entry.channel.toString() }
                        }
                    }
                }
            }
            is VisionOfNumassPoint -> element.append {
                h1{ +"Point"}
                plot {
                    vision.spectra.forEach { (channel, spectrum) ->
                        scatter {
                            name = channel
                            mode = ScatterMode.lines
                            line {
                                shape = LineShape.hv
                            }
                            x.numbers = spectrum.keys.map { it.toInt() }
                            y.numbers = spectrum.values.map { it.toInt() }
                        }
                    }
                }
            }
            is VisionOfNumassSet -> {}
        }
    }


    public companion object : PluginFactory<NumassJsPlugin> {
        override val tag: PluginTag = PluginTag("numass.js", "ru.inr.mass")
        override val type: KClass<NumassJsPlugin> = NumassJsPlugin::class
        override fun invoke(meta: Meta, context: Context): NumassJsPlugin = NumassJsPlugin()
    }
}