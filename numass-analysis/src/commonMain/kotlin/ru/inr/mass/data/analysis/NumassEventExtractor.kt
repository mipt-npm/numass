package ru.inr.mass.data.analysis

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.inr.mass.data.api.NumassBlock
import ru.inr.mass.data.api.NumassEvent

public fun interface NumassEventExtractor {
    public suspend fun extract(block: NumassBlock): Flow<NumassEvent>

    public companion object {
        /**
         * A default event extractor that ignores frames
         */
        public val EVENTS_ONLY: NumassEventExtractor = NumassEventExtractor { it.events }
        public val TQDC: NumassEventExtractor = NumassEventExtractor { block ->
            block.frames.map { frame ->
                var max = 0
                var min = 0
                var indexOfMax = 0

                frame.signal.forEachIndexed { index, sh ->
                    val corrected = sh.toUShort().toInt() - Short.MAX_VALUE
                    if (corrected >= max) {
                        max = corrected
                        indexOfMax = index
                    }
                    if (corrected <= min) {
                        min = corrected
                    }
                }

                NumassEvent(
                    (max - min).toShort().toUShort(),
                    frame.timeOffset + frame.tickSize.inWholeNanoseconds * indexOfMax,
                    block
                )
            }
        }
    }
}

//public fun NumassEventExtractor.filter(
//    condition: NumassBlock.(NumassEvent) -> Boolean,
//): NumassEventExtractor = NumassEventExtractor { block ->
//    extract(block).filter { block.condition(it) }
//}