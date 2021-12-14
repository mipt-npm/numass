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
import kotlinx.coroutines.flow.collect
import ru.inr.mass.data.analysis.NumassAnalyzer.Companion.MAX_CHANNEL
import ru.inr.mass.data.analysis.NumassAnalyzer.Companion.channel
import ru.inr.mass.data.analysis.NumassAnalyzer.Companion.count
import ru.inr.mass.data.analysis.NumassAnalyzer.Companion.cr
import ru.inr.mass.data.analysis.NumassAnalyzer.Companion.crError
import ru.inr.mass.data.api.*
import ru.inr.mass.data.api.NumassPoint.Companion.HV_KEY
import ru.inr.mass.data.api.NumassPoint.Companion.LENGTH_KEY
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.meta.descriptors.Described
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.values.*
import space.kscience.kmath.histogram.Counter
import space.kscience.kmath.histogram.LongCounter
import space.kscience.tables.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

public class NumassAnalyzerResult : Scheme() {
    public var count: Long? by long(NumassAnalyzer.count.name.asName())
    public var countRate: Double? by double(NumassAnalyzer.cr.name.asName())
    public var countRateError: Double? by double(NumassAnalyzer.crError.name.asName())
    public var length: Long? by long(NumassAnalyzer.length.name.asName())

    public var voltage: Double? by double(HV_KEY.asName())

    public var window: UIntRange?
        get() = meta["window"]?.value?.list?.let {
            it[0].int.toUInt().rangeTo(it[1].int.toUInt())
        }
        set(value) {
            meta["window"] = value?.let { ListValue(it.first.toInt(), it.first.toInt()) }
        }

    public companion object : SchemeSpec<NumassAnalyzerResult>(::NumassAnalyzerResult)
}


/**
 * A general raw data analysis utility. Could have different implementations
 * Created by darksnake on 06-Jul-17.
 */
public interface NumassAnalyzer : Described {

    /**
     * Perform analysis on block. The values for count rate, its error and point length in nanos must
     * exist, but occasionally additional values could also be presented.
     *
     * @param block
     * @return
     */
    public suspend fun analyze(block: NumassBlock, config: Meta = Meta.EMPTY): NumassAnalyzerResult

    /**
     * Analysis result for point including hv information
     * @param point
     * @param config
     * @return
     */
    public suspend fun analyzeParent(point: ParentBlock, config: Meta = Meta.EMPTY): NumassAnalyzerResult {
//        //Add properties to config
//        val newConfig = config.builder.apply {
//            if (point is NumassPoint) {
//                setValue("voltage", point.voltage)
//                setValue("index", point.index)
//            }
//            setValue("channel", point.channel)
//        }
        val res = analyze(point, config)
        if (point is NumassPoint) {
            res.voltage = point.voltage
        }

        return res
    }

    /**
     * Return unsorted stream of events including events from frames
     *
     * @param block
     * @return
     */
    public fun getEvents(block: NumassBlock, meta: Meta = Meta.EMPTY): Flow<NumassEvent>

    /**
     * Analyze the whole set. And return results as a table
     *
     * @param set
     * @param config
     * @return
     */
    public suspend fun analyzeSet(set: NumassSet, config: Meta): Table<Value>

    /**
     * Get the approximate number of events in block. Not all analyzers support precise event counting
     *
     * @param block
     * @param config
     * @return
     */
    public suspend fun getCount(block: NumassBlock, config: Meta): Long =
        analyze(block, config).getValue(count.name)?.long ?: 0L

