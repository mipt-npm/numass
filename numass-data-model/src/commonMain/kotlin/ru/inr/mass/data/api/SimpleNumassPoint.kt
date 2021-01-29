package ru.inr.mass.data.api

import hep.dataforge.meta.Meta

/**
 * A simple static implementation of NumassPoint
 * Created by darksnake on 08.07.2017.
 */
public class SimpleNumassPoint(
    override val blocks: List<NumassBlock>,
    override val meta: Meta,
    override val isSequential: Boolean = true,
) : NumassPoint {
    init {
        check(blocks.isNotEmpty()){"No blocks in a point"}
    }

    override fun toString(): String = "SimpleNumassPoint(index = ${index}, hv = $voltage)"

}
