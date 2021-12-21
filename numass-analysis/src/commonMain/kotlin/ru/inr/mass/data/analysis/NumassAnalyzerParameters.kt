package ru.inr.mass.data.analysis

import space.kscience.dataforge.meta.*

public class TimeAnalyzerParameters : Scheme() {

    public enum class AveragingMethod {
        ARITHMETIC,
        WEIGHTED,
        GEOMETRIC
    }

    public var value: Int? by int()

    /**
     * The relative fraction of events that should be removed by time cut
     */
    public var crFraction: Double? by double()
    public var min: Double by double(0.0)
    public var crMin: Double by double(0.0)

    /**
     * The number of events in chunk to split the chain into. If null, no chunks are used
     */
    public var chunkSize: Int? by int()

    public var inverted: Boolean by boolean(true)
    public var sortEvents: Boolean by boolean(false)

    /**
     * Chunk averaging method
     */
    public var averagingMethod: AveragingMethod by enum(AveragingMethod.WEIGHTED)

    public companion object : SchemeSpec<TimeAnalyzerParameters>(::TimeAnalyzerParameters)
}

public class NumassAnalyzerParameters : Scheme() {
    public var deadTime: Double by double(0.0)
    public var window: UIntRange by uIntRange()

    public var t0: TimeAnalyzerParameters by spec(TimeAnalyzerParameters)


    public companion object : SchemeSpec<NumassAnalyzerParameters>(::NumassAnalyzerParameters)
}