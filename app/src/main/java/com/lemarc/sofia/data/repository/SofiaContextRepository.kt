package com.lemarc.sofia.data.repository

import com.lemarc.sofia.data.api.SofiaApiService
import com.lemarc.sofia.data.api.getUouFor
import com.lemarc.sofia.data.local.GbContextEntity
import com.lemarc.sofia.data.local.UouDayEntity
import com.lemarc.sofia.data.local.dao.SofiaDao
import com.lemarc.sofia.data.model.GbContext
import com.lemarc.sofia.data.model.UouDay
import com.lemarc.sofia.util.parseApiInstant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext


class SofiaContextRepository(
    private val apiService: SofiaApiService,
    private val sofiaDao: SofiaDao,
) {
    fun observeUou(): Flow<List<UouDay>> =
        sofiaDao.getUouDays().map { entities ->
            entities.map { UouDay(LocalDate.ofEpochDay(it.epochDay), it.outputUsableMw) }
        }

    fun observeGbContext(): Flow<GbContext?> =
        sofiaDao.getGbContext().map { entity ->
            entity?.let { GbContext(it.timeFrom, it.windOffshoreMw, it.windOnshoreMw, it.solarMw) }
        }

    suspend fun refreshUou() = withContext(Dispatchers.IO) {
        val days = apiService.getUouFor().map { dto ->
            UouDayEntity(
                epochDay = LocalDate.parse(dto.forecast_date).toEpochDay(),
                outputUsableMw = dto.output_usable,
            )
        }
        sofiaDao.refreshUouDays(days)
    }

    suspend fun refreshGbContext() = withContext(Dispatchers.IO) {
        val latest = apiService.getAgwsLatest()
        val timeFrom = parseApiInstant(latest.time_from) ?: return@withContext
        sofiaDao.upsertGbContext(
            GbContextEntity(
                timeFrom = timeFrom,
                windOffshoreMw = latest.wind_offshore_mw,
                windOnshoreMw = latest.wind_onshore_mw,
                solarMw = latest.solar_mw,
            )
        )
    }

    companion object {
        fun create(apiService: SofiaApiService, sofiaDao: SofiaDao): SofiaContextRepository =
            SofiaContextRepository(apiService, sofiaDao)
    }
}
