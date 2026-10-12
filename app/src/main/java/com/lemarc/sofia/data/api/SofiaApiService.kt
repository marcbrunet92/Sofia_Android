package com.lemarc.sofia.data.api

import com.lemarc.sofia.TEST_BMU
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SofiaApiService {
    // Somme des 4 unités Sofia, calculée par le serveur (bmu_id = "SOFIA_TOTAL")
    @GET("pn/total")
    suspend fun getProductionTotal(
        @Query("time_from") timeFrom: String,
        @Query("time_to") timeTo: String,
    ): List<PnEntryDto>

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

    @GET("weather")
    suspend fun getWeather(
        @Query("time_from") timeFrom: String,
        @Query("time_to") timeTo: String,
    ): List<WeatherEntryDto>

    // Somme des 4 unités Sofia, calculée par le serveur (bmu_id = "SOFIA_TOTAL")
    @GET("b1610/total")
    suspend fun getB1610Total(
        @Query("time_from") timeFrom: String,
        @Query("time_to") timeTo: String,
    ): List<B1610EntryDto>

    @GET("b1610/{bmuId}")
    suspend fun getB1610(
        @Path("bmuId") bmuId: String,
        @Query("time_from") timeFrom: String,
        @Query("time_to") timeTo: String,
    ): List<B1610EntryDto>

}

/** PN du mode courant : la somme Sofia calculée par le serveur, ou l'unité de test. */
suspend fun SofiaApiService.getProductionFor(testMode: Boolean, timeFrom: String, timeTo: String): List<PnEntryDto> =
    if (testMode) getProduction(TEST_BMU, timeFrom, timeTo) else getProductionTotal(timeFrom, timeTo)

/** B1610 du mode courant : la somme Sofia calculée par le serveur, ou l'unité de test. */
suspend fun SofiaApiService.getB1610For(testMode: Boolean, timeFrom: String, timeTo: String): List<B1610EntryDto> =
    if (testMode) getB1610(TEST_BMU, timeFrom, timeTo) else getB1610Total(timeFrom, timeTo)

data class PnEntryDto(
    val bmu_id: String,
    val time_from: String,
    val time_to: String,
    val settlement_period: Int,
    val level_mw: Double,
    val source: String,
)

data class PnTopProductionPointDto(
    val max_mw: Double,
    val max_date: String?,
)

data class PnTopProductionWindowsDto(
    val all_time: PnTopProductionPointDto,
    val last_7_days: PnTopProductionPointDto,
    val last_30_days: PnTopProductionPointDto,
    val last_90_days: PnTopProductionPointDto,
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

data class WeatherEntryDto(
    val time_from: String,
    val time_to: String,
    val wind_speed: Double,
    val source: String,
)

data class B1610EntryDto(
    val bmu_id: String,
    val time_from: String,
    val time_to: String,
    val quantity: Double,
    val settlement_period: Int,
)

data class B1610TopProductionPointDto(
    val quantity: Double,
    val max_date: String?,
)

data class B1610TopProductionWindowsDto(
    val all_time: B1610TopProductionPointDto,
    val last_7_days: B1610TopProductionPointDto,
    val last_30_days: B1610TopProductionPointDto,
    val last_90_days: B1610TopProductionPointDto,
)
