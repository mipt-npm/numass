package ru.inr.mass.scripts

import kotlinx.coroutines.flow.toList
import ru.inr.mass.workspace.Numass.readPoint

suspend fun main() {
    val point = readPoint("D:\\Work\\Numass\\data\\2019_11\\Fill_3\\set_2\\p2(30s)(HV1=14000)")
    val events = point.events.toList()
}