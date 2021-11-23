/*
 * Copyright  2017 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ru.inr.mass.data.analysis

import ru.inr.mass.data.api.NumassBlock
import ru.inr.mass.data.api.SignalProcessor
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.descriptors.MetaDescriptor

/**
 * Block analyzer that can perform debunching
 * Created by darksnake on 11.07.2017.
 */
public class DebunchAnalyzer(processor: SignalProcessor? = null) : AbstractAnalyzer(processor) {

    override suspend fun analyze(block: NumassBlock, config: Meta): NumassAnalyzerResult {
        TODO()
    }

    override val descriptor: MetaDescriptor? = null
}
