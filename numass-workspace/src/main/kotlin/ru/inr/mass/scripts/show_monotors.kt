package ru.inr.mass.scripts

import ru.inr.mass.data.api.NumassPoint
import ru.inr.mass.workspace.Numass
import space.kscience.plotly.Plotly
import space.kscience.plotly.makeFile
import space.kscience.plotly.scatter

fun main() {
    val directory = Numass.readDirectory("D:\\Work\\numass-data\\set_3\\")

    val monitorPoints: List<NumassPoint> = directory.filter { it.voltage == 14000.0 }.sortedBy { it.startTime  }

    Plotly.plot {
        scatter {
            x.numbers = monitorPoints.map {
                it.startTime.toEpochMilliseconds()
            }
            y.numbers = monitorPoints.map { it.framesCount }
        }
    }.makeFile()
}