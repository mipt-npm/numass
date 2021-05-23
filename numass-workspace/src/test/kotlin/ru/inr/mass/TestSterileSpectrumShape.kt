package ru.inr.mass

import ru.inr.mass.models.*
import space.kscience.kmath.misc.Symbol
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertTrue

internal class TestSterileSpectrumShape {
    val args: Map<Symbol, Double> = mapOf(
        NBkgSpectrum.norm to 8e5,
        NBkgSpectrum.bkg to 2.0,
        NumassBeta.mnu2 to 0.0,
        NumassBeta.e0 to 18575.0,
        NumassBeta.msterile2 to 1000.0.pow(2),
        NumassBeta.u2 to 1e-2,
        NumassTransmission.thickness to 0.1,
        NumassTransmission.trap to 1.0
    )

    val spectrum = SterileNeutrinoSpectrum(fss = FSS.default).withNBkg()

    val data by lazy {
        javaClass.getResource("/old-spectrum.dat").readText().lines().map {
            val (u, oldValue) = it.split("\t")
            u.toDouble() to oldValue.toDouble()
        }
    }

    @Test
    @Ignore
    fun checkAbsoluteDif() {
        val t = 1e6

        val res = data.sumOf { (u, oldValue) ->
            val newValue = spectrum(u, args)
            abs(newValue - oldValue) * sqrt(t) / sqrt(oldValue)
        }

        println(res / data.size)

        assertTrue { res / data.size < 0.1 }
    }
}