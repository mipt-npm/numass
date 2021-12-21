package ru.inr.mass.data.proto

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import ru.inr.mass.data.api.NumassSet
import ru.inr.mass.data.api.ParentBlock
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.values.ListValue
import java.nio.file.Path
import kotlin.test.assertEquals

class TestNumassDirectory {
    val context = Context("numass-test") {
        plugin(NumassProtoPlugin)
    }

    @Test
    fun testDanteRead() {
        val dataPath = Path.of("src/test/resources", "testData/dante")
        val testSet = context.readNumassDirectory(dataPath)
        assertEquals("2018-04-13T22:01:46", testSet.meta["end_time"].string)
        assertEquals(ListValue.EMPTY, testSet.meta["comments"]?.value)
        assertEquals(31, testSet.points.size)
        val point22 = testSet.points.find { it.index == 22 }!!
        point22.flowBlocks()
        assertEquals("2018-04-13T21:56:09", point22.meta["end_time"].string)
    }

    @Test
    fun testTQDCRead() = runBlocking {
        val pointPath = Path.of("src/test/resources", "testData/tqdc")
        val set: NumassSet = context.readNumassDirectory(pointPath)
        val point = set.first { it.voltage == 18200.0 }
        point.getChannels().forEach { (channel, block) ->
            println("$channel: $block")
            if(block is ParentBlock){
                block.flowBlocks().toList().forEach{
                    println("\t${it.channel}:${it.eventsCount}")
                }
            }
        }
    }
}