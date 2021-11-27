package ru.inr.mass.data.api

import ru.inr.mass.data.proto.HVData
import ru.inr.mass.data.proto.HVEntry
import ru.inr.mass.data.proto.lines
import space.kscience.dataforge.io.Envelope
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string

public fun HVData.Companion.readEnvelope(envelope: Envelope): HVData {
    check(envelope.meta["type"].string == "voltage") { "Expecting voltage type envelope" }
    return HVData(buildList {
        envelope.data?.read {
            //Some problems with readLines
            lines().forEach { str ->
                add(HVEntry.readString(str))
            }
        }
    })
}