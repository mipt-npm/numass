package ru.inr.mass.workspace

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import ru.inr.mass.data.api.NumassPoint
import space.kscience.dataforge.context.logger
import space.kscience.dataforge.context.warn
import space.kscience.kmath.histogram.UnivariateHistogram
import space.kscience.kmath.histogram.center
import space.kscience.kmath.histogram.put
import space.kscience.kmath.structures.DoubleBuffer
import space.kscience.kmath.structures.asBuffer


/**
 * Build an amplitude spectrum
 */
fun NumassPoint.spectrum(): UnivariateHistogram = UnivariateHistogram.uniform(1.0) {
    runBlocking {
        events.collect { put(it.amplitude.toDouble()) }
    }
}

operator fun UnivariateHistogram.component1(): DoubleBuffer = bins.map { it.domain.center }.toDoubleArray().asBuffer()
operator fun UnivariateHistogram.component2(): DoubleBuffer = bins.map { it.value }.toDoubleArray().asBuffer()

fun Collection<NumassPoint>.spectrum(): UnivariateHistogram {
    if (distinctBy { it.voltage }.size != 1) {
        NUMASS.logger.warn { "Spectrum is generated from points with different hv value: $this" }
    }

    return UnivariateHistogram.uniform(1.0) {
        runBlocking {
            this@spectrum.forEach { point ->
                point.events.collect { put(it.amplitude.toDouble()) }
            }
        }
    }
}

/**
 * Re-shape the spectrum with the increased bin size and range. Throws a error if new bin is smaller than before.
 */
fun UnivariateHistogram.reShape(
    binSize: Int,
    channelRange: IntRange,
): UnivariateHistogram = UnivariateHistogram.uniform(binSize.toDouble()) {
    this@reShape.bins.filter { it.domain.center.toInt() in channelRange }.forEach { bin ->
        if (bin.domain.volume() > binSize.toDouble()) error("Can't reShape the spectrum with increased binning")
        putValue(bin.domain.center, bin.value)
    }
}