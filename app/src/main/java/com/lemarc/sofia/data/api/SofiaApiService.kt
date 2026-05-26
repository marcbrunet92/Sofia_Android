package com.lemarc.sofia.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SofiaApiService {
    @GET("pn/{bmuId}")
    suspend fun getProduction(
        @Path("bmuId") bmuId: String,
        @Query("time_from") timeFrom: String,
        @Query("time_to") timeTo: String,
    ): List<PnEntryDto>

    @GET("remit")
    suspend fun getRemits(
        @Query("bmu_id") bmuId: String? = null,
        @Query("event_status") eventStatus: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
    ): List<RemitEntryDto>
}

data class PnEntryDto(
    val bmu_id: String,
    val time_from: String,
    val time_to: String,
    val settlement_period: Int,
    val level_mw: Double,
    val source: String,
)

data class RemitEntryDto(
    val id: Int,
    val mrid: String,
    val revision_number: Int,
    val bmu_id: String,
    val participant_id: String,
    val asset_id: String,
    val unavailability_type: String,
    val event_type: String,
    val message_heading: String,
    val fuel_type: String,
    val normal_capacity_mw: Double?,
    val available_capacity_mw: Double?,
    val unavailable_capacity_mw: Double?,
    val event_status: String,
    val event_start_time: String?,
    val event_end_time: String?,
    val cause: String,
    val related_information: String,
    val publish_time: String?,
    val outage_profile: String,
)
