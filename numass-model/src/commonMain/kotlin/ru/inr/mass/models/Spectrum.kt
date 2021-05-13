package ru.inr.mass.models

import space.kscience.kmath.expressions.DifferentiableExpression
import space.kscience.kmath.expressions.Expression
import space.kscience.kmath.misc.Symbol

public fun interface Spectrum : Expression<Double> {
    public val abscissa: Symbol get() = Symbol.x

    public operator fun invoke(x: Double, arguments: Map<Symbol, Double>): Double

    override fun invoke(arguments: Map<Symbol, Double>): Double =
        invoke(arguments[abscissa] ?: error("Argument $abscissa not found in arguments"), arguments)
}

public interface DifferentiableSpectrum : DifferentiableExpression<Double, Spectrum>, Spectrum

public fun interface Kernel : Expression<Double> {
    public val x: Symbol get() = Symbol.x
    public val y: Symbol get() = Symbol.y

    public operator fun invoke(x: Double, y: Double, arguments: Map<Symbol, Double>): Double

    override fun invoke(arguments: Map<Symbol, Double>): Double {
        val xValue = arguments[x] ?: error("$x value not found in arguments")
        val yValue = arguments[y] ?: error("$y value not found in arguments")
        return invoke(xValue, yValue, arguments)
    }
}

public interface DifferentiableKernel : DifferentiableExpression<Double, Kernel>, Kernel

public fun <T> Expression<T>.withDefault(default: Map<Symbol, T>): Expression<T> = Expression { args ->
    invoke(default + args)
}

public typealias UnivariateFunction = (Double) -> Double
public typealias BivariateFunction = (Double, Double) -> Double