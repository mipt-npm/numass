package ru.inr.mass.data.proto

import hep.dataforge.context.Context
import hep.dataforge.context.logger
import hep.dataforge.io.io
import hep.dataforge.io.readEnvelopeFile
import hep.dataforge.meta.DFExperimental
import hep.dataforge.meta.Meta
import ru.inr.mass.data.api.NumassPoint
import ru.inr.mass.data.api.NumassSet
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.streams.toList

@OptIn(ExperimentalPathApi::class)
public class NumassDirectorySet internal constructor(
    public val context: Context,
    public val path: Path,
) : NumassSet {

    @OptIn(DFExperimental::class)
    override val meta: Meta by lazy {
        val metaFilePath = path / "meta"
        if (metaFilePath.exists()) {
            val envelope = context.io.readEnvelopeFile(metaFilePath) ?: error("Envelope could not be read from $metaFilePath")
            envelope.meta
        } else {
            context.logger.warn { "Meta file does not exist for Numass set $metaFilePath" }
            Meta.EMPTY
        }
    }

    override val points: List<NumassPoint> by lazy<List<NumassPoint>> {
        Files.list(path).filter {
            it.fileName.name.startsWith("p")
        }.map { pointPath ->
            try {
                context.readNumassFile(pointPath)
            } catch (e: Exception) {
                context.logger.error(e) { "Error reading Numass point file $pointPath" }
                null
            }
        }.toList().filterNotNull()
    }
}

@OptIn(DFExperimental::class)
public fun Context.readNumassFile(path: Path): ProtoNumassPoint? {
    val envelope = io.readEnvelopeFile(path) ?: error("Envelope could not be read from $path")
    return ProtoNumassPoint.fromEnvelope(envelope)
}

public fun Context.readNumassFile(path: String): ProtoNumassPoint? = readNumassFile(Path.of(path))

@OptIn(ExperimentalPathApi::class)
public fun Context.readNumassDirectory(path: Path): NumassDirectorySet {
    if(!path.exists()) error("Path $path does not exist")
    if(!path.isDirectory()) error("The path $path is not a directory")
    return NumassDirectorySet(this, path)
}

public fun Context.readNumassDirectory(path: String): NumassDirectorySet = readNumassDirectory(Path.of(path))