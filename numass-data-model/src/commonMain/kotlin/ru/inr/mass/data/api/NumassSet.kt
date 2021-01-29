/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inr.mass.data.api

import hep.dataforge.meta.Meta
import hep.dataforge.meta.get
import hep.dataforge.meta.long
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import hep.dataforge.provider.Provider
import kotlinx.datetime.Instant

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
    public val startTime: Instant
        get() = meta[NumassPoint.START_TIME_KEY].long?.let {
            Instant.fromEpochMilliseconds(it)
        } ?: firstPoint.startTime

    //suspend fun getHvData(): Table?

    override fun iterator(): Iterator<NumassPoint> {
        return points.iterator()
    }

    override val defaultTarget: String get() = NUMASS_POINT_TARGET

    override fun content(target: String): Map<Name, Any> {
        return if (target == NUMASS_POINT_TARGET) {
            points.associateBy { "point[${it.voltage}]".toName() }
        } else {
            super.content(target)
        }
    }

    public companion object {
        //public const val DESCRIPTION_KEY = "info"
        public const val NUMASS_POINT_TARGET: String = "point"
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
