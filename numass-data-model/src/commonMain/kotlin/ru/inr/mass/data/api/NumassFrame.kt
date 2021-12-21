package ru.inr.mass.data.api

import kotlin.time.Duration


/**
 * The continuous frame of digital detector data
 * Created by darksnake on 06-Jul-17.
 * @param timeOffset The time offset relative to block start in nanos
 * @param tickSize The time interval per tick
 * @param signal The buffered signal shape in ticks
 */
public class NumassFrame(
    public val timeOffset: Long,
    public val tickSize: Duration,
    public val signal: ShortArray,
) {
    public val length: Duration get() = tickSize * signal.size
}
