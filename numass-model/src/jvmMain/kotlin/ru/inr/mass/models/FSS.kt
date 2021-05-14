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
package ru.inr.mass.models

import space.kscience.kmath.data.ColumnarData
import space.kscience.kmath.misc.Symbol
import space.kscience.kmath.misc.UnstableKMathAPI
import space.kscience.kmath.misc.symbol
import space.kscience.kmath.structures.Buffer
import space.kscience.kmath.structures.DoubleBuffer


@OptIn(UnstableKMathAPI::class)
public class FSS(public val ps: DoubleBuffer, public val es: DoubleBuffer) : ColumnarData<Double> {

    override val size: Int get() = ps.size

    override fun get(symbol: Symbol): Buffer<Double>? = when (symbol) {
        p -> ps
        e -> es
        else -> null
    }

    public companion object {
        public val p: Symbol by symbol
        public val e: Symbol by symbol
    }
}