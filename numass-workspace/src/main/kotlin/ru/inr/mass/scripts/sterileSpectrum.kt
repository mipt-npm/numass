package ru.inr.mass.scripts

import kotlinx.html.code
import ru.inr.mass.models.*
import ru.inr.mass.models.NBkgSpectrum.Companion.bkg
import ru.inr.mass.models.NBkgSpectrum.Companion.norm
import ru.inr.mass.models.NumassBeta.e0
import ru.inr.mass.models.NumassBeta.mnu2
import ru.inr.mass.models.NumassBeta.msterile2
import ru.inr.mass.models.NumassBeta.u2
import ru.inr.mass.models.NumassTransmission.Companion.thickness
import ru.inr.mass.models.NumassTransmission.Companion.trap
import ru.inr.mass.workspace.buffer
import space.kscience.kmath.expressions.Symbol
import space.kscience.kmath.real.step
import space.kscience.plotly.*
import space.kscience.plotly.models.AxisType
import space.kscience.plotly.models.ScatterMode
import space.kscience.plotly.models.appendXY
import kotlin.math.pow
import kotlin.system.measureTimeMillis

fun main() {
    val spectrum = SterileNeutrinoSpectrum(fss = FSS.default).withNBkg()

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
        val spectrumTime = measureTimeMillis {
            plot {
                scatter {
                    name = "Computed spectrum"
                    mode = ScatterMode.lines
                    x.buffer = 14000.0..18600.0 step 10.0
                    y.numbers = x.doubles.map { spectrum(it, args) }
                }
                layout {
                    title = "Sterile neutrino spectrum"
                    yaxis.type = AxisType.log
                }
            }
        }
        println("Spectrum with 460 points computed in $spectrumTime millis")

        plot {
            scatter {
                mode = ScatterMode.markers
                javaClass.getResource("/old-spectrum.dat").readText().lines().map {
                    val (u, w) = it.split("\t").map { it.toDouble() }
                    appendXY(u, w / spectrum(u, args) - 1.0)
                }
            }
            layout {
                title = "Sterile neutrino old/new ratio"
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
            +args.toString()
        }
    }.makeFile()

}