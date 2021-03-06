package ru.inr.mass.data.analysis

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.runBlocking
import ru.inr.mass.data.api.NumassBlock
import ru.inr.mass.data.api.getTime
import space.kscience.kmath.histogram.Histogram
import space.kscience.kmath.histogram.UniformHistogram1D
import space.kscience.kmath.histogram.uniform1D
import space.kscience.kmath.operations.DoubleField
import kotlin.math.max
import kotlin.time.DurationUnit

public fun <T, R> Flow<T>.zipWithNext(block: (l: T, r: T) -> R): Flow<R> {
    var current: T? = null
    return transform { r ->
        current?.let { l ->
            emit(block(l, r))
        }
        current = r
    }
}

public fun NumassBlock.timeHistogram(
    binSize: Double,
    extractor: NumassEventExtractor = NumassEventExtractor.EVENTS_ONLY,
): UniformHistogram1D<Double> = Histogram.uniform1D(DoubleField, binSize).produce {
    runBlocking {
        extractor.extract(this@timeHistogram).zipWithNext { l, r ->
            if(l.owner == r.owner) {
                max((r.getTime() - l.getTime()).toDouble(DurationUnit.SECONDS),0.0)
            } else {
                0
            }
        }.collect {
            putValue(it.toDouble())
        }
    }
}
