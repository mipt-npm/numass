package ru.inr.mass.scripts

import ru.inr.mass.workspace.buffer
import space.kscience.kmath.functions.asFunction
import space.kscience.kmath.integration.integrate
import space.kscience.kmath.integration.splineIntegrator
import space.kscience.kmath.integration.value
import space.kscience.kmath.interpolation.interpolatePolynomials
import space.kscience.kmath.interpolation.splineInterpolator
import space.kscience.kmath.operations.DoubleField
import space.kscience.kmath.real.step
import space.kscience.plotly.Plotly
import space.kscience.plotly.layout
import space.kscience.plotly.makeFile
import space.kscience.plotly.models.AxisType
import space.kscience.plotly.scatter
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

fun main() {
    val backScatteringSpectrum: List<Pair<Double, Double>> = {}.javaClass
        .getResource("/simulation/Gun19_E_back_scatt.dat")!!.readText()
        .lineSequence().drop(2).mapNotNull {
            if (it.isBlank()) return@mapNotNull null
            val (e, p) = it.split('\t')
            Pair(e.toDouble(), p.toDouble())
        }.toList()

    val interpolated = DoubleField.splineInterpolator
        .interpolatePolynomials(backScatteringSpectrum)
        .asFunction(DoubleField, 0.0)

    val sigma = 0.3
    val detectorResolution: (Double) -> Double = { x ->
        1.0 / sqrt(2 * PI) / sigma * exp(-(x / sigma).pow(2) / 2.0)
    }

    val convoluted: (Double) -> Double = { x ->
        DoubleField.splineIntegrator.integrate(-2.0..2.0) { y ->
            detectorResolution(y) * interpolated(x - y)
        }.value
    }

    Plotly.plot {
//        scatter {
//            name = "simulation"
//            x.numbers = backScatteringSpectrum.map { 19.0 - it.first }
//            y.numbers = backScatteringSpectrum.map { it.second }
//        }
        scatter {
            name = "smeared"
            x.buffer = 0.0..20.0 step 0.1
            y.numbers = x.doubles.map { convoluted(19.0 - it) * 0.14/0.01 + 0.86 * detectorResolution(it - 19.0) }
            println(y.doubles.sum()*0.1)//Norm check
        }
        layout {
            yaxis.type = AxisType.log
        }
    }.makeFile()

}