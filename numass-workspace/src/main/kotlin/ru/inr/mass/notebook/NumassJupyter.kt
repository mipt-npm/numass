package ru.inr.mass.notebook


import org.jetbrains.kotlinx.jupyter.api.HTML
import org.jetbrains.kotlinx.jupyter.api.libraries.JupyterIntegration
import ru.inr.mass.data.api.NumassBlock
import ru.inr.mass.data.api.NumassFrame
import ru.inr.mass.data.api.NumassSet
import ru.inr.mass.data.proto.NumassDirectorySet
import ru.inr.mass.workspace.Numass
import ru.inr.mass.workspace.plotNumassBlock
import ru.inr.mass.workspace.plotNumassSet
import space.kscience.dataforge.data.DataTree
import space.kscience.plotly.Plotly
import space.kscience.plotly.scatter
import space.kscience.plotly.toHTML
import space.kscience.plotly.toPage

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
            HTML(Plotly.plotNumassBlock(it).toPage().render())
        }

        render<NumassFrame> { numassFrame ->
            HTML(
                Plotly.plot {
                    scatter {
                        x.numbers = numassFrame.signal.indices.map { numassFrame.tickSize.times(it).inWholeNanoseconds }
                        y.numbers = numassFrame.signal.toList()
                    }
                }.toHTML()
            )
        }

        render<NumassSet> { numassSet ->
            HTML(Plotly.plotNumassSet(numassSet).toPage().render())
        }

        render<DataTree<NumassDirectorySet>> { tree ->
            HTML("TODO: render repository tree")
        }
    }
}