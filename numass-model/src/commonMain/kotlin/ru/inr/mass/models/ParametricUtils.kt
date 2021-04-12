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
package hep.dataforge.stat.parametric

import hep.dataforge.exceptions.NameNotFoundException

/**
 *
 * ParametricUtils class.
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
object ParametricUtils {
    /**
     * Создает одномерное сечение многомерной именованной функции
     *
     * @param nFunc - исходная именованная функция
     * @param parName - имя параметра, по которому делается сечение
     * @param pars - Точка, вкоторой вычеслено сечение
     * @return a [UniFunction] object.
     */
    fun getNamedProjection(nFunc: ParametricValue, parName: String?, pars: Values?): UniFunction {
        return object : UniFunction() {
            var curPars: NamedVector = NamedVector(pars)
            @Throws(NotDefinedException::class)
            fun derivValue(x: Double): Double {
                curPars.setValue(parName, x)
                return nFunc.derivValue(parName, curPars)
            }

            fun providesDeriv(): Boolean {
                return nFunc.providesDeriv(parName)
            }

            fun value(x: Double): Double {
                curPars.setValue(parName, x)
                return nFunc.value(curPars)
            }
        }
    }

    /**
     *
     * getNamedProjectionDerivative.
     *
     * @param nFunc a [hep.dataforge.stat.parametric.ParametricValue] object.
     * @param parName a [String] object.
     * @param derivativeName a [String] object.
     * @param pars
     * @return a [org.apache.commons.math3.analysis.UnivariateFunction] object.
     */
    fun getNamedProjectionDerivative(
        nFunc: ParametricValue,
        parName: String?, derivativeName: String?, pars: Values?
    ): UnivariateFunction {
        return object : UnivariateFunction() {
            var curPars: NamedVector = NamedVector(pars)
            fun value(x: Double): Double {
                curPars.setValue(parName, x)
                return nFunc.derivValue(derivativeName, curPars)
            }
        }
    }

    /**
     * Функция, которая запоминает исходную точку, и при нехватке параметров
     * берет значения оттуда.
     *
     * @param func a [hep.dataforge.stat.parametric.ParametricValue] object.
     * @param initPars
     * @param freePars - Описывает, каким параметрам можно будет изменяться.
     * Если null, то разрешено изменение всех параметров.
     * @return a [hep.dataforge.stat.parametric.ParametricValue] object.
     */
    fun getNamedSubFunction(func: ParametricValue, initPars: Values, vararg freePars: String?): ParametricValue {
        require(initPars.getNames().contains(func.namesAsArray())) { "InitPars does not cover all of func parameters." }
        val names: NameList
        if (freePars.size > 0) {
            names = NameList(freePars)
        } else {
            names = initPars.getNames()
        }
        return object : AbstractParametricValue(names) {
            private val allPars: NamedVector = NamedVector(initPars)
            private val f: ParametricValue = func
            override fun derivValue(derivParName: String?, pars: Values): Double {
                if (!pars.getNames().contains(this.namesAsArray())) {
                    throw NameNotFoundException()
                }
                for (name in this.getNames()) {
                    allPars.setValue(name, pars.getDouble(name))
                }
                return f.derivValue(derivParName, allPars)
            }

            override fun providesDeriv(name: String?): Boolean {
                return f.providesDeriv(name) && this.getNames().contains(name)
            }

            override fun value(pars: Values): Double {
                if (!pars.getNames().contains(this.namesAsArray())) {
                    throw NameNotFoundException()
                }
                for (name in this.getNames()) {
                    allPars.setValue(name, pars.getDouble(name))
                }
                return f.value(allPars)
            }
        }
    }

    fun getSpectrumDerivativeFunction(name: String?, s: ParametricFunction, pars: Values?): UnivariateFunction {
        return UnivariateFunction { x: Double -> s.derivValue(name, x, pars) }
    }

    fun getSpectrumFunction(s: ParametricFunction, pars: Values?): UnivariateFunction {
        return UnivariateFunction { x: Double -> s.value(x, pars) }
    }

    fun getSpectrumPointFunction(s: ParametricFunction, x: Double): ParametricValue {
        return object : AbstractParametricValue(s) {
            @Throws(NotDefinedException::class, NamingException::class)
            override fun derivValue(derivParName: String?, pars: Values?): Double {
                return s.derivValue(derivParName, x, pars)
            }

            override fun providesDeriv(name: String?): Boolean {
                return s.providesDeriv(name)
            }

            @Throws(NamingException::class)
            override fun value(pars: Values?): Double {
                return s.value(x, pars)
            }
        }
    }
}