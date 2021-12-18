package ru.inr.mass.data.analysis

import kotlinx.coroutines.flow.Flow
import ru.inr.mass.data.api.NumassBlock
import ru.inr.mass.data.api.NumassEvent

public fun interface NumassEventExtractor {
    public suspend fun extract(block: NumassBlock): Flow<NumassEvent>

    public companion object {
        /**
         * A default event extractor that ignores frames
         */
        public val EVENTS_ONLY: NumassEventExtractor = NumassEventExtractor { it.events }
    }
}

//public fun NumassEventExtractor.filter(
//    condition: NumassBlock.(NumassEvent) -> Boolean,
//): NumassEventExtractor = NumassEventExtractor { block ->
//    extract(block).filter { block.condition(it) }
//}