package com.lemarc.sofia.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "production_points")
data class ProductionPointEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val id: String,
    val timeFrom: Instant,
    val timeTo: Instant,
    val quantity: Double,
    val isTestMode: Boolean,
)

@Entity(tableName = "b1610_points")
data class B1610PointEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val id: String,
    val timeFrom: Instant,
    val timeTo: Instant,
    val quantity: Double,
    val isTestMode: Boolean,
)

@Entity(tableName = "weather_points")
data class WeatherPointEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val id: String,
    val timeFrom: Instant,
    val timeTo: Instant,
    val quantity: Double,
)

@Entity(tableName = "remit_notices")
data class RemitNoticeEntity(
    @PrimaryKey val id: Int,
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
    val isTestMode: Boolean,
)

@Entity(tableName = "mel_points")
data class MelPointEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val id: String,
    val timeFrom: Instant,
    val timeTo: Instant,
    val quantity: Double,
    val isTestMode: Boolean,
)

@Entity(tableName = "uou_days", primaryKeys = ["isTestMode", "epochDay"])
data class UouDayEntity(
    val isTestMode: Boolean,
    val epochDay: Long,
    val outputUsableMw: Double,
)

/** Une seule ligne (id = 0) : la dernière demi-heure AGWS connue. */
@Entity(tableName = "gb_context")
data class GbContextEntity(
    @PrimaryKey val id: Int = 0,
    val timeFrom: Instant,
    val windOffshoreMw: Double?,
    val windOnshoreMw: Double?,
    val solarMw: Double?,
)
