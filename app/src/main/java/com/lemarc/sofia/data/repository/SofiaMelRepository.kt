package com.lemarc.sofia.data.repository

import com.lemarc.sofia.HISTORY_START
import com.lemarc.sofia.TEST_BMU
import com.lemarc.sofia.data.api.MelEntryDto
import com.lemarc.sofia.data.api.SofiaApiService
import com.lemarc.sofia.data.api.getMelFor
import com.lemarc.sofia.data.local.MelPointEntity
import com.lemarc.sofia.data.local.dao.SofiaDao
import com.lemarc.sofia.data.model.GraphPoint
import com.lemarc.sofia.util.parseApiUtc
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** MEL (Maximum Export Limit) : puissance maximale exportable déclarée, en MW. */
class SofiaMelRepository(
    private val apiService: SofiaApiService,
    private val sofiaDao: SofiaDao,
) {
    fun observeMel(testMode: Boolean): Flow<List<GraphPoint>> =
        sofiaDao.getMelPoints(testMode).map { entities -> entities.map { it.toModel() } }

    suspend fun refreshMel(testMode: Boolean) = withContext(Dispatchers.IO) {
        // Une seule série : la somme Sofia est calculée par le serveur
        val entries = apiService.getMelFor(
            testMode = testMode,
            timeFrom = HISTORY_START.toString(),
            timeTo = Instant.now().toString(),
        )
        val id = if (testMode) TEST_BMU else "SOFIA_TOTAL"
        sofiaDao.refreshMelPoints(testMode, entries.toMelPoints(id).map { it.toMelEntity(testMode) })
    }

    companion object {
        fun create(apiService: SofiaApiService, sofiaDao: SofiaDao): SofiaMelRepository =
            SofiaMelRepository(apiService, sofiaDao)
    }
}

internal fun List<MelEntryDto>.toMelPoints(id: String): List<GraphPoint> =
    map { dto ->
        GraphPoint(
            id = id,
            timeFrom = parseApiUtc(dto.time_from),
            timeTo = parseApiUtc(dto.time_to),
            quantity = dto.level_mw,
        )
    }.sortedBy { it.timeFrom }

fun GraphPoint.toMelEntity(testMode: Boolean): MelPointEntity =
    MelPointEntity(id = id, timeFrom = timeFrom, timeTo = timeTo, quantity = quantity, isTestMode = testMode)

fun MelPointEntity.toModel(): GraphPoint =
    GraphPoint(id = id, timeFrom = timeFrom, timeTo = timeTo, quantity = quantity)
