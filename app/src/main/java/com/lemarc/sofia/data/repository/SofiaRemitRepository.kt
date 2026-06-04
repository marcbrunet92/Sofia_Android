package com.lemarc.sofia.data.repository

import com.lemarc.sofia.BASE_URL
import com.lemarc.sofia.SOFIA_BMUS
import com.lemarc.sofia.TEST_BMU
import com.lemarc.sofia.data.api.RemitEntryDto
import com.lemarc.sofia.data.api.SofiaApiService
import com.lemarc.sofia.data.model.RemitNotice
import com.lemarc.sofia.util.parseApiInstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SofiaRemitRepository(
    private val apiService: SofiaApiService,
) {
    suspend fun fetchRemits(testMode: Boolean): List<RemitNotice> = withContext(Dispatchers.IO) {
        val bmus = if (testMode) listOf(TEST_BMU) else SOFIA_BMUS
        val remits = coroutineScope {
            bmus.map { bmuId ->
                async {
                    apiService.getRemits(
                        bmuId = bmuId,
                        eventStatus = "Active",
                        limit = 100,
                        offset = 0,
                    )
                }
            }.awaitAll().flatten()
        }
        remits
            .distinctBy { it.id }
            .sortedByDescending { parseApiInstant(it.publish_time) ?: InstantMin }
            .map(::toNotice)
    }

    companion object {
        fun create(): SofiaRemitRepository {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return SofiaRemitRepository(retrofit.create(SofiaApiService::class.java))
        }
    }
}

private fun toNotice(dto: RemitEntryDto): RemitNotice =
    RemitNotice(
        id = dto.id,
        mrid = dto.mrid,
        revisionNumber = dto.revision_number,
        bmuId = dto.bmu_id,
        participantId = dto.participant_id,
        assetId = dto.asset_id,
        unavailabilityType = dto.unavailability_type,
        eventType = dto.event_type,
        messageHeading = dto.message_heading,
        fuelType = dto.fuel_type,
        normalCapacityMw = dto.normal_capacity_mw,
        availableCapacityMw = dto.available_capacity_mw,
        unavailableCapacityMw = dto.unavailable_capacity_mw,
        eventStatus = dto.event_status,
        eventStartTime = parseApiInstant(dto.event_start_time),
        eventEndTime = parseApiInstant(dto.event_end_time),
        cause = dto.cause,
        relatedInformation = dto.related_information,
        publishTime = parseApiInstant(dto.publish_time),
        outageProfile = dto.outage_profile,
    )

private val InstantMin = java.time.Instant.EPOCH