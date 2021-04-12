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

import hep.dataforge.exceptions.NotDefinedException

/**
 * Универсальная обертка, которая объединяет именованную и обычную функцию.
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
class ParametricMultiFunctionWrapper : hep.dataforge.stat.parametric.ParametricValue, MultiFunction {
    private val multiFunc: MultiFunction?
    private val nFunc: hep.dataforge.stat.parametric.ParametricValue?
    private val names: NameList

    constructor(names: NameList, multiFunc: MultiFunction?) {
        this.names = names
        nFunc = null
        this.multiFunc = multiFunc
    }

    constructor(nFunc: hep.dataforge.stat.parametric.ParametricValue) {
        names = nFunc.getNames()
        this.nFunc = nFunc
        multiFunc = null
    }

    /**
     * {@inheritDoc}
     */
    override fun derivValue(parName: String?, pars: Values): Double {
        return if (nFunc != null) {
            nFunc.derivValue(parName, pars)
        } else {
            require(pars.getNames().contains(names.asArray())) { "Wrong parameter set." }
            require(names.contains(parName)) { "Wrong derivative parameter name." }
            multiFunc.derivValue(getNumberByName(parName),
                MathUtils.getDoubleArray(pars, getNames().asArray()))
        }
    }

    /**
     * {@inheritDoc}
     */
    @Throws(NotDefinedException::class)
    fun derivValue(n: Int, vector: DoubleArray?): Double {
        return if (multiFunc != null) {
            multiFunc.derivValue(n, vector)
        } else {
            val set = NamedVector(names.asArray(), vector)
            nFunc!!.derivValue(names.asArray().get(n), set)
        }
    }

    val dimension: Int
        get() = getNames().size()

    /**
     * {@inheritDoc}
     */
    fun getNames(): NameList {
        return names
    }

    private fun getNumberByName(name: String?): Int {
        return getNames().asList().indexOf(name)
    }

    /**
     * {@inheritDoc}
     *
     *
     * Подразумевается, что аналитически заданы все(!) производные
     */
    fun providesDeriv(n: Int): Boolean {
        return if (nFunc != null && nFunc.providesDeriv(getNames().asArray().get(n))) {
            true
        } else multiFunc != null && multiFunc.providesDeriv(n)
    }

    /**
     * {@inheritDoc}
     */
    override fun providesDeriv(name: String?): Boolean {
        return if (nFunc != null) {
            nFunc.providesDeriv(name)
        } else {
            multiFunc.providesDeriv(getNumberByName(name))
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun value(pars: Values): Double {
        return if (nFunc != null) {
            nFunc.value(pars)
        } else {
            require(pars.getNames().contains(names.asArray())) { "Wrong parameter set." }
            this.value(MathUtils.getDoubleArray(pars, getNames().asArray()))
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun value(vector: DoubleArray?): Double {
        return if (multiFunc != null) {
            multiFunc.value(vector)
        } else {
            val set = NamedVector(names.asArray(), vector)
            nFunc!!.value(set)
        }
    }
}