package com.lemarc.sofia.data.model

import java.time.Instant

data class WeatherPoint(
    val timeFrom: Instant,
    val timeTo: Instant,
    val windSpeed: Double,
)

data class WeatherSnapshot(
    val points: List<WeatherPoint>,
    val latestWindSpeed: Double?,
    val latestDataTimestamp: Instant?,
)