package ru.inr.mass.data.proto

import ru.inr.mass.data.api.NumassPoint
import ru.inr.mass.data.api.NumassSet
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.error
import space.kscience.dataforge.context.logger
import space.kscience.dataforge.context.warn
import space.kscience.dataforge.io.io
import space.kscience.dataforge.io.readEnvelopeFile
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFExperimental
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
            val envelope = context.io.readEnvelopeFile(metaFilePath)
            envelope.meta
        } else {
            context.logger.warn { "Meta file does not exist for Numass set $metaFilePath" }
            Meta.EMPTY
        }
    }

    override val points: List<NumassPoint> by lazy {
        Files.list(path).filter {
            it.fileName.name.startsWith("p")
        }.map { pointPath ->
            try {
                context.readNumassPointFile(pointPath)
            } catch (e: Exception) {
                context.logger.error(e) { "Error reading Numass point file $pointPath" }
                null
            }
        }.toList().filterNotNull()
    }

    @OptIn(DFExperimental::class)
    public fun getHvData(): List<HVEntry>? {
        val hvFile = path / "voltage"
        return if (hvFile.exists()) {
            val envelope = context.io.readEnvelopeFile(hvFile)
            HVEntry.readEnvelope(envelope)
        } else {
            null
        }
    }
}

@OptIn(DFExperimental::class)
public fun Context.readNumassPointFile(path: Path): NumassPoint? {
    val envelope = io.readEnvelopeFile(path)
    return ProtoNumassPoint.fromEnvelope(envelope)
}

public fun Context.readNumassPointFile(path: String): NumassPoint? = readNumassPointFile(Path.of(path))

@OptIn(ExperimentalPathApi::class)
public fun Context.readNumassDirectory(path: Path): NumassDirectorySet {
    if(!path.exists()) error("Path $path does not exist")
    if(!path.isDirectory()) error("The path $path is not a directory")
    return NumassDirectorySet(this, path)
}

public fun Context.readNumassDirectory(path: String): NumassDirectorySet = readNumassDirectory(Path.of(path))