package ru.inr.mass.scripts

import ru.inr.mass.data.api.NumassBlock
import ru.inr.mass.data.api.channels
import ru.inr.mass.workspace.Numass
import ru.inr.mass.workspace.listFrames
import space.kscience.dataforge.io.write
import space.kscience.dataforge.io.writeUtf8String
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

fun main() {

    val point = Numass.readPoint("D:\\Work\\numass-data\\set_3\\p101(30s)(HV1=14150)")
    val channels: Map<Int, NumassBlock> = point.channels

    //Initialize and create target directory
    val targetDir = Files.createTempDirectory("numass_p101(30s)(HV1=14150)")
    targetDir.createDirectories()

    //dumping meta
    targetDir.resolve("meta").writeText(point.meta.toString())

    channels.forEach { (key, block) ->
        targetDir.resolve("channel-$key.csv").write {
            block.listFrames().forEach { frame ->
                val line = frame.signal.joinToString(", ", postfix = "\n" )
                writeUtf8String(line)
            }
        }
    }

    println("Exported to $targetDir")
}