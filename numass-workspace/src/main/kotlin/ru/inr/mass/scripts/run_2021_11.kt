package ru.inr.mass.scripts

import kotlinx.html.h2
import kotlinx.html.p
import kotlinx.serialization.json.Json
import ru.inr.mass.workspace.Numass.readNumassDirectory
import ru.inr.mass.workspace.listFrames
import space.kscience.dataforge.meta.MetaSerializer
import space.kscience.plotly.*

suspend fun main() {
    //val repo: DataTree<NumassDirectorySet> = readNumassRepository("D:\\Work\\numass-data\\")
    val directory = readNumassDirectory("D:\\Work\\Numass\\data\\test\\set_7")
    val point = directory.points.first()

    val frames = point.listFrames()
    Plotly.page {
        p { +"${frames.size} frames" }
        h2 { +"Random frames" }
        plot {
            val random = kotlin.random.Random(1234)

            repeat(10) {
                val frame = frames.random(random)
                scatter {
                    y.numbers = frame.signal.map { (it.toUShort().toInt() - Short.MAX_VALUE).toShort() }
                }
            }
        }
        h2 { +"Analysis" }
        plot {
            histogram {
                name = "max"
                x.numbers = frames.map { frame -> frame.signal.maxOf { (it.toUShort().toInt() - Short.MAX_VALUE).toShort() } }
            }

            histogram {
                name = "max-min"
                xbins {
                    size = 2.0
                }
                x.numbers = frames.map { frame ->
                    frame.signal.maxOf { it.toUShort().toInt() - Short.MAX_VALUE } -
                            frame.signal.minOf { it.toUShort().toInt() - Short.MAX_VALUE }
                }
            }
        }
        h2 { +"Meta" }
        p { +Json.encodeToString(MetaSerializer, point.meta) }
    }.makeFile()
}