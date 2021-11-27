package ru.inr.mass.data.server

import kotlinx.coroutines.runBlocking
import kotlinx.html.div
import kotlinx.html.h1
import ru.inr.mass.data.api.NumassPoint
import ru.inr.mass.data.proto.NumassProtoPlugin
import ru.inr.mass.data.proto.readNumassPointFile
import space.kscience.dataforge.context.Context
import space.kscience.visionforge.three.server.close
import space.kscience.visionforge.three.server.serve
import space.kscience.visionforge.three.server.show
import space.kscience.visionforge.visionManager
import java.nio.file.Path


public fun main() {
    val context = Context("Numass") {
        plugin(NumassProtoPlugin)
        plugin(NumassCommonPlugin)
    }

    val pointPath = Path.of("C:\\Users\\altavir\\Desktop\\p20211122173034(20s).dat")
    val point: NumassPoint = context.readNumassPointFile(pointPath)!!

    val visionOfNumass = runBlocking {
        point.toVision()
    }

    val server = context.visionManager.serve {
        //use client library
        useNumassWeb()
        //use css
        //useCss("css/styles.css")
        page {
            div("flex-column") {
                h1 { +"Satellite detector demo" }
                //vision(visionOfNumass)
            }
        }
    }

    server.show()


    println("Enter 'exit' to close server")
    while (readLine() != "exit") {
        //
    }

    server.close()

}