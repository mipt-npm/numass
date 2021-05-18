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

import space.kscience.kmath.misc.Symbol
import space.kscience.kmath.misc.symbol

/**
 *
 */
public class NBkgSpectrum(public val source: Spectrum) : DifferentiableSpectrum {
    override fun invoke(x: Double, arguments: Map<Symbol, Double>): Double {
        val normValue = arguments[norm] ?: 1.0
        val bkgValue = arguments[bkg] ?: 0.0
        return normValue * source(x, arguments) + bkgValue
    }

    override fun derivativeOrNull(symbols: List<Symbol>): Spectrum? = when {
        symbols.isEmpty() -> this
        symbols.size == 1 -> when (symbols.first()) {
            norm -> Spectrum { x, arguments -> source(x, arguments) + (arguments[bkg] ?: 0.0) }
            bkg -> Spectrum { x, arguments -> (arguments[norm] ?: 1.0) * source(x, arguments) }
            else -> (source as? DifferentiableSpectrum)?.derivativeOrNull(symbols)?.let { NBkgSpectrum(it) }
        }
        else -> null
    }

    public companion object {
        public val norm: Symbol by symbol
        public val bkg: Symbol by symbol
    }
}

/**
 * Apply transformation adding norming-factor and the background
 */
public fun Spectrum.withNBkg(): NBkgSpectrum = NBkgSpectrum(this)
