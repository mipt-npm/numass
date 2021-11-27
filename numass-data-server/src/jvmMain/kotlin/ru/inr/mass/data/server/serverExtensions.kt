package ru.inr.mass.data.server

import space.kscience.dataforge.context.Context
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.visionforge.html.HtmlVisionFragment
import space.kscience.visionforge.html.ResourceLocation
import space.kscience.visionforge.html.page
import space.kscience.visionforge.html.scriptHeader
import space.kscience.visionforge.makeFile
import space.kscience.visionforge.three.server.VisionServer
import space.kscience.visionforge.three.server.useScript
import java.awt.Desktop
import java.nio.file.Path


public fun VisionServer.useNumassWeb(): Unit {
    useScript("js/numass-web.js")
}

@DFExperimental
public fun Context.makeNumassWebFile(
    content: HtmlVisionFragment,
    path: Path? = null,
    title: String = "VisionForge Numass page",
    resourceLocation: ResourceLocation = ResourceLocation.SYSTEM,
    show: Boolean = true,
): Unit {
    val actualPath = page(title, content = content).makeFile(path) { actualPath ->
        mapOf("numassWeb" to scriptHeader("js/numass-web.js", resourceLocation, actualPath))
    }
    if (show) Desktop.getDesktop().browse(actualPath.toFile().toURI())
}
