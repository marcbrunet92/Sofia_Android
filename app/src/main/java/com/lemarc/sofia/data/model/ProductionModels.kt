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
)
