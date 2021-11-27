package ru.inr.mass.data.server

import kotlinx.coroutines.flow.collect
import kotlinx.serialization.Serializable
import ru.inr.mass.data.api.NumassBlock
import ru.inr.mass.data.api.NumassPoint
import ru.inr.mass.data.api.NumassSet
import ru.inr.mass.data.api.NumassSet.Companion.NUMASS_HV_TARGET
import ru.inr.mass.data.proto.HVData
import ru.inr.mass.data.proto.HVEntry
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.provider.top
import space.kscience.visionforge.VisionBase
import space.kscience.visionforge.VisionGroupBase


public typealias SimpleAmplitudeSpectrum = Map<UShort, UInt>

private suspend fun NumassBlock.simpleAmplitudeSpectrum(): SimpleAmplitudeSpectrum {
    val res = mutableMapOf<UShort, UInt>()
    events.collect {
        res[it.amplitude] = (res[it.amplitude] ?: 0U) + 1U
    }
    return res
}

@Serializable
public class VisionOfNumassPoint(
    public val pointMeta: Meta,
    public val index: Int,
    public val voltage: Double,
    public val spectra: Map<String, SimpleAmplitudeSpectrum>,
) : VisionBase()

public suspend fun NumassPoint.toVision(): VisionOfNumassPoint = VisionOfNumassPoint(
    meta,
    index,
    voltage,
    getChannels().entries.associate { (k, v) ->
        k.toString() to v.simpleAmplitudeSpectrum()
    }
)

@Serializable
public class VisionOfNumassHv(public val hv: HVData) : VisionBase(), Iterable<HVEntry> {
    override fun iterator(): Iterator<HVEntry> = hv.iterator()
}

private val VisionOfNumassPoint.token: NameToken get() = NameToken("point", index.toString())

@Serializable
public class VisionOfNumassSet(public val points: List<VisionOfNumassPoint>) : VisionBase() {
//    init {
//        points.forEach {
//            //childrenInternal[it.token] = it
//        }
//
//    }
}

public suspend fun NumassSet.toVision(): VisionOfNumassSet = VisionOfNumassSet(points.map { it.toVision() }).apply {
    this@toVision.top<HVData>(NUMASS_HV_TARGET).forEach { (key, hv) ->
      //  set(key, VisionOfNumassHv(hv))
    }
}
