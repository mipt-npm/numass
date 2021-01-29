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
        numass.logger.warn { "Spectrum is generated from points with different hv value: $this" }
    }

    return UnivariateHistogram.uniform(1.0) {
        runBlocking {
            this@spectrum.forEach { point ->
                point.events.collect { put(it.channel.toDouble()) }
            }
        }
    }
}