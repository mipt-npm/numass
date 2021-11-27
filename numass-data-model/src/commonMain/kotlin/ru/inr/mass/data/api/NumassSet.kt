/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inr.mass.data.api

import kotlinx.datetime.Instant
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.long
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.provider.Provider

/**
 * A single set of numass measurements together with metadata.
 *
 * @author [Alexander Nozik](mailto:altavir@gmail.com)
 */
public interface NumassSet : Iterable<NumassPoint>, Provider {

    public val meta: Meta

    public val points: List<NumassPoint>

    /**
     * Get the starting time from meta or from first point
     *
     * @return
     */
    public suspend fun getStartTime(): Instant = meta[NumassPoint.START_TIME_KEY].long?.let {
        Instant.fromEpochMilliseconds(it)
    } ?: firstPoint.startTime

    //suspend fun getHvData(): Table?

    override fun iterator(): Iterator<NumassPoint> = points.iterator()

    override val defaultTarget: String get() = NUMASS_POINT_TARGET

    override fun content(target: String): Map<Name, Any> = if (target == NUMASS_POINT_TARGET) {
        points.associateBy { NameToken("point", it.voltage.toString()).asName() }
    } else {
        super.content(target)
    }

    public companion object {
        //public const val DESCRIPTION_KEY = "info"
        public const val NUMASS_POINT_TARGET: String = "numass.point"
        public const val NUMASS_HV_TARGET: String = "numass.hv"
    }
}

/**
 * List all points with given voltage
 *
 * @param voltage
 * @return
 */
public fun NumassSet.getPoints(voltage: Double): List<NumassPoint> {
    return points.filter { it -> it.voltage == voltage }.toList()
}

/**
 * Find first point with given voltage
 *
 * @param voltage
 * @return
 */
public fun NumassSet.pointOrNull(voltage: Double): NumassPoint? {
    return points.firstOrNull { it -> it.voltage == voltage }
}

/**
 * Get the first point if it exists. Throw runtime exception otherwise.
 *
 * @return
 */
public val NumassSet.firstPoint: NumassPoint
    get() = points.firstOrNull() ?: throw RuntimeException("The set is empty")
