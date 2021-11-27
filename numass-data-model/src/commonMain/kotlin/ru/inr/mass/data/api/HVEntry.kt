package ru.inr.mass.data.proto

import kotlinx.datetime.Instant
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
public data class HVEntry(val timestamp: Instant, val value: Double, val channel: Int = 1) {
    public companion object {
        public fun readString(line: String): HVEntry {
            val (timeStr, channelStr, valueStr) = line.split(' ')
            return HVEntry((timeStr + "Z").toInstant(), valueStr.toDouble(), channelStr.toInt())
        }
    }
}

@Serializable
@JvmInline
public value class HVData(public val list: List<HVEntry>) : Iterable<HVEntry> {
    override fun iterator(): Iterator<HVEntry> = list.iterator()

    public companion object
}

