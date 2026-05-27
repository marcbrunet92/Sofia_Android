package com.lemarc.sofia.data.repository

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
        val bmus = if (testMode) listOf(SofiaProductionRepository.TEST_BMU) else SofiaProductionRepository.SOFIA_BMUS
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
                .baseUrl(SofiaProductionRepository.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return SofiaRemitRepository(retrofit.create(SofiaApiService::class.java))
        }
    }
}

private fun toNotice(dto: RemitEntryDto): RemitNotice =
    RemitNotice(
        id = dto.id,
        bmuId = dto.bmu_id,
        eventStatus = dto.event_status,
        eventType = dto.event_type,
        messageHeading = dto.message_heading,
        cause = dto.cause,
        unavailableCapacityMw = dto.unavailable_capacity_mw,
        availableCapacityMw = dto.available_capacity_mw,
        normalCapacityMw = dto.normal_capacity_mw,
        eventStartTime = parseApiInstant(dto.event_start_time),
        eventEndTime = parseApiInstant(dto.event_end_time),
        publishTime = parseApiInstant(dto.publish_time),
    )

private val InstantMin = java.time.Instant.EPOCH
