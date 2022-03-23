package ru.inr.mass.scripts

import kotlinx.coroutines.runBlocking
import kotlinx.html.h2
import kotlinx.html.p
import kotlinx.serialization.json.Json
import ru.inr.mass.data.analysis.NumassEventExtractor
import ru.inr.mass.data.analysis.amplitudeSpectrum
import ru.inr.mass.data.api.NumassFrame
import ru.inr.mass.data.api.channels
import ru.inr.mass.workspace.Numass.readPoint
import ru.inr.mass.workspace.listFrames
import space.kscience.dataforge.meta.MetaSerializer
import space.kscience.plotly.*

//fun NumassFrame.tqdcAmplitude(): Short {
//    var max = Short.MIN_VALUE
//    var min = Short.MAX_VALUE
//
//    signal.forEach { sh: Short ->
//        if (sh >= max) {
//            max = sh
//        }
//        if (sh <= min) {
//            min = sh
//        }
//    }
//
//    return (max - min).toShort()
//}

//fun Flow<NumassFrame>.tqdcAmplitudes(): List<Short> = runBlocking {
//    map { it.tqdcAmplitude() }.toList()
//}

val IntRange.center: Double get() = (endInclusive + start).toDouble() / 2.0

suspend fun main() {
    //val repo: DataTree<NumassDirectorySet> = readNumassRepository("D:\\Work\\numass-data\\")
    //val directory = readDirectory("D:\\Work\\Numass\\data\\2021_11\\Tritium_2\\set_11\\")
    val point = readPoint("D:\\Work\\Numass\\data\\2021_11\\Tritium_2\\set_11\\p0(30s)(HV1=14000)")
    val channel = point.channels[4]!!

    val binning = 16U

    val frames: List<NumassFrame> = channel.listFrames()
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
            scatter {
                name = "max"
                val spectrum = runBlocking {
                    channel.amplitudeSpectrum(NumassEventExtractor.EVENTS_ONLY)
                }.binned(binning)
                x.numbers = spectrum.keys.map { it.center }
                y.numbers = spectrum.values.map { it }
            }

            scatter {
                name = "max-min"
                val spectrum = runBlocking {
                    channel.amplitudeSpectrum(NumassEventExtractor.TQDC)
                }.binned(binning)
                x.numbers = spectrum.keys.map { it.center }
                y.numbers = spectrum.values.map { it }
            }

            scatter {
                name = "max-baseline + filter"
                val spectrum = runBlocking {
                    channel.amplitudeSpectrum(NumassEventExtractor.TQDC_V2)
                }.binned(binning)
                x.numbers = spectrum.keys.map { it.center }
                y.numbers = spectrum.values.map { it }
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