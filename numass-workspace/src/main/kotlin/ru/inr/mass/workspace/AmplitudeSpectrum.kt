@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.inr.mass.workspace

import hep.dataforge.context.logger
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kscience.kmath.histogram.UnivariateHistogram
import ru.inr.mass.data.api.NumassPoint

/**
 * Build an amplitude spectrum
 */
fun NumassPoint.spectrum(): UnivariateHistogram =
    UnivariateHistogram.uniform(1.0) {
        runBlocking {
            events.collect { put(it.channel.toDouble()) }
        }
    }

fun Collection<NumassPoint>.spectrum(): UnivariateHistogram {
    if (distinctBy { it.voltage }.size != 1) {
        NUMASS.logger.warn { "Spectrum is generated from points with different hv value: $this" }
    }

    return UnivariateHistogram.uniform(1.0) {
        runBlocking {
            this@spectrum.forEach { point ->
                point.events.collect { put(it.channel.toDouble()) }
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
    this@reShape.filter { it.position.toInt() in channelRange }.forEach { bin ->
        if(bin.size > binSize.toDouble()) error("Can't reShape the spectrum with increased binning")
        putMany(bin.position, bin.value.toLong())
    }
}