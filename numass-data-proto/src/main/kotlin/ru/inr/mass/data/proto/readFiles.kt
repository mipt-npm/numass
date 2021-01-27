package ru.inr.mass.data.proto

import hep.dataforge.context.Context
import hep.dataforge.io.io
import hep.dataforge.io.readEnvelopeFile
import java.nio.file.Path

public fun Context.readNumassFile(path: Path): ProtoNumassPoint? {
    val envelope = io.readEnvelopeFile(path) ?: error("Envelope could not be read from $path")
    return ProtoNumassPoint.fromEnvelope(envelope)
}

public fun Context.readNumassFile(path: String): ProtoNumassPoint? = readNumassFile(Path.of(path))