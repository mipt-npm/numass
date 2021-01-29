package ru.inr.mass.data.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.nanoseconds

public interface ParentBlock : NumassBlock {
    public val blocks: List<NumassBlock>

    /**
     * If true, the sub-blocks a considered to be isSequential, if not, the sub-blocks are parallel
     */
    public val isSequential: Boolean get() = true
}

/**
 * A block constructed from a set of other blocks. Internal blocks are not necessary subsequent. Blocks are automatically sorted.
 * Created by darksnake on 16.07.2017.
 */
public class MetaBlock(override val blocks: List<NumassBlock>) : ParentBlock {

    override val startTime: Instant
        get() = blocks.first().startTime

    override val length: Duration
        get() = blocks.sumOf { it.length.inNanoseconds }.nanoseconds

    override val events: Flow<NumassEvent>
        get() = blocks.sortedBy { it.startTime }.asFlow().flatMapConcat { it.events }

    override val frames: Flow<NumassFrame>
        get() = blocks.sortedBy { it.startTime }.asFlow().flatMapConcat { it.frames }


}
