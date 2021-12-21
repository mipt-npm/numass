package ru.inr.mass.workspace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ru.inr.mass.data.api.NumassBlock
import ru.inr.mass.data.api.NumassPoint
import ru.inr.mass.data.api.NumassSet
import ru.inr.mass.data.proto.NumassDirectorySet
import ru.inr.mass.data.proto.readNumassDirectory
import ru.inr.mass.data.proto.readNumassPointFile
import space.kscience.dataforge.data.*
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.relativeTo
import kotlin.streams.toList

object Numass {
    fun readDirectory(path: String): NumassDirectorySet = NUMASS.context.readNumassDirectory(path)

    @OptIn(ExperimentalPathApi::class)
    fun readRepository(path: Path): DataTree<NumassDirectorySet> = runBlocking {
        ActiveDataTree {
            @Suppress("BlockingMethodInNonBlockingContext")
            withContext(Dispatchers.IO) {
                Files.walk(path).filter {
                    it.isDirectory() && it.resolve("meta").exists()
                }.toList().forEach { childPath ->
                    val name = Name(childPath.relativeTo(path).map { segment ->
                        NameToken(segment.fileName.toString())
                    })
                    val value = NUMASS.context.readNumassDirectory(childPath)
                    static(name, value, value.meta)
                }
            }
            //TODO add file watcher
        }
    }

    fun readRepository(path: String): DataTree<NumassDirectorySet> = readRepository(Path.of(path))

    fun readPoint(path: String): NumassPoint = NUMASS.context.readNumassPointFile(path)
        ?: error("Can't read numass point at $path")
}

operator fun DataSet<NumassSet>.get(name: String): NumassSet? = runBlocking {
    getData(Name.parse(name))?.await()
}

fun NumassBlock.listFrames() = runBlocking { frames.toList() }

fun NumassBlock.listEvents() = runBlocking { events.toList() }