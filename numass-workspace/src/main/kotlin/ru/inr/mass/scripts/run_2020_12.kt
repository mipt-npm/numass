package ru.inr.mass.scripts

import kotlinx.coroutines.flow.collect
import ru.inr.mass.data.proto.NumassDirectorySet
import ru.inr.mass.workspace.readNumassRepository
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.filter
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string

suspend fun main() {
    val repo: DataTree<NumassDirectorySet> = readNumassRepository("D:\\Work\\Numass\\data\\2018_04")
    val filtered = repo.filter { _, data ->
        data.meta["operator"].string?.startsWith("Vas") ?: false
    }

    filtered.flow().collect {
        println(it)
    }
}