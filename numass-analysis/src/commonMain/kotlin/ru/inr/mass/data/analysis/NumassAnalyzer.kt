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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import ru.inr.mass.data.api.*
import ru.inr.mass.data.api.NumassPoint.Companion.HV_KEY
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.values.ListValue
import space.kscience.dataforge.values.Value
import space.kscience.dataforge.values.ValueType
import space.kscience.dataforge.values.int
import space.kscience.tables.ColumnHeader
import space.kscience.tables.MetaRow
import space.kscience.tables.RowTable
import space.kscience.tables.Table
import kotlin.properties.ReadWriteProperty


public fun MutableMetaProvider.uIntRange(
    default: UIntRange = 0U..Int.MAX_VALUE.toUInt(),
    key: Name? = null,
): ReadWriteProperty<Any?, UIntRange> = value(
    key,
    reader = { value ->
        val (l, r) = value?.list ?: return@value default
        l.int.toUInt()..r.int.toUInt()
    },
    writer = { range ->
        ListValue(range.first.toInt(), range.last.toInt())
    }
)

public class NumassAnalyzerResult : Scheme() {
    public var count: Long by long(0L, NumassAnalyzer.count.name.asName())
    public var countRate: Double by double(0.0, NumassAnalyzer.cr.name.asName())
    public var countRateError: Double by double(0.0, NumassAnalyzer.crError.name.asName())
    public var length: Double by double(0.0, NumassAnalyzer.length.name.asName())

    public var voltage: Double? by double(HV_KEY.asName())

    public var parameters: NumassAnalyzerParameters by spec(NumassAnalyzerParameters)

    public companion object : SchemeSpec<NumassAnalyzerResult>(::NumassAnalyzerResult)
}


/**
 * A general raw data analysis utility. Could have different implementations
 * Created by darksnake on 06-Jul-17.
 */
public abstract class NumassAnalyzer {

    public abstract val extractor: NumassEventExtractor

    /**
     * Perform analysis on block. The values for count rate, its error and point length in nanos must
     * exist, but occasionally additional values could also be presented.
     *
     * @param block
     * @return
     */
    protected abstract suspend fun analyzeInternal(
        block: NumassBlock,
        parameters: NumassAnalyzerParameters,
    ): NumassAnalyzerResult

    /**
     * Analysis result for point including hv information
     * @param point
     * @param parameters
     * @return
     */
    public suspend fun analyze(
        point: ParentBlock,
        parameters: NumassAnalyzerParameters = NumassAnalyzerParameters.empty(),
    ): NumassAnalyzerResult {
        val res = analyzeInternal(point, parameters)
        if (point is NumassPoint) {
            res.voltage = point.voltage
        }
        res.parameters = parameters

        return res
    }

    protected suspend fun NumassBlock.flowFilteredEvents(
        parameters: NumassAnalyzerParameters,
    ): Flow<NumassEvent> {
        val window = parameters.window
        return extractor.extract(this).filter { it.amplitude in window }
    }

    public companion object {

        public val channel: ColumnHeader<Value> by ColumnHeader.value(ValueType.NUMBER)
        public val count: ColumnHeader<Value> by ColumnHeader.value(ValueType.NUMBER)
        public val length: ColumnHeader<Value> by ColumnHeader.value(ValueType.NUMBER)
        public val cr: ColumnHeader<Value> by ColumnHeader.value(ValueType.NUMBER)
        public val crError: ColumnHeader<Value> by ColumnHeader.value(ValueType.NUMBER)
        public val window: ColumnHeader<Value> by ColumnHeader.value(ValueType.LIST)
        public val timestamp: ColumnHeader<Value> by ColumnHeader.value(ValueType.NUMBER)
//
//        val AMPLITUDE_ADAPTER: ValuesAdapter = Adapters.buildXYAdapter(CHANNEL_KEY, COUNT_RATE_KEY)

        public val MAX_CHANNEL: UInt = 10000U
    }
}

/**
 * Analyze the whole set. And return results as a table
 *
 * @param set
 * @param config
 * @return
 */
