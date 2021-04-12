/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.stat.parametric

import hep.dataforge.exceptions.NotDefinedException

/**
 *
 * @author Alexander Nozik
 */
interface ParametricBiFunction : NameSetContainer {
    fun derivValue(parName: String?, x: Double, y: Double, set: Values?): Double
    fun value(x: Double, y: Double, set: Values?): Double
    fun providesDeriv(name: String?): Boolean
    fun derivative(parName: String?): ParametricBiFunction? {
        return if (providesDeriv(parName)) {
            object : ParametricBiFunction {
                override fun derivValue(parName: String?, x: Double, y: Double, set: Values?): Double {
                    throw NotDefinedException()
                }

                override fun value(x: Double, y: Double, set: Values?): Double {
                    return this@ParametricBiFunction.derivValue(parName, x, y, set)
                }

                override fun providesDeriv(name: String?): Boolean {
                    return !names.contains(name)
                }

                val names: NameList
                    get() = this@ParametricBiFunction.getNames()
            }
        } else {
            throw NotDefinedException()
        }
    }
}