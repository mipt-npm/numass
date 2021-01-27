package ru.inr.mass.data.proto

import java.time.Instant


internal fun epochNanoTime(nanos: Long): Instant {
    val seconds = Math.floorDiv(nanos, 1e9.toInt().toLong())
    val reminder = (nanos % 1e9).toInt()
    return Instant.ofEpochSecond(seconds, reminder.toLong())
}