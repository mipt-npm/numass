/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.models.sterile

import ru.inr.mass.models.DifferentiableKernel
import ru.inr.mass.models.Kernel
import space.kscience.kmath.misc.StringSymbol
import space.kscience.kmath.misc.Symbol
import space.kscience.kmath.misc.symbol
import kotlin.math.*

/**
 * A bi-function for beta-spectrum calculation taking energy and final state as
 * input.
 *
 * dissertation p.33
 */
public object NumassBeta : DifferentiableKernel {

    /**
     * Beta spectrum derivative
     *
     * @param n parameter number
     * @param E0
     * @param mnu2
     * @param E
     * @return
     * @throws NotDefinedException
     */
    private fun derivRoot(n: Int, E0: Double, mnu2: Double, E: Double): Double {
        val D = E0 - E//E0-E
        if (D == 0.0) {
            return 0.0
        }

        return if (mnu2 >= 0) {
            if (E >= E0 - sqrt(mnu2)) {
                0.0
            } else {
                val bare = sqrt(D * D - mnu2)
                when (n) {
                    0 -> factor(E) * (2.0 * D * D - mnu2) / bare
                    1 -> -factor(E) * 0.5 * D / bare
                    else -> 0.0
                }
            }
        } else {
            val mu = sqrt(-0.66 * mnu2)
            if (E >= E0 + mu) {
                0.0
            } else {
                val root = sqrt(max(D * D - mnu2, 0.0))
                val exp = exp(-1 - D / mu)
                when (n) {
                    0 -> factor(E) * (D * (D + mu * exp) / root + root * (1 - exp))
                    1 -> factor(E) * (-(D + mu * exp) / root * 0.5 - root * exp * (1 + D / mu) / 3.0 / mu)
                    else -> 0.0
                }
            }
        }
    }

    /**
     * Derivative of spectrum with sterile neutrinos
     *
     * @param name
     * @param E
     * @param E0
     * @param pars
     * @return
     * @throws NotDefinedException
     */
    private fun derivRootsterile(symbol: Symbol, E: Double, E0: Double, pars: Map<Symbol, Double>): Double {
        val mnu2Value = pars.getValue(mnu2)
        val msterile2Value = pars.getValue(msterile2)
        val u2Value = pars.getValue(u2)

        return when (symbol) {
            e0 -> {
                if (u2Value == 0.0) {
                    derivRoot(0, E0, mnu2Value, E)
                } else {
                    u2Value * derivRoot(0, E0, msterile2Value, E) + (1 - u2Value) * derivRoot(0, E0, mnu2Value, E)
                }
            }
            mnu2 -> (1 - u2Value) * derivRoot(1, E0, mnu2Value, E)
            msterile2 -> {
                if (u2Value == 0.0) {
                    0.0
                } else {
                    u2Value * derivRoot(1, E0, msterile2Value, E)
                }
            }
            u2 -> root(E0, msterile2Value, E) - root(E0, mnu2Value, E)
            else -> 0.0
        }

    }

    /**
     * The part independent of neutrino mass. Includes global normalization
     * constant, momentum and Fermi correction
     *
     * @param E
     * @return
     */
    private fun factor(E: Double): Double {
        val me = 0.511006E6
        val eTot = E + me
        val pe = sqrt(E * (E + 2.0 * me))
        val ve = pe / eTot
        val yfactor = 2.0 * 2.0 * 1.0 / 137.039 * PI
        val y = yfactor / ve
        val fn = y / abs(1.0 - exp(-y))
        val fermi = fn * (1.002037 - 0.001427 * ve)
        val res: Double = fermi * pe * eTot
        return K * res
    }

    /**
     * Bare beta spectrum with Mainz negative mass correction
     *
     * @param E0
     * @param mnu2
     * @param E
     * @return
     */
    private fun root(E0: Double, mnu2: Double, E: Double): Double {
        //bare beta-spectrum
        val delta = E0 - E
        val bare = factor(E) * delta * sqrt(max(delta * delta - mnu2, 0.0))
        return when {
            mnu2 >= 0 -> max(bare, 0.0)
            delta == 0.0 -> 0.0
            delta + 0.812 * sqrt(-mnu2) <= 0 -> 0.0              //sqrt(0.66)
            else -> {
                val aux = sqrt(-mnu2 * 0.66) / delta
                max(bare * (1 + aux * exp(-1 - 1 / aux)), 0.0)
            }
        }
    }

    /**
     * beta-spectrum with sterile neutrinos
     *
     * @param E
     * @param E0
     * @param pars
     * @return
     */
    private fun rootsterile(E: Double, E0: Double, pars: Map<Symbol, Double>): Double {
        val mnu2 = pars.getValue(mnu2)
        val mst2 = pars.getValue(msterile2)
        val u2 = pars.getValue(u2)

        return if (u2 == 0.0) {
            root(E0, mnu2, E)
        } else {
            u2 * root(E0, mst2, E) + (1 - u2) * root(E0, mnu2, E)
        }
// P(rootsterile)+ (1-P)root
    }

    override val x: Symbol = StringSymbol("fs")
    override val y: Symbol = StringSymbol("eIn")

    override fun invoke(fs: Double, eIn: Double, arguments: Map<Symbol, Double>): Double {
        val e0 = arguments.getValue(e0)
        return rootsterile(eIn, e0 - fs, arguments)
    }

    override fun derivativeOrNull(symbols: List<Symbol>): Kernel? = when (symbols.size) {
        0 -> this
        1 -> Kernel { fs, eIn, arguments ->
            val e0 = arguments.getValue(e0)
            derivRootsterile(symbols.first(), eIn, e0 - fs, arguments)
        }
        else -> null
    }


    private const val K: Double = 1E-23
    public val e0: Symbol by symbol
    public val mnu2: Symbol by symbol
    public val msterile2: Symbol by symbol
    public val u2: Symbol by symbol

}
