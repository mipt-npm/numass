package ru.inr.mass.data.api

import hep.dataforge.meta.Meta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.datetime.Instant

/**
 * A simple static implementation of NumassPoint
 * Created by darksnake on 08.07.2017.
 */
public class SimpleNumassPoint(
    private val blocks: List<NumassBlock>,
    override val meta: Meta,
    override val startTime: Instant = Instant.DISTANT_PAST,
    override val sequential: Boolean = true,
) : NumassPoint {

    init {
        check(blocks.isNotEmpty()) { "No blocks in a point" }
    }

    override fun flowBlocks(): Flow<NumassBlock> = blocks.asFlow()

    override fun toString(): String = "SimpleNumassPoint(index = ${index}, hv = $voltage)"
}
