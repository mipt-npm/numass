/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.stat.parametric

import hep.dataforge.names.NameList

abstract class AbstractParametricValue : AbstractParametric, ParametricValue {
    constructor(names: NameList?) : super(names) {}
    constructor(list: Array<String?>?) : super(list) {}
    constructor(set: NameSetContainer?) : super(set) {}
}