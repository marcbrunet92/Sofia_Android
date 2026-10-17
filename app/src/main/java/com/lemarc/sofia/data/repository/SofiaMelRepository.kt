package com.lemarc.sofia.data.repository

import com.lemarc.sofia.HISTORY_START
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
    fun observeMel(): Flow<List<GraphPoint>> =
        sofiaDao.getMelPoints().map { entities -> entities.map { it.toModel() } }

    suspend fun refreshMel() = withContext(Dispatchers.IO) {
        // Une seule série : la somme Sofia est calculée par le serveur
        val entries = apiService.getMelFor(
            timeFrom = HISTORY_START.toString(),
            timeTo = Instant.now().toString(),
        )
        val id = "SOFIA_TOTAL"
        sofiaDao.refreshMelPoints(entries.toMelPoints(id).map { it.toMelEntity() })
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

fun GraphPoint.toMelEntity(): MelPointEntity =
    MelPointEntity(id = id, timeFrom = timeFrom, timeTo = timeTo, quantity = quantity)

fun MelPointEntity.toModel(): GraphPoint =
    GraphPoint(id = id, timeFrom = timeFrom, timeTo = timeTo, quantity = quantity)
