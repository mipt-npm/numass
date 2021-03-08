package ru.inr.mass.data.proto

import kotlinx.datetime.Instant
import kotlinx.datetime.toInstant
import space.kscience.dataforge.io.Envelope
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string

public data class HVEntry(val timestamp: Instant, val value: Double, val channel: Int = 1) {
    public companion object {
        public fun readString(line: String): HVEntry {
            val (timeStr, channelStr, valueStr) = line.split(' ')
            return HVEntry((timeStr + "Z").toInstant(), valueStr.toDouble(), channelStr.toInt())
        }

        public fun readEnvelope(envelope: Envelope): List<HVEntry> {
            check(envelope.meta["type"].string == "voltage") { "Expecting voltage type envelope" }
            return buildList {
                envelope.data?.read {
                    //Some problems with readLines
                    lines().forEach { str ->
                        add(readString(str))
                    }
                }
            }
        }
    }
}

