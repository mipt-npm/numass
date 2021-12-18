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

import kotlinx.coroutines.flow.count
import ru.inr.mass.data.api.NumassBlock
import kotlin.math.sqrt

/**
 * A simple event counter
 * Created by darksnake on 07.07.2017.
 */
public class SimpleAnalyzer(
    override val extractor: NumassEventExtractor = NumassEventExtractor.EVENTS_ONLY,
) : NumassAnalyzer() {

    override suspend fun analyzeInternal(
        block: NumassBlock,
        parameters: NumassAnalyzerParameters
    ): NumassAnalyzerResult {

        val count: Int = block.flowFilteredEvents(parameters).count()
        val length: Double = block.getLength().inWholeNanoseconds.toDouble() / 1e9

        val deadTime = parameters.deadTime

        val countRate = if (deadTime > 0) {
            val mu = count.toDouble() / length
            mu / (1.0 - deadTime * 1e-9 * mu)
        } else {
            count.toDouble() / length
        }
        val countRateError = sqrt(count.toDouble()) / length

        return NumassAnalyzerResult {
            this.length = length
            this.count = count.toLong()
            this.countRate = countRate
            this.countRateError = countRateError
            //TODO NumassAnalyzer.timestamp to block.startTime
        }
    }
}
