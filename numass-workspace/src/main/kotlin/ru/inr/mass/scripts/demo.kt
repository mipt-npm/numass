package ru.inr.mass.workspace

import hep.dataforge.data.await
import hep.dataforge.names.toName
import kscience.plotly.Plotly
import kscience.plotly.makeFile

suspend fun main() {
    val repo = readNumassRepository("D:\\Work\\Numass\\data\\2018_04")
    //val dataPath = Path.of("D:\\Work\\Numass\\data\\2018_04\\Adiabacity_19\\set_4\\")
    //val testSet = NUMASS.context.readNumassDirectory(dataPath)
    val testSet = repo.getData("Adiabacity_19.set_4".toName())!!.await()
    Plotly.numassDirectory(testSet).makeFile()
}