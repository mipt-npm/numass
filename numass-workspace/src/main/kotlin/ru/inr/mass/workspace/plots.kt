package ru.inr.mass.workspace

import kotlinx.html.h1
import kotlinx.html.h2
import ru.inr.mass.data.api.NumassPoint
import ru.inr.mass.data.proto.HVEntry
import ru.inr.mass.data.proto.NumassDirectorySet
import space.kscience.dataforge.values.asValue
import space.kscience.dataforge.values.double
import space.kscience.kmath.histogram.UnivariateHistogram
import space.kscience.kmath.histogram.center
import space.kscience.kmath.misc.UnstableKMathAPI
import space.kscience.kmath.structures.Buffer
import space.kscience.kmath.structures.DoubleBuffer
import space.kscience.kmath.structures.asIterable
import space.kscience.plotly.*
import space.kscience.plotly.models.Trace
import space.kscience.plotly.models.TraceValues

@OptIn(UnstableKMathAPI::class)
fun Trace.fromSpectrum(histogram: UnivariateHistogram) {
    x.numbers = histogram.bins.map { it.domain.center }
    y.numbers = histogram.bins.map { it.value }
}

@OptIn(UnstableKMathAPI::class)
fun Plot.spectrum(name: String, histogram: UnivariateHistogram): Trace = scatter {
    this.name = name
    fromSpectrum(histogram)
}

fun Plot.amplitudeSpectrum(
    point: NumassPoint,
    binSize: Int = 20,
    range: IntRange = 0..2000,
    name: String = point.toString(),
): Trace = scatter {
    spectrum(name, point.spectrum().reShape(binSize, range))
}

/**
 * Generate a plot from hv data
 */
fun Plot.hvData(data: List<HVEntry>): Trace = scatter {
    x.strings = data.map { it.timestamp.toString() }
    y.numbers = data.map { it.value }
}

fun Plotly.numassDirectory(set: NumassDirectorySet, binSize: Int = 20, range: IntRange = 0..2000): PlotlyPage =
    Plotly.page {
        h1 {
            +"Numass point set ${set.path}"
        }
        h2 {
            +"Amplitude spectrum"
        }
        plot {
            set.points.sortedBy { it.index }.forEach {
                amplitudeSpectrum(it, binSize, range)
            }
        }
        set.getHvData()?.let { entries ->
            h2 {
                +"HV"
            }
            plot {
                hvData(entries)
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