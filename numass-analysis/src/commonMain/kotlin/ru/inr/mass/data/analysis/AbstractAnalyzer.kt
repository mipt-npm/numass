/*
 * Copyright  2017 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ru.inr.mass.data.analysis

import kotlinx.coroutines.flow.*
import ru.inr.mass.data.api.NumassBlock
import ru.inr.mass.data.api.NumassEvent
import ru.inr.mass.data.api.NumassSet
import ru.inr.mass.data.api.SignalProcessor
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.int
import space.kscience.dataforge.tables.RowTable
import space.kscience.dataforge.tables.Table
import space.kscience.dataforge.values.Value

/**
 * Created by darksnake on 11.07.2017.
 */
public abstract class AbstractAnalyzer(
    private val processor: SignalProcessor? = null,
) : NumassAnalyzer {

    /**
     * Return unsorted stream of events including events from frames.
     * In theory, events after processing could be unsorted due to mixture of frames and events.
     * In practice usually block have either frame or events, but not both.
     *
     * @param block
     * @return
     */
    override fun getEvents(block: NumassBlock, meta: Meta): Flow<NumassEvent> {
        val range = meta.getRange()
        return getAllEvents(block).filter { event ->
            event.amplitude.toInt() in range
        }
    }

    protected fun Meta.getRange(): IntRange {
        val loChannel = get("window.lo")?.int ?: 0
        val upChannel = get("window.up")?.int ?: Int.MAX_VALUE
        return loChannel until upChannel
    }

    protected fun getAllEvents(block: NumassBlock): Flow<NumassEvent> {
        return when {
            block.framesCount == 0L -> block.events
            processor == null -> throw IllegalArgumentException("Signal processor needed to analyze frames")
            else -> flow {
                emitAll(block.events)
                emitAll(block.frames.flatMapConcat { processor.analyze(it) })
            }
        }
    }

//    /**
//     * Get table format for summary table
//     *
//     * @param config
//     * @return
//     */
//    protected open fun getTableFormat(config: Meta): ValueTableHeader {
//        return TableFormatBuilder()
//            .addNumber(HV_KEY, X_VALUE_KEY)
//            .addNumber(NumassAnalyzer.LENGTH_KEY)
//            .addNumber(NumassAnalyzer.COUNT_KEY)
//            .addNumber(NumassAnalyzer.COUNT_RATE_KEY, Y_VALUE_KEY)
//            .addNumber(NumassAnalyzer.COUNT_RATE_ERROR_KEY, Y_ERROR_KEY)
//            .addColumn(NumassAnalyzer.WINDOW_KEY)
//            .addTime()
//            .build()
//    }

    override suspend fun analyzeSet(set: NumassSet, config: Meta): Table<Value> = RowTable(
        NumassAnalyzer.length,
        NumassAnalyzer.count,
        NumassAnalyzer.cr,
        NumassAnalyzer.crError,
//        NumassAnalyzer.window,
//        NumassAnalyzer.timestamp
    ) {

        set.points.forEach { point ->
            analyzeParent(point, config)
        }
    }

    public companion object {
//        public val NAME_LIST: List<String> = listOf(
//            NumassAnalyzer.LENGTH_KEY,
//            NumassAnalyzer.COUNT_KEY,
//            NumassAnalyzer.COUNT_RATE_KEY,
//            NumassAnalyzer.COUNT_RATE_ERROR_KEY,
//            NumassAnalyzer.WINDOW_KEY,
//            NumassAnalyzer.TIME_KEY
//        )
    }
}
