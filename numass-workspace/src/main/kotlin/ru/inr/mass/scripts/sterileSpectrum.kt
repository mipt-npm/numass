package ru.inr.mass.scripts

import inr.numass.models.sterile.NumassBeta.e0
import inr.numass.models.sterile.NumassBeta.mnu2
import inr.numass.models.sterile.NumassBeta.msterile2
import inr.numass.models.sterile.NumassBeta.u2
import inr.numass.models.sterile.NumassTransmission.Companion.thickness
import inr.numass.models.sterile.SterileNeutrinoSpectrum
import ru.inr.mass.workspace.buffer
import space.kscience.kmath.misc.Symbol
import space.kscience.kmath.real.step
import space.kscience.plotly.Plotly
import space.kscience.plotly.makeFile
import space.kscience.plotly.models.ScatterMode
import space.kscience.plotly.scatter

fun main() {
    val spectrum = SterileNeutrinoSpectrum()
    val args: Map<Symbol, Double> = mapOf(
        mnu2 to 0.0,
        e0 to 18575.0,
        msterile2 to 1e6,
        u2 to 1e-2,
        thickness to 0.2
    )
    Plotly.plot {
        scatter {
            mode = ScatterMode.lines
            x.buffer = 14000.0..18600.0 step 10.0
            y.numbers = x.numbers.map { spectrum(it.toDouble(), args) }
        }
    }.makeFile()
}