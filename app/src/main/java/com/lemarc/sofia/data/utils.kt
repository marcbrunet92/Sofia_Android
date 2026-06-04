package com.lemarc.sofia.data

import com.lemarc.sofia.TimeWindow
import com.lemarc.sofia.data.model.GraphPoint

fun filterPoints(points: List<GraphPoint>, window: TimeWindow): List<GraphPoint> {
    val duration = window.duration ?: return points
    val lastTimestamp = points.lastOrNull()?.timeTo ?: return emptyList()
    val threshold = lastTimestamp.minus(duration)
    return points.filter { it.timeTo >= threshold }
}