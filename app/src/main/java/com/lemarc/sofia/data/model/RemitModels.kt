package com.lemarc.sofia.data.model

import java.time.Instant

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