package ru.inr.mass.data.api

import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.nanoseconds

public interface ParentBlock : NumassBlock {

    public fun flowBlocks(): Flow<NumassBlock>

    /**
     * If true, the sub-blocks a considered to be sequential, if not, the sub-blocks are parallel
     */
    public val sequential: Boolean get() = true
}

/**
 * A block constructed from a set of other blocks. Internal blocks are not necessary subsequent. Blocks are automatically sorted.
 * Created by darksnake on 16.07.2017.
 */
public class MetaBlock(private val blocks: List<NumassBlock>) : ParentBlock {

    override fun flowBlocks(): Flow<NumassBlock> = blocks.asFlow()

    override val startTime: Instant
        get() = blocks.first().startTime

    override suspend fun getLength(): Duration = blocks.sumOf { it.getLength().inNanoseconds }.nanoseconds

    override val events: Flow<NumassEvent>
        get() = flow {
            blocks.sortedBy { it.startTime }.forEach { emitAll(it.events) }
        }

    override val frames: Flow<NumassFrame>
        get() = blocks.sortedBy { it.startTime }.asFlow().flatMapConcat { it.frames }


}
