package ru.inr.mass.models

import kotlin.test.Test
import kotlin.test.assertEquals

internal class TestGenerateRanges {
    @Test
    fun simpleRanges() {
        val ranges = generateRanges(0.0..100.0, 10.0, 55.0, 120.0)
        assertEquals(3, ranges.ranges.size)
        assertEquals(55.0..100.0, ranges.ranges.last().first)
        assertEquals(10.0..55.0, ranges.ranges[1].first)
    }
}