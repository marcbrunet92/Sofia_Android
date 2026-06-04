package com.lemarc.sofia.data.model

import java.time.Instant

data class B1610Point(
    val bmuId: String,
    val timeFrom: Instant,
    val timeTo: Instant,
    val quantity: Double,
)

data class B1610Snapshot(
    val points: List<B1610Point>,
    val latestDataTimestamp: Instant?,
    val topB1610: TopB1610Windows,
)

data class TopB1610Point(
    val maxQuantity: Double,
    val maxDate: Instant?,
)

data class TopB1610Windows(
    val allTime: TopB1610Point,
    val last7Days: TopB1610Point,
    val last30Days: TopB1610Point,
    val last90Days: TopB1610Point,
) {
    companion object {
        val Empty = TopB1610Windows(
            allTime = TopB1610Point(0.0, null),
            last7Days = TopB1610Point(0.0, null),
            last30Days = TopB1610Point(0.0, null),
            last90Days = TopB1610Point(0.0, null),
        )
    }
}
