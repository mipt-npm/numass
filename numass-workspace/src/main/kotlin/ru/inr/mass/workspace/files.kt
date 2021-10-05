package ru.inr.mass.workspace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.inr.mass.data.proto.NumassDirectorySet
import ru.inr.mass.data.proto.readNumassDirectory
import space.kscience.dataforge.data.ActiveDataTree
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.static
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.relativeTo
import kotlin.streams.toList

fun readNumassDirectory(path: String): NumassDirectorySet = NUMASS.context.readNumassDirectory(path)

@OptIn(ExperimentalPathApi::class)
suspend fun readNumassRepository(path: Path): DataTree<NumassDirectorySet> = ActiveDataTree {
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

suspend fun readNumassRepository(path: String): DataTree<NumassDirectorySet> = readNumassRepository(Path.of(path))
