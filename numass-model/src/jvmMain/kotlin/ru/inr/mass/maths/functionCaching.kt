package ru.inr.mass.maths

import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction
import kotlin.math.abs

public fun UnivariateFunction.cache(
    range: ClosedFloatingPointRange<Double>,
    numCachePoints: Int,
): PolynomialSplineFunction? {
    val length = abs(range.endInclusive - range.start)
    val grid: DoubleArray = DoubleArray(numCachePoints) { range.start + length / (numCachePoints - 1) * it }
    val vals = DoubleArray(grid.size) { value(grid[it]) }
    val interpolator = SplineInterpolator()
    return interpolator.interpolate(grid, vals)
}