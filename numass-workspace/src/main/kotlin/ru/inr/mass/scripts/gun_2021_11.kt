package ru.inr.mass.scripts

import ru.inr.mass.data.api.channels
import ru.inr.mass.data.proto.NumassDirectorySet
import ru.inr.mass.workspace.Numass.readRepository
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.await
import space.kscience.dataforge.data.data
import space.kscience.plotly.Plotly
import space.kscience.plotly.histogram
import space.kscience.plotly.makeFile

suspend fun main() {
    val repo: DataTree<NumassDirectorySet> = readRepository("D:\\Work\\Numass\\data\\2021_11\\Adiabacity_17\\")

    //select point number 2 (U = 16900 V) from each directory
    val points = repo.items().mapValues {
        val directory = it.value.data?.await()
        val point = directory?.points?.find { it.voltage == 16900.0 }
        point
    }

    Plotly.plot {
        points.forEach { name, point ->
            if (point != null) {
                histogram {
                    this.name = name.toString()
                    xbins {
                        size = 4.0
                    }
                    x.numbers = point.frames.tqdcAmplitudes()
                }
            }
        }
    }.makeFile()


}