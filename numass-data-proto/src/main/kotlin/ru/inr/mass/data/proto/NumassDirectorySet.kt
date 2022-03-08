package ru.inr.mass.data.proto

import ru.inr.mass.data.api.NumassPoint
import ru.inr.mass.data.api.NumassSet
import ru.inr.mass.data.api.NumassSet.Companion.NUMASS_HV_TARGET
import ru.inr.mass.data.api.readEnvelope
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.error
import space.kscience.dataforge.context.logger
import space.kscience.dataforge.context.warn
import space.kscience.dataforge.io.io
import space.kscience.dataforge.io.readEnvelopeFile
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.streams.toList

public class NumassDirectorySet internal constructor(
    public val context: Context,
    public val path: Path,
) : NumassSet {

    @OptIn(DFExperimental::class)
    override val meta: Meta
        get() {
            val metaFilePath = path / "meta"
            return if (metaFilePath.exists()) {
                val envelope = context.io.readEnvelopeFile(metaFilePath)
                envelope.meta
            } else {
                context.logger.warn { "Meta file does not exist for Numass set $metaFilePath" }
                Meta.EMPTY
            }
        }

    override val points: List<NumassPoint>
        get() = Files.list(path).filter {
            it.fileName.name.startsWith("p")
        }.map { pointPath ->
            try {
                context.readNumassPointFile(pointPath)
            } catch (e: Exception) {
                context.logger.error(e) { "Error reading Numass point file $pointPath" }
                null
            }
        }.toList().filterNotNull()


    @OptIn(DFExperimental::class)
    public fun getHvData(): HVData? {
        val hvFile = path / "voltage"
        return if (hvFile.exists()) {
            val envelope = context.io.readEnvelopeFile(hvFile)
            HVData.readEnvelope(envelope)
        } else {
            null
        }
    }

    override fun content(target: String): Map<Name, Any> = if (target == NUMASS_HV_TARGET) {
        val hvData = getHvData()
        if (hvData != null) {
            mapOf("hv".asName() to hvData)
        } else {
            emptyMap()
        }
    } else super.content(target)

    public companion object
}

@OptIn(DFExperimental::class)
public fun Context.readNumassPointFile(path: Path): NumassPoint? {
    val envelope = io.readEnvelopeFile(path)
    return ProtoNumassPoint.fromEnvelope(envelope)
}

public fun Context.readNumassPointFile(path: String): NumassPoint? = readNumassPointFile(Path.of(path))

@OptIn(ExperimentalPathApi::class)
public fun Context.readNumassDirectory(path: Path): NumassDirectorySet {
    if (!path.exists()) error("Path $path does not exist")
    if (!path.isDirectory()) error("The path $path is not a directory")
    return NumassDirectorySet(this, path)
}

public fun Context.readNumassDirectory(path: String): NumassDirectorySet = readNumassDirectory(Path.of(path))