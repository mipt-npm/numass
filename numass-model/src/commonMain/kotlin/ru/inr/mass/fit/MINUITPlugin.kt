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
package hep.dataforge.stat.fit

import hep.dataforge.context.*

/**
 * Мэнеджер для MINUITа. Пока не играет никакой активной роли кроме ведения
 * внутреннего лога.
 *
 * @author Darksnake
 * @version $Id: $Id
 */
@PluginDef(group = "hep.dataforge",
    name = "MINUIT",
    dependsOn = ["hep.dataforge:fitting"],
    info = "The MINUIT fitter engine for DataForge fitting")
class MINUITPlugin : BasicPlugin() {
    fun attach(@NotNull context: Context?) {
        super.attach(context)
        clearStaticLog()
    }

    @Provides(Fitter.FITTER_TARGET)
    fun getFitter(fitterName: String): Fitter? {
        return if (fitterName == "MINUIT") {
            MINUITFitter()
        } else {
            null
        }
    }

    @ProvidesNames(Fitter.FITTER_TARGET)
    fun listFitters(): List<String> {
        return listOf("MINUIT")
    }

    fun detach() {
        clearStaticLog()
        super.detach()
    }

    class Factory : PluginFactory() {
        fun build(meta: Meta?): Plugin {
            return MINUITPlugin()
        }

        fun getType(): java.lang.Class<out Plugin?> {
            return MINUITPlugin::class.java
        }
    }

    companion object {
        /**
         * Constant `staticLog`
         */
        private val staticLog: Chronicle? = Chronicle("MINUIT-STATIC", Global.INSTANCE.getHistory())

        /**
         *
         *
         * clearStaticLog.
         */
        fun clearStaticLog() {
            staticLog.clear()
        }

        /**
         *
         *
         * logStatic.
         *
         * @param str  a [String] object.
         * @param pars a [Object] object.
         */
        fun logStatic(str: String?, vararg pars: Any?) {
            checkNotNull(staticLog) { "MINUIT log is not initialized." }
            staticLog.report(str, pars)
            LoggerFactory.getLogger("MINUIT").info(String.format(str, *pars))
            //        Out.out.printf(str,pars);
//        Out.out.println();
        }
    }
}