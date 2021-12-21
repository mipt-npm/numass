package ru.inr.mass.scripts

import kotlinx.html.h2
import kotlinx.html.p
import kotlinx.serialization.json.Json
import ru.inr.mass.data.api.NumassFrame
import ru.inr.mass.workspace.Numass.readDirectory
import ru.inr.mass.workspace.listFrames
import space.kscience.dataforge.meta.MetaSerializer
import space.kscience.plotly.*

fun NumassFrame.tqdcAmplitude(): Short {
    var max = Short.MIN_VALUE
    var min = Short.MAX_VALUE

    signal.forEach { sh: Short ->
        if (sh >= max) {
            max = sh
        }
        if (sh <= min) {
            min = sh
        }
    }

    return (max - min).toShort()
}

suspend fun main() {
    //val repo: DataTree<NumassDirectorySet> = readNumassRepository("D:\\Work\\numass-data\\")
    val directory = readDirectory("D:\\Work\\Numass\\data\\test\\set_7")
    val point = directory.points.first()

    val frames: List<NumassFrame> = point.listFrames()
    Plotly.page {
        p { +"${frames.size} frames" }
        h2 { +"Random frames" }
        plot {
            val random = kotlin.random.Random(1234)

            repeat(10) {
                val frame = frames.random(random)
                scatter {
                    y.numbers = frame.signal.toList()
                }
            }
        }
        h2 { +"Analysis" }
        plot {
            histogram {
                name = "max"
                x.numbers = frames.map { frame -> frame.signal.maxOrNull() ?: 0 }
            }

            histogram {
                name = "max-min"
                xbins {
                    size = 2.0
                }
                x.numbers = frames.map { it.tqdcAmplitude() }
            }
        }
        h2 { +"Meta" }
        p { +Json.encodeToString(MetaSerializer, point.meta) }
    }.makeFile()


    //    val point = Numass.readPoint("D:\\Work\\Numass\\data\\test\\set_7\\p0(30s)(HV1=14000)")
//
//    Plotly.plot {
//        histogram {
//            xbins.size = 2
//            x.numbers = point.frames.map { it.tqdcAmplitude() }.toList()
//        }
//
//        histogram {
//            x.numbers = point.flowBlocks().flatMapMerge { it.frames.map { it.tqdcAmplitude() } }.toList()
//        }
//
//        histogram {
//            x.numbers = point.getChannels().values.flatMap { it.listFrames().map { it.tqdcAmplitude() } }
//        }
//    }.makeFile()
}