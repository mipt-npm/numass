package ru.inr.mass.notebook


import org.jetbrains.kotlinx.jupyter.api.HTML
import org.jetbrains.kotlinx.jupyter.api.libraries.JupyterIntegration
import ru.inr.mass.data.api.NumassBlock
import ru.inr.mass.data.api.NumassSet
import ru.inr.mass.workspace.Numass
import ru.inr.mass.workspace.numassSet
import space.kscience.plotly.Plotly

internal class NumassJupyter : JupyterIntegration() {
    override fun Builder.onLoaded() {
        repositories(
            "https://repo.kotlin.link"
        )

        import(
            "ru.inr.mass.models.*",
            "ru.inr.mass.data.analysis.*",
            "ru.inr.mass.workspace.*",
            "ru.inr.mass.data.api.*",
            "ru.inr.mass.data.proto.*",
            "space.kscience.dataforge.data.*",
            "kotlinx.coroutines.*",
            "kotlinx.coroutines.flow.*",
        )

        import<Numass>()


        render<NumassBlock> {

        }

        render<NumassSet> { numassSet ->
            HTML(Plotly.numassSet(numassSet).render(), true)
        }
    }
}