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

import hep.dataforge.meta.*
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import hep.dataforge.provider.Provider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMap
import kotlinx.coroutines.flow.flatMapConcat
import java.time.Duration
import java.time.Instant
import java.util.stream.Stream

/**
 * Created by darksnake on 06-Jul-17.
 */
public interface NumassPoint : ParentBlock, Provider {

    public val meta: Meta

    override val blocks: List<NumassBlock>

    /**
     * Distinct map of channel number to corresponding grouping block
     */
    public val channels: Map<Int, NumassBlock>
        get() = blocks.toList().groupBy { it.channel }.mapValues { entry ->
            if (entry.value.size == 1) {
                entry.value.first()
            } else {
                MetaBlock(entry.value)
            }
        }

    override fun content(target: String): Map<Name, Any> = when (target) {
        NUMASS_BLOCK_TARGET -> blocks.mapIndexed { index, numassBlock ->
            "block[$index]".toName() to numassBlock
        }.toMap()
        NUMASS_CHANNEL_TARGET -> channels.mapKeys { "channel[${it.key}]".toName() }
        else -> super.content(target)
    }

    /**
     * Get the voltage setting for the point
     *
     * @return
     */
    public val voltage: Double get() = meta[HV_KEY].double ?: 0.0

    /**
     * Get the index for this point in the set
     * @return
     */
    public val index: Int get() = meta[INDEX_KEY].int ?: -1

    /**
     * Get the starting time from meta or from first block
     *
     * @return
     */
    override val startTime: Instant
        get() = meta[START_TIME_KEY]?.long?.let { Instant.ofEpochMilli(it) } ?: firstBlock.startTime

    /**
     * Get the length key of meta or calculate length as a sum of block lengths. The latter could be a bit slow
     *
     * @return
     */
    override val length: Duration
        get() = Duration.ofNanos(blocks.stream().filter { it.channel == 0 }.mapToLong { it -> it.length.toNanos() }
            .sum())

    /**
     * Get all events it all blocks as a single sequence
     *
     *
     * Some performance analysis of different stream concatenation approaches is given here: https://www.techempower.com/blog/2016/10/19/efficient-multiple-stream-concatenation-in-java/
     *
     *
     * @return
     */
    override val events: Flow<NumassEvent>
        get() = blocks.asFlow().flatMapConcat { it.events }

    /**
     * Get all frames in all blocks as a single sequence
     *
     * @return
     */
    override val frames: Flow<NumassFrame>
        get() = blocks.asFlow().flatMapConcat { it.frames }


    override val isSequential: Boolean
        get() = channels.size == 1

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
 * Get the first block if it exists. Throw runtime exception otherwise.
 *
 */
public val NumassPoint.firstBlock: NumassBlock
    get() = blocks.firstOrNull() ?: throw RuntimeException("The point is empty")
