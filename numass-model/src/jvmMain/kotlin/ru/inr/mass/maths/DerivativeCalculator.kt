/* 
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.inr.mass.maths

import hep.dataforge.stat.fit.Param
import hep.dataforge.stat.parametric.ParametricUtils
import hep.dataforge.stat.parametric.ParametricValue

/**
 *
 * DerivativeCalculator class.
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
object DerivativeCalculator {
    private const val numPoints = 3

    /**
     * Calculates finite differences derivative via 3 points differentiator.
     *
     * @param function a [hep.dataforge.stat.parametric.ParametricValue] object.
     * @param point a [hep.dataforge.stat.fit.ParamSet] object.
     * @param parName a [String] object.
     * @return a double.
     */
    fun calculateDerivative(function: ParametricValue?, point: ParamSet, parName: String?): Double {
        val projection: UnivariateFunction = ParametricUtils.getNamedProjection(function, parName, point)
        val par: Param = point.getByName(parName)
        val diff =
            FiniteDifferencesDifferentiator(numPoints, par.getErr() / 2.0, par.getLowerBound(), par.getUpperBound())
        val derivative: UnivariateDifferentiableFunction = diff.differentiate(projection)
        val x = DerivativeStructure(1, 1, 0, point.getDouble(parName))
        val y: DerivativeStructure = derivative.value(x)
        return y.getPartialDerivative(1)
    }

    /**
     * Calculates finite differences derivative via 3 points differentiator.
     *
     * @param function a [org.apache.commons.math3.analysis.UnivariateFunction] object.
     * @param point a double.
     * @param step a double.
     * @return a double.
     */
    fun calculateDerivative(function: UnivariateFunction?, point: Double, step: Double): Double {
        val diff = FiniteDifferencesDifferentiator(numPoints, step)
        val derivative: UnivariateDifferentiableFunction = diff.differentiate(function)
        val x = DerivativeStructure(1, 1, 0, point)
        val y: DerivativeStructure = derivative.value(x)
        return y.getPartialDerivative(1)
    }

    /**
     *
     * providesValidDerivative.
     *
     * @param function a [hep.dataforge.stat.parametric.ParametricValue] object.
     * @param point a [hep.dataforge.stat.fit.ParamSet] object.
     * @param tolerance a double.
     * @param parName a [String] object.
     * @return a boolean.
     */
    fun providesValidDerivative(
        function: ParametricValue,
        point: ParamSet,
        tolerance: Double,
        parName: String?
    ): Boolean {
        if (!function.providesDeriv(parName)) {
            return false
        }
        val calculatedDeriv = calculateDerivative(function, point, parName)
        val providedDeriv: Double = function.derivValue(parName, point)
        return safeRelativeDifference(calculatedDeriv, providedDeriv) <= tolerance
    }

    /**
     * Returns safe from (no devision by zero) relative difference between two
     * input values
     *
     * @param val1
     * @param val2
     * @return
     */
    private fun safeRelativeDifference(val1: Double, val2: Double): Double {
        if (Precision.equals(val1, val2, Precision.EPSILON)) {
            return 0
        }
        val average: Double = Math.abs(val1 + val2) / 2
        return if (average > Precision.EPSILON) {
            Math.abs(val1 - val2) / average
        } else {
            Double.POSITIVE_INFINITY
        }
    }
}