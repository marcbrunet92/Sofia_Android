package com.lemarc.sofia.data.repository

import com.lemarc.sofia.BASE_URL
import com.lemarc.sofia.SOFIA_BMUS
import com.lemarc.sofia.TEST_BMU
import com.lemarc.sofia.data.api.RemitEntryDto
import com.lemarc.sofia.data.api.SofiaApiService
import com.lemarc.sofia.data.local.RemitNoticeEntity
import com.lemarc.sofia.data.local.dao.SofiaDao
import com.lemarc.sofia.data.model.RemitNotice
import com.lemarc.sofia.util.parseApiInstant
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SofiaRemitRepository(
    private val apiService: SofiaApiService,
    private val sofiaDao: SofiaDao,
) {
    fun observeRemits(testMode: Boolean): Flow<List<RemitNotice>> {
        return sofiaDao.getRemitNotices(testMode).map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun refreshRemits(testMode: Boolean) = withContext(Dispatchers.IO) {
        val bmus = if (testMode) listOf(TEST_BMU) else SOFIA_BMUS
        val remits = bmus.map { bmuId ->
            async {
                apiService.getRemits(
                    bmuId = bmuId,
                    eventStatus = "Active",
                    limit = 100,
                    offset = 0,
                )
            }
        }.awaitAll().flatten()

        val notices = remits
            .distinctBy { it.id }
            .sortedByDescending { parseApiInstant(it.publish_time) ?: InstantMin }
            .map { it.toEntity(testMode) }

        sofiaDao.refreshRemitNotices(testMode, notices)
    }

    companion object {
        fun create(apiService: SofiaApiService, sofiaDao: SofiaDao): SofiaRemitRepository {
            return SofiaRemitRepository(apiService, sofiaDao)
        }
    }
}

fun RemitEntryDto.toEntity(testMode: Boolean): RemitNoticeEntity =
    RemitNoticeEntity(
        id = id,
        mrid = mrid,
        revisionNumber = revision_number,
        bmuId = bmu_id,
        participantId = participant_id,
        assetId = asset_id,
        unavailabilityType = unavailability_type,
        eventType = event_type,
        messageHeading = message_heading,
        fuelType = fuel_type,
        normalCapacityMw = normal_capacity_mw,
        availableCapacityMw = available_capacity_mw,
        unavailableCapacityMw = unavailable_capacity_mw,
        eventStatus = event_status,
        eventStartTime = parseApiInstant(event_start_time),
        eventEndTime = parseApiInstant(event_end_time),
        cause = cause,
        relatedInformation = related_information,
        publishTime = parseApiInstant(publish_time),
        outageProfile = outage_profile,
        isTestMode = testMode
    )

fun RemitNoticeEntity.toModel(): RemitNotice =
    RemitNotice(
        id = id,
        mrid = mrid,
        revisionNumber = revisionNumber,
        bmuId = bmuId,
        participantId = participantId,
        assetId = assetId,
        unavailabilityType = unavailabilityType,
        eventType = eventType,
        messageHeading = messageHeading,
        fuelType = fuelType,
        normalCapacityMw = normalCapacityMw,
        availableCapacityMw = availableCapacityMw,
        unavailableCapacityMw = unavailableCapacityMw,
        eventStatus = eventStatus,
        eventStartTime = eventStartTime,
        eventEndTime = eventEndTime,
        cause = cause,
        relatedInformation = relatedInformation,
        publishTime = publishTime,
        outageProfile = outageProfile
    )

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