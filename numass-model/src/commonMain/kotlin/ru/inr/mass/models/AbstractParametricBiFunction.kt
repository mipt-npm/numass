/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.stat.parametric

import hep.dataforge.exceptions.NotDefinedException

abstract class AbstractParametricBiFunction : AbstractParametric, ParametricBiFunction {
    constructor(names: NameList?) : super(names) {}
    constructor(list: Array<String?>?) : super(list) {}
    constructor(set: NameSetContainer?) : super(set) {}

    fun derivValue(parName: String?, x: Double, y: Double, set: Values?): Double {
        return if (!getNames().contains(parName)) {
            0
        } else {
            throw NotDefinedException()
        }
    }
}