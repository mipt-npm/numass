package ru.inr.mass.workspace

import hep.dataforge.data.ActiveDataTree
import hep.dataforge.data.DataTree
import hep.dataforge.data.emitStatic
import hep.dataforge.names.Name
import hep.dataforge.names.NameToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.inr.mass.data.proto.NumassDirectorySet
import ru.inr.mass.data.proto.readNumassDirectory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.relativeTo
import kotlin.streams.toList

fun readNumassDirectory(path: String): NumassDirectorySet = NUMASS.context.readNumassDirectory(path)

@OptIn(ExperimentalPathApi::class)
suspend fun readNumassRepository(path: String): DataTree<NumassDirectorySet> = ActiveDataTree {
    val basePath = Path.of(path)
    @Suppress("BlockingMethodInNonBlockingContext")
    withContext(Dispatchers.IO) {
        Files.walk(Path.of(path)).filter {
            it.isDirectory() && it.resolve("meta").exists()
        }.toList().forEach { childPath ->
            val name = Name(childPath.relativeTo(basePath).map { segment ->
                NameToken(segment.fileName.toString())
            })
            val value = NUMASS.context.readNumassDirectory(childPath)
            emitStatic(name, value, value.meta)
        }
    }
    //TODO add file watcher
}
