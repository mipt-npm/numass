/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inr.mass.models

import space.kscience.kmath.expressions.Symbol
import space.kscience.kmath.expressions.symbol
import space.kscience.kmath.functions.Function1D
import space.kscience.kmath.functions.PiecewisePolynomial
import space.kscience.kmath.functions.asFunction
import space.kscience.kmath.integration.*
import space.kscience.kmath.operations.DoubleField
import kotlin.jvm.Synchronized
import kotlin.math.*


/**
 * @author [Alexander Nozik](mailto:altavir@gmail.com)
 */
public class NumassTransmission(
    public val trapFunc: Kernel = defaultTrapping,
//    private val adjustX: Boolean = false,
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

    override fun invoke(ei: Double, ef: Double, arguments: Map<Symbol, Double>): Double {
        // loss part
        val thickness = arguments[thickness] ?: 0.0
        val loss = getTotalLossValue(thickness, ei, ef)
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
        val trap = (arguments[trap] ?: 1.0) * trapFunc(ei, ef, arguments)
        return loss + trap
    }

    public companion object {
        public val trap: Symbol by symbol
        public val thickness: Symbol by symbol

        private val cache = HashMap<Int, Function1D<Double>>()

        private const val ION_POTENTIAL = 15.4//eV


        private fun getX(arguments: Map<Symbol, Double>, eIn: Double, adjustX: Boolean = false): Double {
            val thickness = arguments[thickness] ?: 0.0
            return if (adjustX) {
                //From our article
                thickness * ln(eIn / ION_POTENTIAL) * eIn * ION_POTENTIAL / 1.9580741410115568e6
            } else {
                thickness
            }
        }

        internal fun p0(eIn: Double, set: Map<Symbol, Double>): Double = getLossProbability(0, getX(set, eIn))

        private fun getGunLossProbabilities(X: Double): List<Double> {
            val res = ArrayList<Double>()
            var prob: Double
            if (X > 0) {
                prob = exp(-X)
            } else {
                // ???????? x ==0, ???? ???????????????? ???????????? ?????????????? ????????, ???????????? ?????????? 1
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

        private fun getGunZeroLossProb(x: Double): Double {
            return exp(-x)
        }

        private fun getCachedSpectrum(order: Int): Function1D<Double> {
            return when {
                order <= 0 -> error("Non-positive loss cache order")
                order == 1 -> singleScatterFunction
                else -> cache.getOrPut(order) {
                    //LoggerFactory.getLogger(javaClass).debug("Scatter cache of order {} not found. Updating", order)
                    getNextLoss(getMargin(order), getCachedSpectrum(order - 1)).asFunction(DoubleField, 0.0)
                }
            }
        }

        /**
         * ?????????????? ?????????????????????? ???????????????????? ?????????????? ???????????? ?????????? ????????????????????
         *
         * @param order
         * @return
         */
        private fun getLoss(order: Int): Function1D<Double> = getCachedSpectrum(order)

        private fun getLossProbDerivs(x: Double): List<Double> {
            val res = ArrayList<Double>()
            val probs = lossProbabilities(x)

            var delta = exp(-x)
            res.add((delta - probs[0]) / x)
            for (i in 1 until probs.size) {
                delta *= x / i
                res.add((delta - probs[i]) / x)
            }

            return res
        }

        /**
         * ???????????????????? ?????????????????? ?????? ??????????????????????, ???????????????? ???????? ????????????
         *
         *
         * ??????????, ??????.48
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
                // ???????? x ==0, ???? ???????????????? ???????????? ?????????????? ????????, ???????????? ?????????? ????????
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

        public fun lossProbabilities(x: Double): List<Double> =
            lossProbCache.getOrPut(x) { calculateLossProbabilities(x) }

        private fun getLossProbability(order: Int, X: Double): Double {
            if (order == 0) {
                return if (X > 0) {
                    1 / X * (1 - exp(-X))
                } else {
                    1.0
                }
            }
            val probs = lossProbabilities(X)
            return if (order >= probs.size) {
                0.0
            } else {
                probs[order]
            }
        }

        private fun getLossValue(order: Int, ei: Double, ef: Double): Double {
            return when {
                ei - ef < 5.0 -> 0.0
                ei - ef >= getMargin(order) -> 0.0
                else -> getLoss(order).invoke(ei - ef)
            }
        }

        /**
         * ?????????????? ???????????? ?? ?????????????????????????? ?????????????????????????? ??????????????????
         *
         * @param probs
         * @param Ei
         * @param Ef
         * @return
         */
        private fun getLossValue(probs: List<Double>, Ei: Double, Ef: Double): Double {
            var sum = 0.0
            for (i in 1 until probs.size) {
                sum += probs[i] * getLossValue(i, Ei, Ef)
            }
            return sum
        }

        /**
         * ?????????????? ????????????????????????????
         *
         * @param order
         * @return
         */
        private fun getMargin(order: Int): Double {
            return 50 + order * 50.0
        }

        /**
         * ???????????????????? ???????????????????????? ?????????????? ?????????????? loss ???? ???????????????? ??????????????????????
         * ????????????
         *
         * @param loss
         * @return
         */
        @Synchronized
        private fun getNextLoss(margin: Double, loss: Function1D<Double>): PiecewisePolynomial<Double> {
            val res = { x: Double ->
                DoubleField.simpsonIntegrator.integrate(5.0..margin, IntegrandMaxCalls(200)) { y ->
                    loss(x - y) * singleScatterFunction(y)
                }.value
            }

            return res.cache(0.0..margin, 100)
        }

        /**
         * ???????????????? ???????????? ?????????????????????? ?????????????? ???????????? ?? ???????????? ???????? ????????????????????????
         * ????????????????
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
         * ???????????????? ???????????? ?????????????? ???????????? ?? ???????????? ???????? ???????????????????????? ????????????????
         *
         * @param thickness
         * @param Ei
         * @param Ef
         * @return
         */
        private fun getTotalLossValue(thickness: Double, Ei: Double, Ef: Double): Double {
            return if (thickness == 0.0) {
                0.0
            } else {
                val probs = lossProbabilities(thickness)
                (1 until probs.size).sumOf { i ->
                    probs[i] * getLossValue(i, Ei, Ef)
                }
            }
        }


        /**
         * ?????????? ???? ??????????????????????, ???? ???????????????? ?????????????????????? ???????????????????? ?????????????? ????????????
         */
        private const val SCATTERING_PROBABILITY_THRESHOLD = 1e-3
        private val lossProbCache = HashMap<Double, List<Double>>(100)


        private val A1 = 0.204
        private val A2 = 0.0556
        private val b = 14.0
        private val pos1 = 12.6
        private val pos2 = 14.3
        private val w1 = 1.85
        private val w2 = 12.5

        public val singleScatterFunction: Function1D<Double> = { eps: Double ->
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


//        /**
//         * A generic loss function for numass experiment in "Lobashev"
//         * parameterization
//         *
//         * @param exPos
//         * @param ionPos
//         * @param exW
//         * @param ionW
//         * @param exIonRatio
//         * @return
//         */
//        public fun getSingleScatterFunction(
//            exPos: Double,
//            ionPos: Double,
//            exW: Double,
//            ionW: Double,
//            exIonRatio: Double,
//        ): UnivariateFunction<Double> {
//            val func: UnivariateFunction<Double> = { eps: Double ->
//                if (eps <= 0) {
//                    0.0
//                } else {
//                    val z1 = eps - exPos
//                    val ex = exIonRatio * exp(-2.0 * z1 * z1 / exW / exW)
//
//                    val z = 4.0 * (eps - ionPos) * (eps - ionPos)
//                    val ion = 1 / (1 + z / ionW / ionW)
//
//                    if (eps < exPos) {
//                        ex
//                    } else {
//                        max(ex, ion)
//                    }
//                }
//            }
//
//            val cutoff = 25.0
//            //calculating lorentz integral analytically
//            val tailNorm = (atan((ionPos - cutoff) * 2.0 / ionW) + 0.5 * PI) * ionW / 2.0
//            val norm: Double = integrator.integrate(range = 0.0..cutoff, function = func).value + tailNorm
//            return { e -> func(e) / norm }
//        }


        public val exPos: Symbol by symbol
        public val ionPos: Symbol by symbol
        public val exW: Symbol by symbol
        public val ionW: Symbol by symbol
        public val exIonRatio: Symbol by symbol

//        public fun getSingleScatterFunction(set: Map<Symbol, Double>): UnivariateFunction<Double> {
//            val exPos = set.getValue(exPos)
//            val ionPos = set.getValue(ionPos)
//            val exW = set.getValue(exW)
//            val ionW = set.getValue(ionW)
//            val exIonRatio = set.getValue(exIonRatio)
//
//            return getSingleScatterFunction(exPos, ionPos, exW, ionW, exIonRatio)
//        }

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

        internal val defaultTrapping: Kernel = Kernel { ei, ef, _ ->
            1.2e-4 - 4.5e-9 * ei
        }
    }

}
