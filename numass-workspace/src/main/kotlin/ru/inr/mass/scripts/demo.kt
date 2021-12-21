package ru.inr.mass.scripts

import ru.inr.mass.data.proto.NumassDirectorySet
import ru.inr.mass.workspace.Numass.readRepository
import ru.inr.mass.workspace.plotNumassSet
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.await
import space.kscience.dataforge.data.getData
import space.kscience.plotly.Plotly
import space.kscience.plotly.makeFile

suspend fun main() {

    val repo: DataTree<NumassDirectorySet> = readRepository("D:\\Work\\Numass\\data\\2018_04")
    val testSet = repo.getData("Adiabacity_19.set_3")?.await() ?: error("Not found")
    Plotly.plotNumassSet(testSet).makeFile()
}