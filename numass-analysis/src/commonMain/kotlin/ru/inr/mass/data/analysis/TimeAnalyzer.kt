///*
// * Copyright  2017 Alexander Nozik.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// *  Unless required by applicable law or agreed to in writing, software
// *  distributed under the License is distributed on an "AS IS" BASIS,
// *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *  See the License for the specific language governing permissions and
// *  limitations under the License.
// */
//
package ru.inr.mass.data.analysis

import kotlinx.coroutines.flow.*
import ru.inr.mass.data.analysis.TimeAnalyzerParameters.AveragingMethod
import ru.inr.mass.data.api.NumassBlock
import ru.inr.mass.data.api.NumassEvent
import ru.inr.mass.data.api.ParentBlock
import space.kscience.kmath.streaming.asFlow
import space.kscience.kmath.streaming.chunked
import space.kscience.kmath.structures.Buffer
import kotlin.math.*


/**
 * An analyzer which uses time information from events
 * Created by darksnake on 11.07.2017.
 */
public open class TimeAnalyzer(override val extractor: NumassEventExtractor) : NumassAnalyzer() {

    override suspend fun analyzeInternal(
        block: NumassBlock,
        parameters: NumassAnalyzerParameters,
    ): NumassAnalyzerResult {
        //Parallel processing and merging of parent blocks
        if (block is ParentBlock) {
            val res = block.flowBlocks().map { analyzeInternal(it, parameters) }.toList()
            return res.combineResults(parameters.t0.averagingMethod)
        }

        val t0 = getT0(block, parameters.t0).toLong()

        return when (val chunkSize = parameters.t0.chunkSize) {
            null -> block.flowFilteredEvents(parameters)
                .byPairs(parameters.t0.inverted)
                .analyze(t0)
            //            // chunk is larger than a number of event
            //            chunkSize > count -> NumassAnalyzerResult {
            //                this.length = length
            //                this.count = count
            //                this.countRate = count.toDouble() / length
            //                this.countRateError = sqrt(count.toDouble()) / length
            //            }
            else -> block.flowFilteredEvents(parameters)
                .byPairs(parameters.t0.inverted)
                .chunked(chunkSize, Buffer.Companion::auto)
                .map { it.asFlow().analyze(t0) }
                .toList()
                .combineResults(parameters.t0.averagingMethod)
        }

    }


    /**
     * Analyze given flow of events + delays
     */
    private suspend fun Flow<Pair<NumassEvent, Long>>.analyze(t0: Long): NumassAnalyzerResult {
        var totalN = 0L
        var totalT = 0L
        filter { pair -> pair.second >= t0 }.collect { pair ->
            totalN++
            //TODO add progress listener here
            totalT+= pair.second
        }

        if (totalN == 0L) {
            error("Zero number of intervals")
        }

        val countRate = 1e6 * totalN / (totalT / 1000 - t0 * totalN / 1000)
        val countRateError = countRate / sqrt(totalN.toDouble())
        val length = totalT / 1e9
        val count = (length * countRate).toLong()

        return NumassAnalyzerResult {
            this.length = totalT / 1e9
            this.count = count
            this.countRate = countRate
            this.countRateError = countRateError
        }
    }

    /**
     * Combine multiple blocks from the same point into one
     *
     * @return
     */
    private fun List<NumassAnalyzerResult>.combineResults(method: AveragingMethod): NumassAnalyzerResult {

        if (this.isEmpty()) {
            return NumassAnalyzerResult.empty()
        }

        val totalTime = sumOf { it.length }

        val (countRate, countRateDispersion) = when (method) {
            AveragingMethod.ARITHMETIC -> Pair(
                sumOf { it.countRate } / size,
                sumOf { it.countRateError.pow(2.0) } / size / size
            )
            AveragingMethod.WEIGHTED -> Pair(
                sumOf { it.countRate * it.length } / totalTime,
                sumOf { (it.countRateError * it.length / totalTime).pow(2.0) }
            )
            AveragingMethod.GEOMETRIC -> {
                val mean = exp(sumOf { ln(it.countRate) } / size)
                val variance = (mean / size).pow(2.0) * sumOf {
                    (it.countRateError / it.countRate).pow(2.0)
                }
                Pair(mean, variance)
            }
        }

        return NumassAnalyzerResult {
            length = totalTime
            count = sumOf { it.count }
            this.countRate = countRate
            this.countRateError = sqrt(countRateDispersion)
        }
    }

    /**
     * Compute actual t0
     */
    private suspend fun getT0(block: NumassBlock, parameters: TimeAnalyzerParameters): Int {
        parameters.value?.let { return it }
        parameters.crFraction?.let { fraction ->
            val cr = block.events.count().toDouble() / block.getLength().inWholeMilliseconds * 1000
            if (cr < parameters.crMin) {
                0
            } else {
                max(-1e9 / cr * ln(1.0 - fraction), parameters.min).toInt()
            }
        }
        return 0
    }

    /**
     * Add a delay after (inverted = false) or before (inverted = true) event to each event
     */
    private suspend fun Flow<NumassEvent>.byPairs(inverted: Boolean = true): Flow<Pair<NumassEvent, Long>> = flow {
        var prev: NumassEvent?
        var next: NumassEvent?
        collect { value ->
            next = value
            prev = next
            if (prev != null && next != null) {
                val delay = next!!.timeOffset - prev!!.timeOffset
                if (delay < 0) error("Events are not ordered!")
                if (inverted) {
                    emit(Pair(next!!, delay))
                } else {
                    emit(Pair(prev!!, delay))
                }
            }
        }
    }

}
