package ru.inr.mass.scripts

import inr.numass.models.sterile.NumassBeta.e0
import inr.numass.models.sterile.NumassBeta.mnu2
import inr.numass.models.sterile.NumassBeta.msterile2
import inr.numass.models.sterile.NumassBeta.u2
import inr.numass.models.sterile.NumassResolution
import inr.numass.models.sterile.NumassTransmission
import inr.numass.models.sterile.NumassTransmission.Companion.thickness
import inr.numass.models.sterile.NumassTransmission.Companion.trap
import inr.numass.models.sterile.SterileNeutrinoSpectrum
import kotlinx.html.code
import ru.inr.mass.models.NBkgSpectrum.Companion.bkg
import ru.inr.mass.models.NBkgSpectrum.Companion.norm
import ru.inr.mass.models.withNBkg
import ru.inr.mass.workspace.buffer
import space.kscience.kmath.misc.Symbol
import space.kscience.kmath.real.step
import space.kscience.plotly.*
import space.kscience.plotly.models.ScatterMode
import space.kscience.plotly.models.appendXY
import kotlin.math.pow

fun main() {
    val spectrum = SterileNeutrinoSpectrum().withNBkg()

    val args: Map<Symbol, Double> = mapOf(
        norm to 8e5,
        bkg to 2.0,
        mnu2 to 0.0,
        e0 to 18575.0,
        msterile2 to 1000.0.pow(2),
        u2 to 1e-2,
        thickness to 0.1,
        trap to 1.0
    )

    Plotly.page {
        plot {
            scatter {
                name = "Computed spectrum"
                mode = ScatterMode.lines
                x.buffer = 14000.0..18600.0 step 10.0
                y.numbers = x.doubles.map { spectrum(it, args) }
            }
            scatter {
                name = "Old spectrum"
                mode = ScatterMode.markers
                javaClass.getResource("/old-spectrum.dat").readText().lines().map {
                    val (u, w) = it.split("\t")
                    appendXY(u.toDouble(), w.toDouble())
                }
            }
            layout {
                title = "Sterile neutrino spectrum"
            }
        }
        plot {
            val resolution = NumassResolution()
            scatter {
                name = "resolution"
                x.buffer = 14000.0..14015.0 step 0.1
                y.numbers = x.doubles.map { resolution(it, 14005.0, args) }
            }
            layout {
                title = "Resolution, U = 14005.0"
            }
        }
        plot {
            val transmission = NumassTransmission()
            scatter {
                name = "transmission"
                x.buffer = 14000.0..14100.0 step 0.2
                y.numbers = x.doubles.map { transmission(it, 14005.0, args) }
            }
            layout {
                title = "Resolution, U = 14005.0"
            }
        }

        code {
            +"""
            norm to 8e5,
            bkg to 2.0,
            mnu2 to 0.0,
            e0 to 18575.0,
            msterile2 to 1000.0.pow(2),
            u2 to 1e-2,
            thickness to 0.1,
            trap to 1.0 
            """.trimIndent()
        }
    }.makeFile()

    println(spectrum(14000.0, args))

}