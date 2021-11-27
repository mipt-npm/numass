package ru.inr.mass.data.server

import space.kscience.dataforge.misc.DFExperimental
import space.kscience.visionforge.runVisionClient


@DFExperimental
public fun main(): Unit = runVisionClient {
    plugin(NumassJsPlugin)
}