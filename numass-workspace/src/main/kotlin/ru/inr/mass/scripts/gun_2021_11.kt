package ru.inr.mass.scripts

import ru.inr.mass.data.analysis.NumassEventExtractor
import ru.inr.mass.data.analysis.energySpectrum
import ru.inr.mass.data.api.NumassEvent
import ru.inr.mass.data.api.NumassPoint
import ru.inr.mass.data.api.channel
import ru.inr.mass.data.proto.NumassDirectorySet
import ru.inr.mass.models.*
import ru.inr.mass.workspace.Numass
import ru.inr.mass.workspace.Numass.readRepository
import ru.inr.mass.workspace.buffer
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.await
import space.kscience.dataforge.data.data
import space.kscience.dataforge.names.NameToken
import space.kscience.kmath.expressions.Symbol
import space.kscience.kmath.functions.PiecewisePolynomial
import space.kscience.kmath.functions.asFunction
import space.kscience.kmath.integration.integrate
import space.kscience.kmath.integration.splineIntegrator
import space.kscience.kmath.integration.value
import space.kscience.kmath.interpolation.LinearInterpolator
import space.kscience.kmath.interpolation.interpolatePolynomials
import space.kscience.kmath.operations.DoubleField
import space.kscience.kmath.real.step
import space.kscience.kmath.structures.asBuffer
import space.kscience.plotly.Plotly
import space.kscience.plotly.makeFile
import space.kscience.plotly.scatter
import kotlin.math.pow

fun Spectrum.cutFrom(lowerCut: Double): Spectrum = Spectrum { x, arguments ->
    if (x < lowerCut) 0.0 else this@cutFrom.invoke(x, arguments)
}

fun Spectrum.convolve(range: ClosedRange<Double>, function: (Double) -> Double): Spectrum = Spectrum { x, arguments ->
    DoubleField.splineIntegrator.integrate(range) { y ->
        this@convolve.invoke(y, arguments) * function(x - y)
    }.value
}

/**
 * E = A * ADC +B
 * Channel A       B
 * 0	0.01453	1.3
 * 2	0.01494	-4.332
 * 3	0.01542	-5.183
 * 4	0.01573	-2.115
 * 5	0.0152	-3.808
 * 6	0.0155	-3.015
 * 7	0.01517	-0.5429
 */
val calibration: (NumassEvent) -> Double = {
    when (it.channel) {
        0 -> 0.01453 * it.amplitude + 1.3
        2 -> 0.01494 * it.amplitude - 5.183
        3 -> 0.01542 * it.amplitude - 5.183
        4 -> 0.01573 * it.amplitude - 2.115
        5 -> 0.0152 * it.amplitude - 3.808
        6 -> 0.0155 * it.amplitude - 3.015
        7 -> 0.01517 * it.amplitude - 0.5429
        else -> error("Unrecognized channel ${it.channel}")
    } * 1000.0
}

private val neutrinoSpectrum = NumassBeta.withFixedX(0.0)

private val args: Map<Symbol, Double> = mapOf(
    NBkgSpectrum.norm to 8e5,
    NBkgSpectrum.bkg to 2.0,
    NumassBeta.mnu2 to 0.0,
    NumassBeta.e0 to 18575.0,
    NumassBeta.msterile2 to 1000.0.pow(2),
    NumassBeta.u2 to 0.0,
    NumassTransmission.thickness to 1.0,
    NumassTransmission.trap to 1.0
)

suspend fun main() {
    val repo: DataTree<NumassDirectorySet> = readRepository("D:\\Work\\Numass\\data\\2021_11\\Adiabacity_17\\")

    val gunEnergy = 17000.0

    val hv = 16900.0

    //select point number 2 (U = 16900 V) from each directory
    val points: Map<NameToken, NumassPoint?> = repo.items().mapValues {
        val directory = it.value.data?.await()
        val point = directory?.points?.find { point -> point.voltage == hv }
        point
    }

    val spectrum: Map<Double, Long> = points.values.first()!!
        .energySpectrum(NumassEventExtractor.TQDC, calibration)
        .filter { it.key > 9000.0 }
        .toSortedMap()

//    //the channel of spectrum peak position
//    val argmax = spectrum.maxByOrNull { it.value }!!.key
//
//    // convert channel to energy
//    fun Short.toEnergy(): Double = toDouble() / argmax * gunEnergy

    val norm = spectrum.values.sum().toDouble()

    val interpolated: PiecewisePolynomial<Double> = LinearInterpolator(DoubleField).interpolatePolynomials(
        spectrum.keys.map { it - gunEnergy }.asBuffer(),
        spectrum.values.map { it.toDouble() / norm }.asBuffer()
    )

    //convolve neutrino model with the gun spectrum
    val model: Spectrum = neutrinoSpectrum
        .cutFrom(14000.0)
        .convolve(0.0..18500.0, interpolated.asFunction(DoubleField, 0.0))

    val tritiumData = Numass.readPoint("D:\\Work\\Numass\\data\\2021_11\\Tritium_2\\set_11\\p0(30s)(HV1=14000)")


    Plotly.plot {
        scatter {
            name = "gun"
            x.numbers = spectrum.keys
            y.numbers = spectrum.values.map { it.toDouble() / norm }
        }

        scatter {
            name = "convoluted"
            x.buffer = 0.0..19000.0 step 100.0
            y.numbers = x.doubles.map { model(it, args) }
            val yNorm = y.doubles.maxOrNull()!!
            y.numbers = y.doubles.map { it / yNorm }
        }

        scatter {
            name = "tritium"
            val tritiumSpectrum = tritiumData.energySpectrum(NumassEventExtractor.TQDC, calibration).toSortedMap()
            x.numbers = tritiumSpectrum.keys
            y.numbers = tritiumSpectrum.values.map { it.toDouble() }
            val yNorm = y.doubles.maxOrNull()!!
            y.numbers = y.doubles.map { it / yNorm }
        }
    }.makeFile()
}