    /**
     * Get approximate effective point length in nanos. It is not necessary corresponds to real point length.
     *
     * @param block
     * @param config
     * @return
     */
    public suspend fun getLength(block: NumassBlock, config: Meta = Meta.EMPTY): Long =
        analyze(block, config).getValue(LENGTH_KEY)?.long ?: 0L

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

public suspend fun NumassAnalyzer.getAmplitudeSpectrum(
    block: NumassBlock,
    range: UIntRange = 0U..MAX_CHANNEL,
    config: Meta = Meta.EMPTY,
): Table<Value> {
    val seconds = block.getLength().inWholeMilliseconds.toDouble() / 1000.0
    return getEvents(block, config).getAmplitudeSpectrum(seconds, range)
}

/**
 * Calculate number of counts in the given channel
 *
 * @param spectrum
 * @param loChannel
 * @param upChannel
 * @return
 */
internal fun Table<Value>.countInWindow(loChannel: Short, upChannel: Short): Long = rows.filter { row ->
    row[channel]?.int in loChannel until upChannel
}.sumOf { it[count]?.long ?: 0L }

/**
 * Calculate the amplitude spectrum for a given block. The s
 *
 * @param this@getAmplitudeSpectrum
 * @param length length in seconds, used for count rate calculation
 * @param config
 * @return
 */
private suspend fun Flow<NumassEvent>.getAmplitudeSpectrum(
    length: Double,
    range: UIntRange = 0U..MAX_CHANNEL,
): Table<Value> {

    //optimized for fastest computation
    val spectrum: MutableMap<UInt, LongCounter> = HashMap()
    collect { event ->
        val channel = event.amplitude
        spectrum.getOrPut(channel.toUInt()) {
            LongCounter()
        }.add(1L)
    }

    return RowTable<Value>(channel, count, cr, crError) {
        range.forEach { ch ->
            val countValue: Long = spectrum[ch]?.value ?: 0L
            valueRow(
                channel to ch,
                count to countValue,
                cr to (countValue.toDouble() / length),
                crError to sqrt(countValue.toDouble()) / length
            )
        }
    }
}

/**
 * Apply window and binning to a spectrum. Empty bins are filled with zeroes
 */
private fun Table<Value>.withBinning(
    binSize: UInt, range: UIntRange = 0U..MAX_CHANNEL,
): Table<Value> = RowTable<Value>(channel, count, cr, crError) {
//    var chan = loChannel
//        ?: this.getColumn(NumassAnalyzer.CHANNEL_KEY).stream().mapToInt { it.int }.min().orElse(0)
//
//    val top = upChannel
//        ?: this.getColumn(NumassAnalyzer.CHANNEL_KEY).stream().mapToInt { it.int }.max().orElse(1)

    val binSizeColumn = newColumn<Value>("binSize")

    var chan = range.first

    while (chan < range.last - binSize) {
        val counter = LongCounter()
        val countRateCounter = Counter.real()
        val countRateDispersionCounter = Counter.real()

        val binLo = chan
        val binUp = chan + binSize

        rows.filter { row ->
            (row[channel]?.int ?: 0U) in binLo until binUp
        }.forEach { row ->
            counter.add(row[count]?.long ?: 0L)
            countRateCounter.add(row[cr]?.double ?: 0.0)
            countRateDispersionCounter.add(row[crError]?.double?.pow(2.0) ?: 0.0)
        }
        val bin = min(binSize, range.last - chan)

        valueRow(
            channel to (chan.toDouble() + bin.toDouble() / 2.0),
            count to counter.value,
            cr to countRateCounter.value,
            crError to sqrt(countRateDispersionCounter.value),
            binSizeColumn to bin
        )
        chan += binSize
    }
}

/**
 * Subtract reference spectrum.
 */
private fun subtractAmplitudeSpectrum(
    sp1: Table<Value>, sp2: Table<Value>,
): Table<Value> = RowTable<Value>(channel, cr, crError) {
    sp1.rows.forEach { row1 ->
        val channelValue = row1[channel]?.double
        val row2 = sp2.rows.find { it[channel]?.double == channelValue } ?: MapRow(emptyMap())

        val value = max((row1[cr]?.double ?: 0.0) - (row2[cr]?.double ?: 0.0), 0.0)
        val error1 = row1[crError]?.double ?: 0.0
        val error2 = row2[crError]?.double ?: 0.0
        val error = sqrt(error1 * error1 + error2 * error2)
        valueRow(channel to channelValue, cr to value, crError to error)
    }
}