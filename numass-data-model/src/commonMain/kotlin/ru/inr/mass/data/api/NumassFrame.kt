package ru.inr.mass.data.api

import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * The continuous frame of digital detector data
 * Created by darksnake on 06-Jul-17.
 * @param time The absolute start time of the frame
 * @param tickSize The time interval per tick
 * @param signal The buffered signal shape in ticks
 */
public class NumassFrame(
    public val time: Instant,
    public val tickSize: Duration,
    public val signal: ShortArray,
) {
    public val length: Duration get() = tickSize * signal.size
}
