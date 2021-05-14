package ru.inr.mass.models


import space.kscience.kmath.functions.PiecewisePolynomial
import space.kscience.kmath.functions.UnivariateFunction
import space.kscience.kmath.interpolation.SplineInterpolator
import space.kscience.kmath.interpolation.interpolatePolynomials
import space.kscience.kmath.operations.DoubleField
import space.kscience.kmath.structures.DoubleBuffer
import kotlin.math.abs

public fun UnivariateFunction<Double>.cache(
    range: ClosedFloatingPointRange<Double>,
    numCachePoints: Int,
): PiecewisePolynomial<Double> {
    val length = abs(range.endInclusive - range.start)
    val grid: DoubleBuffer = DoubleBuffer(numCachePoints) { range.start + length / (numCachePoints - 1) * it }
    val vals = DoubleBuffer(grid.size) { invoke(grid[it]) }
    val interpolator = SplineInterpolator(DoubleField, ::DoubleBuffer)
    return interpolator.interpolatePolynomials(grid, vals)
}