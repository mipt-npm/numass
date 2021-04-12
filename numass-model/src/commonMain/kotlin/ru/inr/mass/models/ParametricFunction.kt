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
 *
 *
 * NamedSpectrum interface.
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
interface ParametricFunction : NameSetContainer {
    fun derivValue(parName: String?, x: Double, set: Values?): Double
    fun value(x: Double, set: Values?): Double
    fun providesDeriv(name: String?): Boolean
    fun derivative(parName: String?): ParametricFunction? {
        return if (providesDeriv(parName)) {
            object : ParametricFunction {
                override fun derivValue(parName: String?, x: Double, set: Values?): Double {
                    return if (names.contains(parName)) {
                        throw NotDefinedException()
                    } else {
                        0
                    }
                }

                override fun value(x: Double, set: Values?): Double {
                    return this@ParametricFunction.derivValue(parName, x, set)
                }

                override fun providesDeriv(name: String?): Boolean {
                    return !names.contains(name)
                }

                val names: NameList
                    get() = this@ParametricFunction.getNames()
            }
        } else {
            throw NotDefinedException()
        }
    }
}