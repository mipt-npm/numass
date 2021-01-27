package ru.inr.mass.data.proto

import hep.dataforge.context.Context
import hep.dataforge.context.logger
import hep.dataforge.io.io
import hep.dataforge.io.readEnvelopeFile
import hep.dataforge.meta.Meta
import ru.inr.mass.data.api.NumassPoint
import ru.inr.mass.data.api.NumassSet
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.streams.toList

@OptIn(ExperimentalPathApi::class)
public class NumassDirectorySet internal constructor(
    public val context: Context,
    public val path: Path,
) : NumassSet {

    override val meta: Meta by lazy {
        val metaFilePath = path / "meta"
        if (metaFilePath.exists()) {
            val envelope = context.io.readEnvelopeFile(path) ?: error("Envelope could not be read from $path")
            envelope.meta
        } else {
            Meta.EMPTY
        }
    }

    override val points: List<NumassPoint> by lazy<List<NumassPoint>> {
        Files.list(path).filter { it.fileName.startsWith("p") }.map { path ->
            try {
                context.readNumassFile(path)
            } catch (e: Exception) {
                context.logger.error(e) { "Error reading Numass point file $path" }
                null
            }
        }.toList().filterNotNull()
    }
}

@OptIn(ExperimentalPathApi::class)
public fun Context.readNumassDirectory(path: Path): NumassDirectorySet {
    if(!path.exists()) error("Path $path does not exist")
    if(!path.isDirectory()) error("The path $path is not a directory")
    return NumassDirectorySet(this, path)
}