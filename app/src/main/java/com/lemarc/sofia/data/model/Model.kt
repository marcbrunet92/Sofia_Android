package com.lemarc.sofia.data.model

import java.time.Instant

data class GraphPoint(
    val id: String,
    val timeFrom: Instant,
    val timeTo: Instant,
    val quantity: Double,
)

data class B1610Snapshot(
    val points: List<GraphPoint>,
    val latestDataTimestamp: Instant?,
    val topB1610: TopWindows,
)

data class WeatherSnapshot(
    val points: List<GraphPoint>,
    val latestWindSpeed: Double?,
    val latestDataTimestamp: Instant?,
)

data class ProductionSnapshot(
    val points: List<GraphPoint>,
    val currentMw: Double,
    val latestDataTimestamp: Instant?,
    val topProduction: TopWindows,
)

data class TopPoint(
    val maxQuantity: Double,
    val maxDate: Instant?,
)

data class TopWindows(
    val allTime: TopPoint,
    val last7Days: TopPoint,
    val last30Days: TopPoint,
    val last90Days: TopPoint,
) {
    companion object {
        val Empty = TopWindows(
            allTime = TopPoint(0.0, null),
            last7Days = TopPoint(0.0, null),
            last30Days = TopPoint(0.0, null),
            last90Days = TopPoint(0.0, null),
        )
    }
}

data class RemitNotice(
    val id: Int,
    val mrid: String,
    val revisionNumber: Int,
    val bmuId: String,
    val participantId: String,
    val assetId: String,
    val unavailabilityType: String,
    val eventType: String,
    val messageHeading: String,
    val fuelType: String,
    val normalCapacityMw: Double?,
    val availableCapacityMw: Double?,
    val unavailableCapacityMw: Double?,
    val eventStatus: String,
    val eventStartTime: Instant?,
    val eventEndTime: Instant?,
    val cause: String,
    val relatedInformation: String,
    val publishTime: Instant?,
    val outageProfile: String,
)