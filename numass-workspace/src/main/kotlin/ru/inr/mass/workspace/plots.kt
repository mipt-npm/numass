package ru.inr.mass.workspace

import kotlinx.html.h1
import kotlinx.html.h2
import kscience.kmath.histogram.UnivariateHistogram
import kscience.kmath.misc.UnstableKMathAPI
import kscience.plotly.*
import kscience.plotly.models.Bar
import kscience.plotly.models.Scatter
import kscience.plotly.models.Trace
import ru.inr.mass.data.api.NumassPoint
import ru.inr.mass.data.proto.HVEntry
import ru.inr.mass.data.proto.NumassDirectorySet

@OptIn(UnstableKMathAPI::class)
fun Trace.fromSpectrum(histogram: UnivariateHistogram) {
    x.numbers = histogram.map { it.position }
    y.numbers = histogram.map { it.value }
}

/**
 *
 */
@OptIn(UnstableKMathAPI::class)
fun Plot.spectrum(name: String, histogram: UnivariateHistogram): Bar = bar {
    this.name = name
    fromSpectrum(histogram)
}

fun Plot.amplitudeSpectrum(point: NumassPoint, binSize: Int = 20, range: IntRange = 0..4096, name: String = point.toString()): Bar = bar {
    spectrum(name, point.spectrum().reShape(binSize, range))
}

/**
 * Generate a plot from hv data
 */
fun Plot.hvData(data: List<HVEntry>): Scatter = scatter {
    x.strings = data.map { it.timestamp.toString() }
    y.numbers = data.map { it.value }
}

fun NumassDirectorySet.plotlyPage(binSize: Int = 20, range: IntRange = 0..4096): PlotlyPage = Plotly.page {
    h1 {
        +"Numass point set $path"
    }
    h2{
        +"Amplitude spectrum"
    }
    plot {
        points.forEach {
            amplitudeSpectrum(it,binSize, range)
        }
    }
    hvData?.let {entries->
        h2{
            +"HV"
        }
        plot {
            hvData(entries)
        }
    }
}