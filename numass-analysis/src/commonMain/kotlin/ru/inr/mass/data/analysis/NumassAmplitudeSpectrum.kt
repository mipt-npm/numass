package ru.inr.mass.data.analysis

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ru.inr.mass.data.api.NumassBlock
import space.kscience.kmath.histogram.LongCounter
import kotlin.math.min

public class NumassAmplitudeSpectrum(public val amplitudes: Map<Short, ULong>) {

    public val minChannel: Short by lazy { amplitudes.keys.minOf { it } }
    public val maxChannel: Short by lazy { amplitudes.keys.maxOf { it } }

    public val channels: IntRange by lazy { minChannel..maxChannel }

    public fun binned(binSize: UInt, range: IntRange = channels): Map<IntRange, Double> {
        val keys = sequence {
            var left = range.first
            do {
                val right = min(left + binSize.toInt(), range.last)
                yield(left..right)
                left = right
            } while (right < range.last)
        }

        return keys.associateWith { bin -> amplitudes.filter { it.key in bin }.values.sum().toDouble() }
    }

    public fun sum(range: IntRange = channels): ULong =
        amplitudes.filter { it.key in range }.values.sum()
}

/**
 * Build an amplitude spectrum with bin of 1.0 counted from 0.0. Some bins could be missing
 */
public suspend fun NumassBlock.amplitudeSpectrum(
    extractor: NumassEventExtractor = NumassEventExtractor.EVENTS_ONLY,
): NumassAmplitudeSpectrum {
    val map = HashMap<Short, LongCounter>()
    extractor.extract(this).collect { event ->
        map.getOrPut(event.amplitude) { LongCounter() }.add(1L)
    }
    return NumassAmplitudeSpectrum(map.mapValues { it.value.value.toULong() })
}

/**
 * Collect events from block in parallel
 */
public suspend fun Collection<NumassBlock>.amplitudeSpectrum(
    extractor: NumassEventExtractor = NumassEventExtractor.EVENTS_ONLY,
): NumassAmplitudeSpectrum {
    val hist = List(UShort.MAX_VALUE.toInt()) {
        LongCounter()
    }
    coroutineScope {
        forEach { block ->
            launch {
                extractor.extract(block).collect { event ->
                    hist[event.amplitude.toInt()].add(1L)
                }
            }
        }
    }

    val map = hist.mapIndexedNotNull { index, counter ->
        if (counter.value == 0L) {
            null
        } else {
            index.toShort() to counter.value.toULong()
        }
    }.toMap()

    return NumassAmplitudeSpectrum(map)
}