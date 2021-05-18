/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.models.sterile


import inr.numass.models.sterile.NumassBeta.e0
import inr.numass.models.sterile.NumassBeta.mnu2
import inr.numass.models.sterile.NumassBeta.msterile2
import inr.numass.models.sterile.NumassBeta.u2
import inr.numass.models.sterile.NumassTransmission.Companion.thickness
import inr.numass.models.sterile.NumassTransmission.Companion.trap
import ru.inr.mass.models.*
import space.kscience.kmath.expressions.derivative
import space.kscience.kmath.integration.integrate
import space.kscience.kmath.integration.integrator
import space.kscience.kmath.integration.value
import space.kscience.kmath.misc.Symbol
import space.kscience.kmath.operations.DoubleField
import kotlin.math.min

/**
 * @param source variables:Eo offset,Ein; parameters: "mnu2", "msterile2", "U2"
 * @param transmission variables:Ein,Eout; parameters: "A"
 * @param resolution variables:Eout,U; parameters: "X", "trap"
 */
public class SterileNeutrinoSpectrum(
    public val source: DifferentiableKernel = NumassBeta,
    public val transmission: DifferentiableKernel = NumassTransmission(),
    public val resolution: DifferentiableKernel = NumassResolution(),
    public val fss: FSS? = FSS.default,
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

        return DoubleField.integrator.integrate(u..eMax) { eIn ->
            sumByFSS(eIn, sourceFunction, arguments) * transResFunction(eIn, u, arguments)
        }.value ?: error("Integration failed")
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

        private fun lossRes(transFunc: Kernel, eIn: Double, u: Double, arguments: Map<Symbol, Double>): Double {
            val integrand = { eOut: Double -> transFunc(eIn, eOut, arguments) * resolution(eOut, u, arguments) }

            val border = u + 30
            val firstPart = DoubleField.integrator.integrate(u..min(eIn, border), function = integrand).value
            val secondPart: Double = if (eIn > border) {
                DoubleField.integrator.integrate(border..eIn, function = integrand).value
            } else {
                0.0
            }
            return firstPart + secondPart
        }

    }

    public companion object
}
