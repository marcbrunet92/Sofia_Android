package com.lemarc.sofia.data.model

import java.time.Instant

data class RemitNotice(
    val id: Int,
    val bmuId: String,
    val eventStatus: String,
    val eventType: String,
    val messageHeading: String,
    val cause: String,
    val unavailableCapacityMw: Double?,
    val availableCapacityMw: Double?,
    val normalCapacityMw: Double?,
    val eventStartTime: Instant?,
    val eventEndTime: Instant?,
    val publishTime: Instant?,
)
