package ru.inr.mass.scripts

import kotlinx.coroutines.flow.toList
import kotlinx.html.code
import kotlinx.html.h2
import kotlinx.html.p
import kotlinx.serialization.json.Json
import ru.inr.mass.workspace.readNumassDirectory
import space.kscience.dataforge.io.JsonMetaFormat
import space.kscience.dataforge.io.toString
import space.kscience.dataforge.meta.MetaSerializer
import space.kscience.plotly.*

suspend fun main() {
    //val repo: DataTree<NumassDirectorySet> = readNumassRepository("D:\\Work\\numass-data\\")
    val directory = readNumassDirectory("D:\\Work\\numass-data\\set_3\\")
    val point = directory.points.first()

    val frames = point.frames.toList()
    Plotly.page {
        p { +"${frames.size} frames" }
        h2 { +"Random frames" }
        plot {
            val random = kotlin.random.Random(1234)

            repeat(10) {
                val frame = frames.random(random)
                scatter {
                    y.numbers = frame.signal.map { it.toUShort().toInt() - Short.MAX_VALUE }
                }
            }
        }
        h2 { +"Analysis" }
        plot {
            histogram {
                name="max"
                x.numbers = frames.map { frame -> frame.signal.maxOf {  it.toUShort().toInt() - Short.MAX_VALUE } }
            }

            histogram {
                name="max-min"
                xbins{
                    size = 2.0
                }
                x.numbers = frames.map { frame ->
                    frame.signal.maxOf {  it.toUShort().toInt() - Short.MAX_VALUE } -
                            frame.signal.minOf {  it.toUShort().toInt() - Short.MAX_VALUE }
                }
            }
        }
        h2 { +"Meta" }
        p { +Json.encodeToString(MetaSerializer, point.meta) }
    }.makeFile()
}