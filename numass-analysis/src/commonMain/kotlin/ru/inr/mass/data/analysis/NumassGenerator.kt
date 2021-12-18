package ru.inr.mass.data.analysis
//
//import hep.dataforge.stat.defaultGenerator
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.takeWhile
//import kotlinx.coroutines.flow.toList
//import kotlinx.datetime.Instant
//import org.apache.commons.math3.distribution.EnumeratedRealDistribution
//import ru.inr.mass.data.analysis.NumassAnalyzer.Companion.CHANNEL_KEY
//import ru.inr.mass.data.analysis.NumassAnalyzer.Companion.COUNT_RATE_KEY
//import ru.inr.mass.data.api.NumassBlock
//import ru.inr.mass.data.api.OrphanNumassEvent
//import ru.inr.mass.data.api.SimpleBlock
//import space.kscience.kmath.chains.Chain
//import space.kscience.kmath.chains.MarkovChain
//import space.kscience.kmath.chains.StatefulChain
//import space.kscience.kmath.stat.RandomGenerator
//import space.kscience.tables.Table
//import kotlin.math.ln
//import kotlin.time.Duration.Companion.nanoseconds
//
//private fun RandomGenerator.nextExp(mean: Double): Double {
//    return -mean * ln(1.0 - nextDouble())
//}
//
//private fun RandomGenerator.nextDeltaTime(cr: Double): Long {
//    return (nextExp(1.0 / cr) * 1e9).toLong()
//}
//
//public suspend fun Flow<OrphanNumassEvent>.generateBlock(start: Instant, length: Long): NumassBlock {
//    return SimpleBlock.produce(start, length.nanoseconds) {
//        takeWhile { it.timeOffset < length }.toList()
//    }
//}
//
//private class MergingState(private val chains: List<Chain<OrphanNumassEvent>>) {
//    suspend fun poll(): OrphanNumassEvent {
//        val next = chains.minBy { it.value.timeOffset } ?: chains.first()
//        val res = next.value
//        next.next()
//        return res
//    }
//
//}
//
///**
// * Merge event chains in ascending time order
// */
//public fun List<Chain<OrphanNumassEvent>>.merge(): Chain<OrphanNumassEvent> {
//    return StatefulChain(MergingState(this), OrphanNumassEvent(0.toUShort(), 0L)) {
//        poll()
//    }
//}
//
///**
// * Apply dead time based on event that caused it
// */
//public fun Chain<OrphanNumassEvent>.withDeadTime(deadTime: (OrphanNumassEvent) -> Long): Chain<OrphanNumassEvent> {
//    return MarkovChain(this.value) {
//        val start = this.value
//        val dt = deadTime(start)
//        do {
//            val next = next()
//        } while (next.timeOffset - start.timeOffset < dt)
//        this.value
//    }
//}
//
//public object NumassGenerator {
//
//    public val defaultAmplitudeGenerator: RandomGenerator.(OrphanNumassEvent?, Long) -> Short =
//        { _, _ -> ((nextDouble() + 2.0) * 100).toShort() }
//
//    /**
//     * Generate an event chain with fixed count rate
//     * @param cr = count rate in Hz
//     * @param rnd = random number generator
//     * @param amp amplitude generator for the chain. The receiver is rng, first argument is the previous event and second argument
//     * is the delay between the next event. The result is the amplitude in channels
//     */
//    public fun generateEvents(
//        cr: Double,
//        rnd: RandomGenerator = defaultGenerator,
//        amp: RandomGenerator.(OrphanNumassEvent?, Long) -> Short = defaultAmplitudeGenerator,
//    ): Chain<OrphanNumassEvent> = MarkovChain(OrphanNumassEvent(rnd.amp(null, 0), 0)) { event ->
//        val deltaT = rnd.nextDeltaTime(cr)
//        OrphanNumassEvent(rnd.amp(event, deltaT), event.timeOffset + deltaT)
//    }
//
//    public fun mergeEventChains(vararg chains: Chain<OrphanNumassEvent>): Chain<OrphanNumassEvent> =
//        listOf(*chains).merge()
//
//
//    private data class BunchState(var bunchStart: Long = 0, var bunchEnd: Long = 0)
//
//    /**
//     * The chain of bunched events
//     * @param cr count rate of events inside bunch
//     * @param bunchRate number of bunches per second
//     * @param bunchLength the length of bunch
//     */
//    public fun generateBunches(
//        cr: Double,
//        bunchRate: Double,
//        bunchLength: Double,
//        rnd: RandomGenerator = defaultGenerator,
//        amp: RandomGenerator.(OrphanNumassEvent?, Long) -> Short = defaultAmplitudeGenerator,
//    ): Chain<OrphanNumassEvent> {
//        return StatefulChain(
//            BunchState(0, 0),
//            OrphanNumassEvent(rnd.amp(null, 0), 0)) { event ->
//            if (event.timeOffset >= bunchEnd) {
//                bunchStart = bunchEnd + rnd.nextDeltaTime(bunchRate)
//                bunchEnd = bunchStart + (bunchLength * 1e9).toLong()
//                OrphanNumassEvent(rnd.amp(null, 0), bunchStart)
//            } else {
//                val deltaT = rnd.nextDeltaTime(cr)
//                OrphanNumassEvent(rnd.amp(event, deltaT), event.timeOffset + deltaT)
//            }
//        }
//    }
//
//    /**
//     * Generate a chain using provided spectrum for amplitudes
//     */
//    public fun generateEvents(
//        cr: Double,
//        rnd: RandomGenerator = defaultGenerator,
//        spectrum: Table,
//    ): Chain<OrphanNumassEvent> {
//
//        val channels = DoubleArray(spectrum.size())
//        val values = DoubleArray(spectrum.size())
//        for (i in 0 until spectrum.size()) {
//            channels[i] = spectrum.get(CHANNEL_KEY, i).double
//            values[i] = spectrum.get(COUNT_RATE_KEY, i).double
//        }
//        val distribution = EnumeratedRealDistribution(channels, values)
//
//        return generateEvents(cr, rnd) { _, _ -> distribution.sample().toShort() }
//    }
//}