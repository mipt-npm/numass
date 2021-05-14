/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.models.sterile

import ru.inr.mass.models.DifferentiableKernel
import ru.inr.mass.models.Kernel
import ru.inr.mass.models.cache
import space.kscience.kmath.functions.PiecewisePolynomial
import space.kscience.kmath.functions.UnivariateFunction
import space.kscience.kmath.functions.value
import space.kscience.kmath.integration.GaussIntegrator
import space.kscience.kmath.integration.integrate
import space.kscience.kmath.misc.Symbol
import space.kscience.kmath.misc.symbol
import space.kscience.kmath.operations.DoubleField
import kotlin.math.*


public fun PiecewisePolynomial<Double>.asFunction(defaultValue: Double = 0.0): UnivariateFunction<Double> = {
    value(DoubleField, it) ?: defaultValue
}

/**
 * @author [Alexander Nozik](mailto:altavir@gmail.com)
 */
public class NumassTransmission(
    public val trapFunc: Kernel,
    private val adjustX: Boolean = false,
) : DifferentiableKernel {
    //    private val trapFunc: Kernel =         if (meta.hasValue("trapping")) {
//        val trapFuncStr = meta.getString("trapping")
//        trapFunc = if (trapFuncStr.startsWith("function::")) {
//            FunctionLibrary.buildFrom(context).buildBivariateFunction(trapFuncStr.substring(10))
//        } else {
//            BivariateFunction { Ei: Double, Ef: Double ->
//                val binding = HashMap<String, Any>()
//                binding["Ei"] = Ei
//                binding["Ef"] = Ef
//                ExpressionUtils.function(trapFuncStr, binding)
//            }
//        }
//    } else {
//        LoggerFactory.getLogger(javaClass).warn("Trapping function not defined. Using default")
//        trapFunc = FunctionLibrary.buildFrom(context).buildBivariateFunction("numass.trap.nominal")
//    }
    //private val lossCache = HashMap<Double, UnivariateFunction>()
    override fun derivativeOrNull(symbols: List<Symbol>): Kernel? = when (symbols.size) {
        0 -> this
        1 -> when (symbols.first()) {
            trap -> trapFunc
            thickness -> Kernel { eIn, eOut, arguments ->
                val thickness = arguments[thickness] ?: 0.0
                val probs = getLossProbDerivs(thickness)

                var sum = 0.0
                for (i in 1 until probs.size) {
                    sum += probs[i] * getLossValue(i, eIn, eOut)
                }
                sum
            }
            else -> null
        }
        else -> null
    }

    override fun invoke(x: Double, y: Double, arguments: Map<Symbol, Double>): Double {
        // loss part
        val thickness = arguments[thickness] ?: 0.0
        val loss = getTotalLossValue(thickness, x, y)
        //        double loss;
        //
        //        if(eIn-eOut >= 300){
        //            loss = 0;
        //        } else {
        //            UnivariateFunction lossFunction = this.lossCache.computeIfAbsent(X, theX ->
        //                    FunctionCaching.cacheUnivariateFunction(0, 300, 400, x -> calculator.getTotalLossValue(theX, eIn, eIn - x))
        //            );
        //
        //            loss = lossFunction.value(eIn - eOut);
        //        }

        //trapping part
        val trap = arguments.getOrElse(trap) { 1.0 } * trapFunc(x, y, arguments)
        return loss + trap
    }

    public companion object {
        public val trap: Symbol by symbol
        public val thickness: Symbol by symbol

        private val cache = HashMap<Int, UnivariateFunction<Double>>()

        private const val ION_POTENTIAL = 15.4//eV


        private fun getX(arguments: Map<Symbol, Double>, eIn: Double, adjustX: Boolean = false): Double {
            return if (adjustX) {
                //From our article
                arguments.getValue(thickness) * ln(eIn / ION_POTENTIAL) * eIn * ION_POTENTIAL / 1.9580741410115568e6
            } else {
                arguments.getValue(thickness)
            }
        }

        private fun p0(eIn: Double, set: Map<Symbol, Double>): Double = getLossProbability(0, getX(set, eIn))

        private fun getGunLossProbabilities(X: Double): List<Double> {
            val res = ArrayList<Double>()
            var prob: Double
            if (X > 0) {
                prob = exp(-X)
            } else {
                // если x ==0, то выживает только нулевой член, первый равен 1
                res.add(1.0)
                return res
            }
            res.add(prob)

            var n = 0
            while (prob > SCATTERING_PROBABILITY_THRESHOLD) {
                /*
                * prob(n) = prob(n-1)*X/n;
                 */
                n++
                prob *= X / n
                res.add(prob)
            }

            return res
        }

        fun getGunZeroLossProb(x: Double): Double {
            return exp(-x)
        }

        private fun getCachedSpectrum(order: Int): UnivariateFunction<Double> {
            return when {
                order <= 0 -> error("Non-positive loss cache order")
                order == 1 -> singleScatterFunction
                else -> cache.getOrPut(order) {
                    //LoggerFactory.getLogger(javaClass).debug("Scatter cache of order {} not found. Updating", order)
                    getNextLoss(getMargin(order), getCachedSpectrum(order - 1)).asFunction()
                }
            }
        }

        /**
         * Ленивое рекурсивное вычисление функции потерь через предыдущие
         *
         * @param order
         * @return
         */
        private fun getLoss(order: Int): UnivariateFunction<Double> {
            return getCachedSpectrum(order)
        }

        private fun getLossProbDerivs(x: Double): List<Double> {
            val res = ArrayList<Double>()
            val probs = getLossProbabilities(x)

            var delta = exp(-x)
            res.add((delta - probs[0]) / x)
            for (i in 1 until probs.size) {
                delta *= x / i
                res.add((delta - probs[i]) / x)
            }

            return res
        }

        /**
         * рекурсивно вычисляем все вероятности, котрорые выше порога
         *
         *
         * дисер, стр.48
         *
         * @param X
         * @return
         */
        private fun calculateLossProbabilities(x: Double): List<Double> {
            val res = ArrayList<Double>()
            var prob: Double
            if (x > 0) {
                prob = 1 / x * (1 - exp(-x))
            } else {
                // если x ==0, то выживает только нулевой член, первый равен нулю
                res.add(1.0)
                return res
            }
            res.add(prob)

            while (prob > SCATTERING_PROBABILITY_THRESHOLD) {
                /*
            * prob(n) = prob(n-1)-1/n! * X^n * exp(-X);
             */
                var delta = exp(-x)
                for (i in 1 until res.size + 1) {
                    delta *= x / i
                }
                prob -= delta / x
                res.add(prob)
            }

            return res
        }

        fun getLossProbabilities(x: Double): List<Double> = lossProbCache.getOrPut(x) { calculateLossProbabilities(x) }

        fun getLossProbability(order: Int, X: Double): Double {
            if (order == 0) {
                return if (X > 0) {
                    1 / X * (1 - exp(-X))
                } else {
                    1.0
                }
            }
            val probs = getLossProbabilities(X)
            return if (order >= probs.size) {
                0.0
            } else {
                probs[order]
            }
        }

        fun getLossValue(order: Int, Ei: Double, Ef: Double): Double {
            return when {
                Ei - Ef < 5.0 -> 0.0
                Ei - Ef >= getMargin(order) -> 0.0
                else -> getLoss(order).invoke(Ei - Ef)
            }
        }

        /**
         * функция потерь с произвольными вероятностями рассеяния
         *
         * @param probs
         * @param Ei
         * @param Ef
         * @return
         */
        fun getLossValue(probs: List<Double>, Ei: Double, Ef: Double): Double {
            var sum = 0.0
            for (i in 1 until probs.size) {
                sum += probs[i] * getLossValue(i, Ei, Ef)
            }
            return sum
        }

        /**
         * граница интегрирования
         *
         * @param order
         * @return
         */
        private fun getMargin(order: Int): Double {
            return 80 + order * 50.0
        }

        /**
         * генерирует кэшированную функцию свертки loss со спектром однократных
         * потерь
         *
         * @param loss
         * @return
         */
        @Synchronized
        private fun getNextLoss(margin: Double, loss: UnivariateFunction<Double>): PiecewisePolynomial<Double> {
            val res = { x: Double ->
                integrator.integrate(5.0..margin) { y ->
                    loss(x - y) * singleScatterFunction(y)
                }
            }

            return res.cache(0.0..margin, 200)

        }

        /**
         * Значение полной производной функции потерь с учетом всех неисчезающих
         * порядков
         *
         * @param X
         * @param eIn
         * @param eOut
         * @return
         */
        private fun getTotalLossDeriv(X: Double, eIn: Double, eOut: Double): Double {
            val probs = getLossProbDerivs(X)

            var sum = 0.0
            for (i in 1 until probs.size) {
                sum += probs[i] * getLossValue(i, eIn, eOut)
            }
            return sum
        }

        /**
         * Значение полной функции потерь с учетом всех неисчезающих порядков
         *
         * @param x
         * @param Ei
         * @param Ef
         * @return
         */
        fun getTotalLossValue(x: Double, Ei: Double, Ef: Double): Double {
            return if (x == 0.0) {
                0.0
            } else {
                val probs = getLossProbabilities(x)
                (1 until probs.size).sumByDouble { i ->
                    probs[i] * getLossValue(i, Ei, Ef)
                }
            }
        }


        /**
         * порог по вероятности, до которого вычисляются компоненты функции потерь
         */
        private const val SCATTERING_PROBABILITY_THRESHOLD = 1e-3
        private val integrator = GaussIntegrator(DoubleField)
        private val lossProbCache = HashMap<Double, List<Double>>(100)


        private val A1 = 0.204
        private val A2 = 0.0556
        private val b = 14.0
        private val pos1 = 12.6
        private val pos2 = 14.3
        private val w1 = 1.85
        private val w2 = 12.5

        public val singleScatterFunction: UnivariateFunction<Double> = { eps: Double ->
            when {
                eps <= 0 -> 0.0
                eps <= b -> {
                    val z = eps - pos1
                    A1 * exp(-2.0 * z * z / w1 / w1)
                }
                else -> {
                    val z = 4.0 * (eps - pos2) * (eps - pos2)
                    A2 / (1 + z / w2 / w2)
                }
            }
        }


        /**
         * A generic loss function for numass experiment in "Lobashev"
         * parameterization
         *
         * @param exPos
         * @param ionPos
         * @param exW
         * @param ionW
         * @param exIonRatio
         * @return
         */
        public fun getSingleScatterFunction(
            exPos: Double,
            ionPos: Double,
            exW: Double,
            ionW: Double,
            exIonRatio: Double,
        ): UnivariateFunction<Double> {
            val func: UnivariateFunction<Double> = { eps: Double ->
                if (eps <= 0) {
                    0.0
                } else {
                    val z1 = eps - exPos
                    val ex = exIonRatio * exp(-2.0 * z1 * z1 / exW / exW)

                    val z = 4.0 * (eps - ionPos) * (eps - ionPos)
                    val ion = 1 / (1 + z / ionW / ionW)

                    if (eps < exPos) {
                        ex
                    } else {
                        max(ex, ion)
                    }
                }
            }

            val cutoff = 25.0
            //caclulating lorentz integral analythically
            val tailNorm = (atan((ionPos - cutoff) * 2.0 / ionW) + 0.5 * PI) * ionW / 2.0
            val norm: Double = integrator.integrate(range = 0.0..cutoff, function = func) + tailNorm
            return { e -> func(e) / norm }
        }


        public val exPos: Symbol by symbol
        public val ionPos: Symbol by symbol
        public val exW: Symbol by symbol
        public val ionW: Symbol by symbol
        public val exIonRatio: Symbol by symbol

        public fun getSingleScatterFunction(set: Map<Symbol, Double>): UnivariateFunction<Double> {
            val exPos = set.getValue(exPos)
            val ionPos = set.getValue(ionPos)
            val exW = set.getValue(exW)
            val ionW = set.getValue(ionW)
            val exIonRatio = set.getValue(exIonRatio)

            return getSingleScatterFunction(exPos, ionPos, exW, ionW, exIonRatio)
        }

        public val trapFunction: (Double, Double) -> Double = { Ei: Double, Ef: Double ->
            val eps = Ei - Ef
            if (eps > 10) {
                1.86e-04 * exp(-eps / 25.0) + 5.5e-05
            } else {
                0.0
            }
        }

//        fun plotScatter(frame: PlotFrame, set: Values) {
//            //"X", "shift", "exPos", "ionPos", "exW", "ionW", "exIonRatio"
//
//            //        JFreeChartFrame frame = JFreeChartFrame.drawFrame("Differential scattering crosssection", null);
//            val X = set.getDouble("X")
//
//            val exPos = set.getDouble("exPos")
//
//            val ionPos = set.getDouble("ionPos")
//
//            val exW = set.getDouble("exW")
//
//            val ionW = set.getDouble("ionW")
//
//            val exIonRatio = set.getDouble("exIonRatio")
//
//            val scatterFunction = getSingleScatterFunction(exPos, ionPos, exW, ionW, exIonRatio)
//
//            if (set.names.contains("X")) {
//                val probs = LossCalculator.getGunLossProbabilities(set.getDouble("X"))
//                val single = { e: Double -> probs[1] * scatterFunction.value(e) }
//                frame.add(XYFunctionPlot.plot("Single scattering", 0.0, 100.0, 1000) { x: Double -> single(x) })
//
//                for (i in 2 until probs.size) {
//                    val scatter = { e: Double -> probs[i] * LossCalculator.getLossValue(i, e, 0.0) }
//                    frame.add(XYFunctionPlot.plot(i.toString() + " scattering", 0.0, 100.0, 1000) { x: Double -> scatter(x) })
//                }
//
//                val total = UnivariateFunction { eps ->
//                    if (probs.size == 1) {
//                        return@UnivariateFunction 0.0
//                    }
//                    var sum = probs[1] * scatterFunction.value(eps)
//                    for (i in 2 until probs.size) {
//                        sum += probs[i] * LossCalculator.getLossValue(i, eps, 0.0)
//                    }
//                    return@UnivariateFunction sum
//                }
//
//                frame.add(XYFunctionPlot.plot("Total loss", 0.0, 100.0, 1000) { x: Double -> total.value(x) })
//
//            } else {
//
//                frame.add(XYFunctionPlot.plot("Differential cross-section", 0.0, 100.0, 2000) { x: Double -> scatterFunction.value(x) })
//            }
//
//        }
    }

}
