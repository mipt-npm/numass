/*
 * Copyright  2018 Alexander Nozik.
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

package ru.inr.mass.data.api

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.double
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.int
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit

/**
 * Created by darksnake on 06-Jul-17.
 */
@OptIn(FlowPreview::class)
public interface NumassPoint : ParentBlock {

    public val meta: Meta

    /**
     * Get the voltage setting for the point
     */
    public val voltage: Double get() = meta[HV_KEY].double ?: 0.0

    /**
     * Get the index for this point in the set
     */
    public val index: Int get() = meta[INDEX_KEY].int ?: -1

    /**
     * Get the length key of meta or calculate length as a sum of block lengths. The latter could be a bit slow
     */
    override suspend fun getLength(): Duration = blocks.filter { it.channel == 0 }.toList()
        .sumOf { it.getLength().toLong(DurationUnit.NANOSECONDS) }.nanoseconds

    /**
     * Get all events it all blocks as a single sequence
     * Some performance analysis of different stream concatenation approaches is given here: https://www.techempower.com/blog/2016/10/19/efficient-multiple-stream-concatenation-in-java/
     */
    override val events: Flow<NumassEvent> get() = blocks.asFlow().flatMapConcat { it.events }

    /**
     * Get all frames in all blocks as a single sequence
     */
    override val frames: Flow<NumassFrame> get() = blocks.asFlow().flatMapConcat { it.frames }


    public suspend fun isSequential(): Boolean = channels.size == 1

    override fun toString(): String

    public companion object {
        public const val NUMASS_BLOCK_TARGET: String = "block"
        public const val NUMASS_CHANNEL_TARGET: String = "channel"

        public const val START_TIME_KEY: String = "start"
        public const val LENGTH_KEY: String = "length"
        public const val HV_KEY: String = "voltage"
        public const val INDEX_KEY: String = "index"
    }
}

/**
 * Distinct map of channel number to corresponding grouping block
 */
public val NumassPoint.channels: Map<Int, NumassBlock>
    get() = blocks.groupBy { it.channel }.mapValues { entry ->
        if (entry.value.size == 1) {
            entry.value.first()
        } else {
            MetaBlock(entry.value)
        }
    }

public val NumassPoint.title: String get() = "p$index(HV=$voltage)"

/**
 * Get the first block if it exists. Throw runtime exception otherwise.
 *
 */
public suspend fun NumassPoint.getFirstBlock(): NumassBlock =
    blocks.firstOrNull() ?: throw RuntimeException("The point is empty")