public suspend fun NumassAnalyzer.analyzeSet(
    set: NumassSet,
    config: NumassAnalyzerParameters = NumassAnalyzerParameters.empty(),
): Table<Value> = RowTable(
    NumassAnalyzer.length,
    NumassAnalyzer.count,
    NumassAnalyzer.cr,
    NumassAnalyzer.crError,
//        NumassAnalyzer.window,
//        NumassAnalyzer.timestamp
) {
    set.points.forEach { point ->
        addRow(MetaRow(analyze(point, config).meta))
    }
}
//
//public suspend fun NumassAnalyzer.getAmplitudeSpectrum(
//    block: NumassBlock,
//    range: UIntRange = 0U..MAX_CHANNEL,
//    config: Meta = Meta.EMPTY,
//): Table<Value> {
//    val seconds = block.getLength().inWholeMilliseconds.toDouble() / 1000.0
//    return getEvents(block, config).getAmplitudeSpectrum(seconds, range)
//}
//
///**
// * Calculate number of counts in the given channel
// *
// * @param spectrum
// * @param loChannel
// * @param upChannel
// * @return
// */
//internal fun Table<Value>.countInWindow(loChannel: Short, upChannel: Short): Long = rows.filter { row ->
//    row[channel]?.int in loChannel until upChannel
//}.sumOf { it[count]?.long ?: 0L }
//
///**
// * Calculate the amplitude spectrum for a given block. The s
// *
// * @param this@getAmplitudeSpectrum
// * @param length length in seconds, used for count rate calculation
// * @param config
// * @return
// */
//private suspend fun Flow<NumassEvent>.getAmplitudeSpectrum(
//    length: Double,
//    range: UIntRange = 0U..MAX_CHANNEL,
//): Table<Value> {
//
//    //optimized for fastest computation
//    val spectrum: MutableMap<UInt, LongCounter> = HashMap()
//    collect { event ->
//        val channel = event.amplitude
//        spectrum.getOrPut(channel.toUInt()) {
//            LongCounter()
//        }.add(1L)
//    }
//
//    return RowTable<Value>(channel, count, cr, crError) {
//        range.forEach { ch ->
//            val countValue: Long = spectrum[ch]?.value ?: 0L
//            valueRow(
//                channel to ch,
//                count to countValue,
//                cr to (countValue.toDouble() / length),
//                crError to sqrt(countValue.toDouble()) / length
//            )
//        }
//    }
//}
//
///**
// * Apply window and binning to a spectrum. Empty bins are filled with zeroes
// */
//private fun Table<Value>.withBinning(
//    binSize: UInt, range: UIntRange = 0U..MAX_CHANNEL,
//): Table<Value> = RowTable<Value>(channel, count, cr, crError) {
////    var chan = loChannel
////        ?: this.getColumn(NumassAnalyzer.CHANNEL_KEY).stream().mapToInt { it.int }.min().orElse(0)
////
////    val top = upChannel
////        ?: this.getColumn(NumassAnalyzer.CHANNEL_KEY).stream().mapToInt { it.int }.max().orElse(1)
//
//    val binSizeColumn = newColumn<Value>("binSize")
//
//    var chan = range.first
//
//    while (chan < range.last - binSize) {
//        val counter = LongCounter()
//        val countRateCounter = Counter.real()
//        val countRateDispersionCounter = Counter.real()
//
//        val binLo = chan
//        val binUp = chan + binSize
//
//        rows.filter { row ->
//            (row[channel]?.int ?: 0U) in binLo until binUp
//        }.forEach { row ->
//            counter.add(row[count]?.long ?: 0L)
//            countRateCounter.add(row[cr]?.double ?: 0.0)
//            countRateDispersionCounter.add(row[crError]?.double?.pow(2.0) ?: 0.0)
//        }
//        val bin = min(binSize, range.last - chan)
//
//        valueRow(
//            channel to (chan.toDouble() + bin.toDouble() / 2.0),
//            count to counter.value,
//            cr to countRateCounter.value,
//            crError to sqrt(countRateDispersionCounter.value),
//            binSizeColumn to bin
//        )
//        chan += binSize
//    }
//}
//
///**
// * Subtract reference spectrum.
// */
//private fun subtractAmplitudeSpectrum(
//    sp1: Table<Value>, sp2: Table<Value>,
//): Table<Value> = RowTable<Value>(channel, cr, crError) {
//    sp1.rows.forEach { row1 ->
//        val channelValue = row1[channel]?.double
//        val row2 = sp2.rows.find { it[channel]?.double == channelValue } ?: MapRow(emptyMap())
//
//        val value = max((row1[cr]?.double ?: 0.0) - (row2[cr]?.double ?: 0.0), 0.0)
//        val error1 = row1[crError]?.double ?: 0.0
//        val error2 = row2[crError]?.double ?: 0.0
//        val error = sqrt(error1 * error1 + error2 * error2)
//        valueRow(channel to channelValue, cr to value, crError to error)
//    }
//}