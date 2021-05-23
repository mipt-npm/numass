/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inr.mass.models

import space.kscience.kmath.expressions.Symbol
import space.kscience.kmath.expressions.symbol
import kotlin.math.sqrt

/**
 * @author [Alexander Nozik](mailto:altavir@gmail.com)
 */
public class NumassResolution(
    public val resA: Double = 8.3e-5,
    public val resB: Double = 0.0,
    public val tailFunction: (Double, Double) -> Double = { _, _ -> 1.0 },
) : DifferentiableKernel {

//    private val tailFunction: Kernel = when {
//        meta["tail"] != null -> {
//            val tailFunctionStr = meta["tail"].string
//            if (tailFunctionStr.startsWith("function::")) {
//                FunctionLibrary.buildFrom(context).buildBivariateFunction(tailFunctionStr.substring(10))
//            } else {
//                BivariateFunction { E, U ->
//                    val binding = HashMap<String, Any>()
//                    binding["E"] = E
//                    binding["U"] = U
//                    binding["D"] = E - U
//                    ExpressionUtils.function(tailFunctionStr, binding)
//                }
//            }
//        }
//        meta.hasValue("tailAlpha") -> {
//            //add polynomial function here
//            val alpha = meta.getDouble("tailAlpha")
//            val beta = meta.getDouble("tailBeta", 0.0)
//            BivariateFunction { E: Double, U: Double -> 1 - (E - U) * (alpha + E / 1000.0 * beta) / 1000.0 }
//
//        }
//        else -> ResolutionFunction.getConstantTail()
//    }

    private fun getValueFast(E: Double, U: Double): Double {
        val delta = resA * E
        return when {
            E - U < 0 -> 0.0
            E - U > delta -> tailFunction(E, U)
            else -> (E - U) / delta
        }
    }

    override fun derivativeOrNull(symbols: List<Symbol>): Kernel = Kernel { _, _, _ -> 0.0 }

    override val x: Symbol get() = e
    override val y: Symbol get() = u

    override fun invoke(E: Double, U: Double, arguments: Map<Symbol, Double>): Double {
        if (resB <= 0) {
            return this.getValueFast(E, U)
        }
        val delta = resA * E
        return when {
            E - U < 0 -> 0.0
            E - U > delta -> tailFunction(E, U)
            else -> (1 - sqrt(1 - (E - U) / E * resB)) / (1 - sqrt(1 - resA * resB))
        }
    }


    public companion object {
        public val e: Symbol by symbol
        public val u: Symbol by symbol

    }

}
