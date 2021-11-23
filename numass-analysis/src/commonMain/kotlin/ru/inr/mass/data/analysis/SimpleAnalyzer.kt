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

package inr.numass.data.analyzers

import kotlinx.coroutines.flow.count
import ru.inr.mass.data.analysis.AbstractAnalyzer
import ru.inr.mass.data.analysis.NumassAnalyzerResult
import ru.inr.mass.data.api.NumassBlock
import ru.inr.mass.data.api.SignalProcessor
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.meta.descriptors.value
import space.kscience.dataforge.meta.double
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.int
import space.kscience.dataforge.values.ValueType
import kotlin.math.sqrt

/**
 * A simple event counter
 * Created by darksnake on 07.07.2017.
 */
public class SimpleAnalyzer(processor: SignalProcessor? = null) : AbstractAnalyzer(processor) {

    override val descriptor: MetaDescriptor = MetaDescriptor {
        value("deadTime", ValueType.NUMBER) {
            info = "Dead time in nanoseconds for correction"
            default(0.0)
        }
    }

    override suspend fun analyze(block: NumassBlock, config: Meta): NumassAnalyzerResult {
        val loChannel = config["window.lo"]?.int ?: 0
        val upChannel = config["window.up"]?.int ?: Int.MAX_VALUE

        val count: Int = getEvents(block, config).count()
        val length: Double = block.getLength().inWholeNanoseconds.toDouble() / 1e9

        val deadTime = config["deadTime"]?.double ?: 0.0

        val countRate = if (deadTime > 0) {
            val mu = count.toDouble() / length
            mu / (1.0 - deadTime * 1e-9 * mu)
        } else {
            count.toDouble() / length
        }
        val countRateError = sqrt(count.toDouble()) / length

        return NumassAnalyzerResult {
            this.length = length.toLong()
            this.count = count.toLong()
            this.countRate = countRate
            this.countRateError = countRateError
            this.window = loChannel.toUInt().rangeTo(upChannel.toUInt())
            //TODO NumassAnalyzer.timestamp to block.startTime
        }
    }
}
