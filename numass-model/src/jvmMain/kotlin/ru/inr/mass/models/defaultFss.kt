package ru.inr.mass.models

import space.kscience.kmath.real.div
import space.kscience.kmath.real.sum
import space.kscience.kmath.structures.DoubleBuffer

private val defaultFss: FSS by lazy {
    val stream = FSS::class.java.getResourceAsStream("/data/FS.txt") ?: error("Default FS resource not found")
    stream.use { inputStream ->
        val data = inputStream.bufferedReader().lineSequence().map {
            val (e, p) = it.split("\t")
            e.toDouble() to p.toDouble()
        }.toList()
        val es = DoubleBuffer(data.size) { data[it].first }
        val ps = DoubleBuffer(data.size) { data[it].second }
        FSS(es, ps / ps.sum())
    }
}

public val FSS.Companion.default: FSS get() = defaultFss