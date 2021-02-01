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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import kotlin.time.Duration

public open class OrphanNumassEvent(
    public val amplitude: Short,
    public val timeOffset: Long,
) : Comparable<OrphanNumassEvent> {
    public operator fun component1(): Short = amplitude
    public operator fun component2(): Long = timeOffset

    override fun compareTo(other: OrphanNumassEvent): Int {
        return this.timeOffset.compareTo(other.timeOffset)
    }
}

/**
 * A single numass event with given amplitude and time.
 *
 * @author Darksnake
 * @property amp the amplitude of the event
 * @property timeOffset time in nanoseconds relative to block start
 * @property owner an owner block for this event
 *
 */
public class NumassEvent(
    amplitude: Short,
    timeOffset: Long,
    public val owner: NumassBlock,
) : OrphanNumassEvent(amplitude, timeOffset)

public val NumassEvent.channel: Int get() = owner.channel

public fun NumassEvent.getTime(): Instant = owner.startTime.plus(timeOffset, DateTimeUnit.NANOSECOND)


/**
 * A single continuous measurement block. The block can contain both isolated events and signal frames
 *
 *
 * Created by darksnake on 06-Jul-17.
 */
public interface NumassBlock {

    /**
     * The absolute start time of the block
     */
    public val startTime: Instant

    /**
     * The length of the block
     */
    public suspend fun getLength(): Duration

    /**
     * Stream of isolated events. Could be empty
     */
    public val events: Flow<NumassEvent>

    /**
     * Stream of frames. Could be empty
     */
    public val frames: Flow<NumassFrame>

    public val channel: Int get() = 0
}

public fun OrphanNumassEvent.adopt(parent: NumassBlock): NumassEvent {
    return NumassEvent(this.amplitude, this.timeOffset, parent)
}

/**
 * A simple in-memory implementation of block of events. No frames are allowed
 * Created by darksnake on 08.07.2017.
 */
public class SimpleBlock(
    override val startTime: Instant,
    private val length: Duration,
    rawEvents: Iterable<OrphanNumassEvent>,
) : NumassBlock {

    override suspend fun getLength(): Duration = length

    private val eventList by lazy { rawEvents.map { it.adopt(this) } }

    override val frames: Flow<NumassFrame> get() = emptyFlow()

    override val events: Flow<NumassEvent> get() = eventList.asFlow()

    public companion object {

    }
}

public suspend fun SimpleBlock(
    startTime: Instant,
    length: Duration,
    producer: suspend () -> Iterable<OrphanNumassEvent>,
): SimpleBlock {
    return SimpleBlock(startTime, length, producer())
}