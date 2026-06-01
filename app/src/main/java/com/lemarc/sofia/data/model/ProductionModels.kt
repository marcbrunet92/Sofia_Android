package com.lemarc.sofia.data.model

import java.time.Instant

data class ProductionPoint(
    val bmuId: String,
    val timeFrom: Instant,
    val timeTo: Instant,
    val settlementPeriod: Int,
    val levelMw: Double,
)

data class ProductionSnapshot(
    val points: List<ProductionPoint>,
    val currentMw: Double,
    val maxCapacityMw: Double,
    val latestDataTimestamp: Instant?,
    val topProduction: TopProductionWindows,
)

data class TopProductionPoint(
    val maxMw: Double,
    val maxDate: Instant?,
)

data class TopProductionWindows(
    val allTime: TopProductionPoint,
    val last7Days: TopProductionPoint,
    val last30Days: TopProductionPoint,
    val last90Days: TopProductionPoint,
) {
    companion object {
        val Empty = TopProductionWindows(
            allTime = TopProductionPoint(0.0, null),
            last7Days = TopProductionPoint(0.0, null),
            last30Days = TopProductionPoint(0.0, null),
            last90Days = TopProductionPoint(0.0, null),
        )
    }
}
