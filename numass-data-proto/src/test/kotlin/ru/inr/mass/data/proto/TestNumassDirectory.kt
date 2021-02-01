package ru.inr.mass.data.proto

import hep.dataforge.context.Context
import hep.dataforge.meta.get
import hep.dataforge.meta.string
import hep.dataforge.meta.value
import hep.dataforge.values.ListValue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals

class TestNumassDirectory {
    val context = Context("numass-test") {
        plugin(NumassProtoPlugin)
    }

    @Test
    fun testDirectoryRead() {
        val dataPath = Path.of("src/test/resources", "testData/set_4")
        val testSet = context.readNumassDirectory(dataPath)
        assertEquals("2018-04-13T22:01:46", testSet.meta["end_time"].string)
        assertEquals(ListValue.EMPTY, testSet.meta["comments"].value)
        assertEquals(31, testSet.points.size)
        val point22 = testSet.points.find { it.index == 22 }!!
        point22.flowBlocks()
        assertEquals("2018-04-13T21:56:09", point22.meta["end_time"].string)
    }
}