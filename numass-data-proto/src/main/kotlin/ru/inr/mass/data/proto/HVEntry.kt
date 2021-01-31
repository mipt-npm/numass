package ru.inr.mass.data.proto

import hep.dataforge.io.Envelope
import hep.dataforge.meta.get
import hep.dataforge.meta.string
import kotlinx.datetime.Instant
import kotlinx.datetime.toInstant
import kotlinx.io.asInputStream

public data class HVEntry(val timestamp: Instant, val value: Double, val channel: Int = 1) {
    public companion object {
        public fun readString(line: String): HVEntry {
            val (timeStr, channelStr, valueStr) = line.split(' ')
            return HVEntry((timeStr+"Z").toInstant(), valueStr.toDouble(), channelStr.toInt())
        }

        public fun readEnvelope(envelope: Envelope): List<HVEntry> {
            check(envelope.meta["type"].string == "voltage"){"Expecting voltage type envelope"}
            return buildList {
                envelope.data?.read {
                    //Some problems with readLines
                    asInputStream().bufferedReader().lines().forEach { str->
                        add(readString(str))
                    }
                }
            }
        }
    }
}

