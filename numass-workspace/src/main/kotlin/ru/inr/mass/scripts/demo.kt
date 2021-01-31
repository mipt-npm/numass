package ru.inr.mass.workspace

import kscience.plotly.makeFile
import ru.inr.mass.data.proto.readNumassDirectory
import java.nio.file.Path

fun main() {
    val dataPath = Path.of("D:\\Work\\Numass\\data\\2018_04\\Adiabacity_19\\set_4\\")
    val testSet = NUMASS.context.readNumassDirectory(dataPath)
    testSet.plotlyPage().makeFile()
}