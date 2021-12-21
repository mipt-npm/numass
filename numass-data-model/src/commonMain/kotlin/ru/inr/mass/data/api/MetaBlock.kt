package ru.inr.mass.data.api

import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit

public interface ParentBlock : NumassBlock {

    public val blocks: List<NumassBlock>

    /**
     * If true, the sub-blocks a considered to be sequential, if not, the sub-blocks are parallel
     */
    public val sequential: Boolean get() = true
}

/**
 * A block constructed from a set of other blocks. Internal blocks are not necessary subsequent. Blocks are automatically sorted.
 * Created by darksnake on 16.07.2017.
 */
public class MetaBlock(override val blocks: List<NumassBlock>) : ParentBlock {

    override val startTime: Instant
        get() = blocks.first().startTime

    override suspend fun getLength(): Duration =
        blocks.sumOf { it.getLength().toDouble(DurationUnit.NANOSECONDS) }.nanoseconds

    override val events: Flow<NumassEvent>
        get() = flow {
            blocks.sortedBy { it.startTime }.forEach { emitAll(it.events) }
        }

    override val frames: Flow<NumassFrame>
        get() = blocks.sortedBy { it.startTime }.asFlow().flatMapMerge { it.frames }

    override val eventsCount: Long
        get() = blocks.sumOf { it.eventsCount }
    override val framesCount: Long
        get() = blocks.sumOf { it.framesCount }
}
