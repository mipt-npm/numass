package ru.inr.mass.data.api

import kotlinx.coroutines.flow.Flow

/**
 * An ancestor to numass frame analyzers
 * Created by darksnake on 07.07.2017.
 */
public interface SignalProcessor {
    public fun analyze(frame: NumassFrame): Flow<NumassEvent>
}
