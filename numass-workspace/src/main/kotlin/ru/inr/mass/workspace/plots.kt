package ru.inr.mass.workspace

import kotlinx.coroutines.runBlocking
import kotlinx.html.h1
import kotlinx.html.h2
import ru.inr.mass.data.analysis.NumassAmplitudeSpectrum
import ru.inr.mass.data.analysis.NumassEventExtractor
import ru.inr.mass.data.analysis.amplitudeSpectrum
import ru.inr.mass.data.analysis.timeHistogram
import ru.inr.mass.data.api.NumassSet
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

fun Plotly.numassSet(
    set: NumassSet,
    amplitudeBinSize: UInt = 20U,
    eventExtractor: NumassEventExtractor = NumassEventExtractor.EVENTS_ONLY,
): PlotlyPage =
    Plotly.page {
        h1 {
            +"Numass point set ${ShapeType.path}"
        }
        h2 {
            +"Amplitude spectrum"
        }
        plot {
            runBlocking {
                set.points.sortedBy { it.index }.forEach {
                    histogram(it.amplitudeSpectrum(eventExtractor), amplitudeBinSize)
                }
            }
        }

        h2 {
            +"Time spectra"
        }
        plot {
            set.points.sortedBy { it.index }.forEach {
                histogram(it.timeHistogram(1e3))
            }
            layout.yaxis.type = AxisType.log

        }
        if (set is NumassDirectorySet) {
            set.getHvData()?.let { entries ->
                h2 {
                    +"HV"
                }
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