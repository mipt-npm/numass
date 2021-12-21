package ru.inr.mass.workspace

import kotlinx.coroutines.runBlocking
import kotlinx.html.h1
import kotlinx.html.h2
import ru.inr.mass.data.analysis.NumassAmplitudeSpectrum
import ru.inr.mass.data.analysis.NumassEventExtractor
import ru.inr.mass.data.analysis.amplitudeSpectrum
import ru.inr.mass.data.analysis.timeHistogram
import ru.inr.mass.data.api.NumassBlock
import ru.inr.mass.data.api.NumassPoint
import ru.inr.mass.data.api.NumassSet
import ru.inr.mass.data.api.title
import ru.inr.mass.data.proto.HVData
import ru.inr.mass.data.proto.NumassDirectorySet
import space.kscience.dataforge.values.asValue
import space.kscience.dataforge.values.double
import space.kscience.kmath.histogram.UnivariateHistogram
import space.kscience.kmath.histogram.center
import space.kscience.kmath.misc.UnstableKMathAPI
import space.kscience.kmath.operations.asIterable
import space.kscience.kmath.structures.Buffer
import space.kscience.kmath.structures.DoubleBuffer
import space.kscience.plotly.*
import space.kscience.plotly.models.*
import kotlin.time.DurationUnit

/**
 * Plot a kmath histogram
 */
@OptIn(UnstableKMathAPI::class)
fun Plot.histogram(histogram: UnivariateHistogram, block: Scatter.() -> Unit = {}): Trace = scatter {
    x.numbers = histogram.bins.map { it.domain.center }
    y.numbers = histogram.bins.map { it.value }
    line.shape = LineShape.hv
    block()
}

fun Plot.histogram(
    spectrum: NumassAmplitudeSpectrum,
    binSize: UInt = 20U,
    block: Scatter.() -> Unit = {},
): Trace = scatter {
    val binned = spectrum.binned(binSize)
    x.numbers = binned.keys.map { (it.first + it.last).toDouble() / 2.0 }
    y.numbers = binned.values
    line.shape = LineShape.hv
    block()
}

/**
 * Generate a plot from hv data
 */
fun Plot.hvData(data: HVData): Trace = scatter {
    x.strings = data.map { it.timestamp.toString() }
    y.numbers = data.map { it.value }
}


fun Plotly.plotNumassBlock(
    block: NumassBlock,
    amplitudeBinSize: UInt = 20U,
    eventExtractor: NumassEventExtractor = NumassEventExtractor.EVENTS_ONLY,
    splitChannels: Boolean = true
): PlotlyFragment = Plotly.fragment {
    plot {
        runBlocking {
            if (splitChannels && block is NumassPoint) {
                block.getChannels().forEach { (channel, channelBlock) ->
                    val spectrum = channelBlock.amplitudeSpectrum(eventExtractor)
                    histogram(spectrum, amplitudeBinSize) {
                        name = block.title + "[$channel]"
                    }
                }
            } else {
                scatter {
                    val spectrum = block.amplitudeSpectrum(eventExtractor)
                    histogram(spectrum, amplitudeBinSize)
                }
            }
        }
    }
}

fun Plotly.plotNumassSet(
    set: NumassSet,
    amplitudeBinSize: UInt = 20U,
    eventExtractor: NumassEventExtractor = NumassEventExtractor.EVENTS_ONLY,
): PlotlyFragment = Plotly.fragment {

    h1 { +"Numass point set ${(set as? NumassDirectorySet)?.path ?: ""}" }

    //TODO do in parallel
    val spectra = runBlocking {
        set.points.sortedBy { it.index }.map { it to it.amplitudeSpectrum(eventExtractor) }
    }

    h2 { +"Amplitude spectrum" }

    plot {
        spectra.forEach { (point, spectrum) ->
            histogram(spectrum, amplitudeBinSize) {
                name = point.title
            }
        }
    }

    h2 { +"Time spectra" }

    plot {
        spectra.forEach { (point,spectrum) ->
            val countRate = runBlocking {
                spectrum.sum().toDouble() / point.getLength().toDouble(DurationUnit.SECONDS)
            }
            val binSize = 1.0 / countRate  / 10.0
            histogram(point.timeHistogram(binSize)) {
                name = point.title
            }
        }
        layout.yaxis.type = AxisType.log
    }

    h2 { +"Integral spectrum" }

    plot {
        scatter {
            mode = ScatterMode.markers
            x.numbers = spectra.map { it.first.voltage }
            y.numbers = spectra.map { it.second.sum().toLong() }
        }
    }

    if (set is NumassDirectorySet) {
        set.getHvData()?.let { entries ->
            h2 { +"HV" }
            plot {
                hvData(entries)
            }
        }
    }
}

/**
 * Add a number buffer accessor for Plotly trace values
 */
public var TraceValues.buffer: Buffer<Number>
    get() = value?.list?.let { list -> DoubleBuffer(list.size) { list[it].double } } ?: DoubleBuffer()
    set(value) {
        this.value = value.asIterable().map { it.asValue() }.asValue()
    }