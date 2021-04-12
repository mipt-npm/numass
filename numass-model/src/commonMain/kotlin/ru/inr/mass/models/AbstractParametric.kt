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

abstract class AbstractParametric : AbstractNamedSet {
    constructor(names: NameList?) : super(names) {}
    constructor(list: Array<String?>?) : super(list) {}
    constructor(set: NameSetContainer?) : super(set) {}

    /**
     * Provide default value for parameter with name `name`. Throws
     * NameNotFound if no default found for given parameter.
     *
     * @param name
     * @return
     */
    protected fun getDefaultParameter(name: String?): Double {
        throw NameNotFoundException(name)
    }

    /**
     * Extract value from input vector using default value if required parameter
     * not found
     *
     * @param name
     * @param set
     * @return
     */
    protected fun getParameter(name: String?, set: Values): Double {
        //FIXME add default value
        return set.getDouble(name)
    }
}