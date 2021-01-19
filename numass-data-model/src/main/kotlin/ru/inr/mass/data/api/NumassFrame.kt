package ru.inr.mass.data.api

import java.nio.ShortBuffer
import java.time.Duration
import java.time.Instant

/**
 * The continuous frame of digital detector data
 * Created by darksnake on 06-Jul-17.
 */
public class NumassFrame(
        /**
         * The absolute start time of the frame
         */
        public val time: Instant,
        /**
         * The time interval per tick
         */
        public val tickSize: Duration,
        /**
         * The buffered signal shape in ticks
         */
        public val signal: ShortBuffer) {

    public val length: Duration
        get() = tickSize.multipliedBy(signal.capacity().toLong())
}
