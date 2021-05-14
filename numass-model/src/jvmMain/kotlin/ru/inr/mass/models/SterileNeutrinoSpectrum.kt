/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.models.sterile


import ru.inr.mass.models.DifferentiableKernel
import ru.inr.mass.models.DifferentiableSpectrum
import ru.inr.mass.models.FSS
import ru.inr.mass.models.Spectrum
import space.kscience.dataforge.context.Context
import space.kscience.kmath.misc.Symbol

/**
 * @param source variables:Eo offset,Ein; parameters: "mnu2", "msterile2", "U2"
 * @param transmission variables:Ein,Eout; parameters: "A"
 * @param resolution variables:Eout,U; parameters: "X", "trap"
 */
class SterileNeutrinoSpectrum(
    context: Context,
    public val source: DifferentiableKernel = NumassBeta,
    public val transmission: DifferentiableKernel,
    public val resolution: DifferentiableKernel,
    public val fss: FSS?
) : DifferentiableSpectrum {


    /**
     * auxiliary function for trans-res convolution
     */
    private val transRes: DifferentiableKernel = TransRes()

    //    private boolean useMC;
    //private val fast: Boolean = configuration.getBoolean("fast", true)


    override fun invoke(x: Double, arguments: Map<Symbol, Double>): Double {
        TODO("Not yet implemented")
    }

    override fun derivativeOrNull(symbols: List<Symbol>): Spectrum? {
        TODO("Not yet implemented")
    }

    override fun derivValue(parName: String, u: Double, set: Values): Double {
        return when (parName) {
            "U2", "msterile2", "mnu2", "E0" -> integrate(u, source.derivative(parName), transRes, set)
            "X", "trap" -> integrate(u, source, transRes.derivative(parName), set)
            else -> throw NotDefinedException()
        }
    }

    override fun value(u: Double, set: Values): Double {
        return integrate(u, source, transRes, set)
    }

    override fun providesDeriv(name: String): Boolean {
        return source.providesDeriv(name) && transmission.providesDeriv(name) && resolution.providesDeriv(name)
    }


    /**
     * Direct Gauss-Legendre integration
     *
     * @param u
     * @param sourceFunction
     * @param transResFunction
     * @param set
     * @return
     */
    private fun integrate(
        u: Double,
        sourceFunction: DifferentiableKernel,
        transResFunction: DifferentiableKernel,
        set: Map<Symbol, Double>,
    ): Double {

        val eMax = set.getDouble("E0") + 5.0

        if (u >= eMax) {
            return 0.0
        }

        val integrator: UnivariateIntegrator<*> = if (fast) {
            when {
                eMax - u < 300 -> NumassIntegrator.getFastInterator()
                eMax - u > 2000 -> NumassIntegrator.getHighDensityIntegrator()
                else -> NumassIntegrator.getDefaultIntegrator()
            }

        } else {
            NumassIntegrator.getHighDensityIntegrator()
        }

        return integrator.integrate(u, eMax) { eIn ->
            sumByFSS(eIn, sourceFunction, set) * transResFunction.value(eIn,
                u,
                set)
        }
    }

    private fun sumByFSS(eIn: Double, sourceFunction: ParametricBiFunction, set: Values): Double {
        return if (fss == null) {
            sourceFunction.value(0.0, eIn, set)
        } else {
            (0 until fss.size()).sumByDouble { fss.getP(it) * sourceFunction.value(fss.getE(it), eIn, set) }
        }
    }


    private inner class TransRes : DifferentiableKernel {

        override fun providesDeriv(name: String): Boolean {
            return true
        }

        override fun derivValue(parName: String, eIn: Double, u: Double, set: Values): Double {
            return when (parName) {
                "X" -> throw NotDefinedException()//TODO implement p0 derivative
                "trap" -> lossRes(transmission.derivative(parName), eIn, u, set)
                else -> super.derivValue(parName, eIn, u, set)
            }
        }

        override fun value(eIn: Double, u: Double, set: Values): Double {

            val p0 = LossCalculator.p0(set, eIn)
            return p0 * resolution.value(eIn, u, set) + lossRes(transmission, eIn, u, set)
        }

        private fun lossRes(transFunc: ParametricBiFunction, eIn: Double, u: Double, set: Values): Double {
            val integrand = { eOut: Double -> transFunc.value(eIn, eOut, set) * resolution.value(eOut, u, set) }

            val border = u + 30
            val firstPart = NumassIntegrator.getFastInterator().integrate(u, Math.min(eIn, border), integrand)
            val secondPart: Double = if (eIn > border) {
                if (fast) {
                    NumassIntegrator.getDefaultIntegrator().integrate(border, eIn, integrand)
                } else {
                    NumassIntegrator.getHighDensityIntegrator().integrate(border, eIn, integrand)
                }
            } else {
                0.0
            }
            return firstPart + secondPart
        }

    }

    companion object {

        private val list = arrayOf("X", "trap", "E0", "mnu2", "msterile2", "U2")
    }

}
