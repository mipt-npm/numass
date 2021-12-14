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

import inr.numass.data.analyzers.SimpleAnalyzer
import kotlinx.coroutines.flow.Flow
import ru.inr.mass.data.api.NumassBlock
import ru.inr.mass.data.api.NumassEvent
import ru.inr.mass.data.api.NumassSet
import ru.inr.mass.data.api.SignalProcessor
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.values.Value
import space.kscience.dataforge.values.asValue
import space.kscience.dataforge.values.setValue
import space.kscience.tables.Table

/**
 * An analyzer dispatcher which uses different analyzer for different meta
 * Created by darksnake on 11.07.2017.
 */
public open class SmartAnalyzer(processor: SignalProcessor? = null) : AbstractAnalyzer(processor) {
    private val simpleAnalyzer = SimpleAnalyzer(processor)
    private val debunchAnalyzer = DebunchAnalyzer(processor)
    private val timeAnalyzer: NumassAnalyzer = TODO()// TimeAnalyzer(processor)

    override val descriptor: MetaDescriptor? = null

    private fun getAnalyzer(config: Meta): NumassAnalyzer = when (val type = config["type"]?.string) {
        null -> if (config["t0"] != null) {
            timeAnalyzer
        } else {
            simpleAnalyzer
        }
        "simple" -> simpleAnalyzer
        "time" -> timeAnalyzer
        "debunch" -> debunchAnalyzer
        else -> throw IllegalArgumentException("Analyzer $type not found")
    }

    override suspend fun analyze(block: NumassBlock, config: Meta): NumassAnalyzerResult {
        val analyzer = getAnalyzer(config)
        val res = analyzer.analyze(block, config)
        return NumassAnalyzerResult.read(res.meta).apply {
            setValue(T0_KEY, 0.0.asValue())
        }
    }

    override fun getEvents(block: NumassBlock, meta: Meta): Flow<NumassEvent> =
        getAnalyzer(meta).getEvents(block, meta)


    override suspend fun analyzeSet(set: NumassSet, config: Meta): Table<Value> {
        return getAnalyzer(config).analyzeSet(set, config)
//        fun Value.computeExpression(point: NumassPoint): Int {
//            return when {
//                this.type == ValueType.NUMBER -> this.int
//                this.type == ValueType.STRING -> {
//                    val exprParams = HashMap<String, Any>()
//
//                    exprParams["U"] = point.voltage
//
//                    ExpressionUtils.function(this.string, exprParams).toInt()
//                }
//                else -> error("Can't interpret $type as expression or number")
//            }
//        }
//
//        val lo = config.getValue("window.lo", 0)
//        val up = config.getValue("window.up", Int.MAX_VALUE)
//
//        val format = getTableFormat(config)
//
//        return ListTable.Builder(format)
//            .rows(set.points.map { point ->
//                val newConfig = config.builder.apply {
//                    setValue("window.lo", lo.computeExpression(point))
//                    setValue("window.up", up.computeExpression(point))
//                }
//                analyzeParent(point, newConfig)
//            })
//            .build()
    }

    public companion object : SmartAnalyzer() {
        public const val T0_KEY: String = "t0"
    }
}