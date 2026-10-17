package com.lemarc.sofia.data.repository

import com.lemarc.sofia.SOFIA_BMUS
import com.lemarc.sofia.data.api.RemitEntryDto
import com.lemarc.sofia.data.api.SofiaApiService
import com.lemarc.sofia.data.local.RemitNoticeEntity
import com.lemarc.sofia.data.local.dao.SofiaDao
import com.lemarc.sofia.data.model.RemitNotice
import com.lemarc.sofia.util.parseApiInstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SofiaRemitRepository(
    private val apiService: SofiaApiService,
    private val sofiaDao: SofiaDao,
) {
    fun observeRemits(): Flow<List<RemitNotice>> {
        return sofiaDao.getRemitNotices().map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun refreshRemits() = withContext(Dispatchers.IO) {
        val bmus = SOFIA_BMUS
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
            .map { it.toEntity() }

        sofiaDao.refreshRemitNotices(notices)
    }

    companion object {
        fun create(apiService: SofiaApiService, sofiaDao: SofiaDao): SofiaRemitRepository {
            return SofiaRemitRepository(apiService, sofiaDao)
        }
    }
}

fun RemitEntryDto.toEntity(): RemitNoticeEntity =
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

private val InstantMin = java.time.Instant.EPOCH