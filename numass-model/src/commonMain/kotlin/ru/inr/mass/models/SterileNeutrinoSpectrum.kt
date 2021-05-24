/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inr.mass.models


import ru.inr.mass.models.NumassBeta.e0
import ru.inr.mass.models.NumassBeta.mnu2
import ru.inr.mass.models.NumassBeta.msterile2
import ru.inr.mass.models.NumassBeta.u2
import ru.inr.mass.models.NumassTransmission.Companion.thickness
import ru.inr.mass.models.NumassTransmission.Companion.trap
import space.kscience.kmath.expressions.Symbol
import space.kscience.kmath.expressions.derivative
import space.kscience.kmath.integration.UnivariateIntegrandRanges
import space.kscience.kmath.integration.gaussIntegrator
import space.kscience.kmath.integration.integrate
import space.kscience.kmath.integration.value
import space.kscience.kmath.operations.DoubleField
import space.kscience.kmath.real.step
import space.kscience.kmath.structures.toDoubleArray


/**
 * @param source variables:Eo offset,Ein; parameters: "mnu2", "msterile2", "U2"
 * @param transmission variables:Ein,Eout; parameters: "A"
 * @param resolution variables:Eout,U; parameters: "X", "trap"
 */
public class SterileNeutrinoSpectrum(
    public val source: DifferentiableKernel = NumassBeta,
    public val transmission: DifferentiableKernel = NumassTransmission(),
    public val resolution: DifferentiableKernel = NumassResolution(),
    public val fss: FSS? = null,
) : DifferentiableSpectrum {

    /**
     * auxiliary function for trans-res convolution
     */
    private val transRes: DifferentiableKernel = TransRes()

    //    private boolean useMC;
    //private val fast: Boolean = configuration.getBoolean("fast", true)


    override fun invoke(u: Double, arguments: Map<Symbol, Double>): Double {
        return convolute(u, source, transRes, arguments)
    }

    override fun derivativeOrNull(symbols: List<Symbol>): Spectrum? {
        if (symbols.isEmpty()) return this
        return when (symbols.singleOrNull() ?: TODO("First derivatives only")) {
            u2, msterile2, mnu2, e0 -> Spectrum { u, arguments ->
                convolute(u, source.derivative(symbols), transRes, arguments)
            }
            thickness, trap -> Spectrum { u, arguments ->
                convolute(u, source, transRes.derivative(symbols), arguments)
            }
            else -> null
        }
    }

    /**
     * Direct Gauss-Legendre integration
     *
     * @param u
     * @param sourceFunction
     * @param transResFunction
     * @param arguments
     * @return
     */
    private fun convolute(
        u: Double,
        sourceFunction: Kernel,
        transResFunction: Kernel,
        arguments: Map<Symbol, Double>,
    ): Double {

        val eMax = arguments.getValue(e0) + 5.0

        if (u >= eMax) {
            return 0.0
        }

//        val integrator: UnivariateIntegrator<*> = if (fast) {
//            when {
//                eMax - u < 300 -> getFastInterator()
//                eMax - u > 2000 -> getHighDensityIntegrator()
//                else -> getDefaultIntegrator()
//            }
//        } else {
//            getHighDensityIntegrator()
//        }

        return DoubleField.gaussIntegrator.integrate(u..eMax, generateRanges(
            u..eMax,
            u + 2.0,
            u + 7.0,
            u + 15.0,
            u + 30.0,
            *((u + 50)..(u + 6000) step 25.0).toDoubleArray()
        )) { eIn ->
            sumByFSS(eIn, sourceFunction, arguments) * transResFunction(eIn, u, arguments)
        }.value
    }

    private fun sumByFSS(eIn: Double, sourceFunction: Kernel, arguments: Map<Symbol, Double>): Double {
        return if (fss == null) {
            sourceFunction(0.0, eIn, arguments)
        } else {
            (0 until fss.size).sumOf { fss.ps[it] * sourceFunction(fss.es[it], eIn, arguments) }
        }
    }


    private inner class TransRes : DifferentiableKernel {

        override fun invoke(eIn: Double, u: Double, arguments: Map<Symbol, Double>): Double {
            val p0 = NumassTransmission.p0(eIn, arguments)
            return p0 * resolution(eIn, u, arguments) + lossRes(transmission, eIn, u, arguments)
        }

        override fun derivativeOrNull(symbols: List<Symbol>): Kernel? {
            if (symbols.isEmpty()) return this
            return when (symbols.singleOrNull() ?: TODO("First derivatives only")) {
                thickness -> null//TODO implement p0 derivative
                trap -> Kernel { eIn, u, arguments -> lossRes(transmission.derivative(symbols), eIn, u, arguments) }
                else -> null
            }
        }

        private fun lossRes(
            transFunc: Kernel,
            eIn: Double,
            u: Double,
            arguments: Map<Symbol, Double>,
        ): Double = DoubleField.gaussIntegrator.integrate(u..eIn, generateRanges(
            u..eIn,
            u + 2.0,
            u + 7.0,
            u + 15.0,
            u + 30.0,
            *((u + 50)..(u + 6000) step 30.0).toDoubleArray()
        )) { eOut: Double ->
            transFunc(eIn, eOut, arguments) * resolution(eOut, u, arguments)
        }.value
    }

    public companion object
}


internal fun generateRanges(
    range: ClosedFloatingPointRange<Double>,
    vararg borders: Double,
    points: Int = 5,
): UnivariateIntegrandRanges {
    if (borders.isEmpty() || borders.first() > range.endInclusive) return UnivariateIntegrandRanges(range to points)
    val ranges = listOf(
        range.start,
        *borders.filter { it in range }.sorted().toTypedArray(),
        range.endInclusive
    ).zipWithNext { l, r ->
        l..r to points
    }
    return UnivariateIntegrandRanges(ranges)
}