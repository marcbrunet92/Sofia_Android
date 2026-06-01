package com.lemarc.sofia.data.repository

import com.lemarc.sofia.data.api.PnEntryDto
import com.lemarc.sofia.data.api.PnTopProductionPointDto
import com.lemarc.sofia.data.api.PnTopProductionWindowsDto
import com.lemarc.sofia.data.api.SofiaApiService
import com.lemarc.sofia.data.model.ProductionPoint
import com.lemarc.sofia.data.model.ProductionSnapshot
import com.lemarc.sofia.data.model.TopProductionPoint
import com.lemarc.sofia.data.model.TopProductionWindows
import com.lemarc.sofia.util.parseApiInstant
import com.lemarc.sofia.util.parseApiUtc
import java.time.Instant
import kotlin.math.ceil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SofiaProductionRepository(
    private val apiService: SofiaApiService,
) {
    suspend fun fetchProduction(testMode: Boolean): ProductionSnapshot = withContext(Dispatchers.IO) {
        val requestedBmus = if (testMode) listOf(TEST_BMU) else SOFIA_BMUS
        val (entries, topProduction) = coroutineScope {
            val productionDeferred = async {
                requestedBmus.map { bmuId ->
                    async {
                        apiService.getProduction(
                            bmuId = bmuId,
                            timeFrom = HISTORY_START.toString(),
                            timeTo = Instant.now().toString(),
                        )
                    }
                }.awaitAll().flatten()
            }
            val topProductionDeferred = async {
                runCatching { apiService.getTopProduction() }
                    .getOrNull()
                    ?.toTopProductionWindows()
                    ?: TopProductionWindows.Empty
            }
            productionDeferred.await() to topProductionDeferred.await()
        }
        val aggregatedPoints = aggregateProduction(entries, testMode)
        ProductionSnapshot(
            points = aggregatedPoints,
            currentMw = aggregatedPoints.lastOrNull()?.levelMw ?: 0.0,
            maxCapacityMw = if (testMode) deriveTestCapacity(aggregatedPoints) else SOFIA_MAX_CAPACITY_MW,
            latestDataTimestamp = aggregatedPoints.lastOrNull()?.timeTo,
            topProduction = topProduction,
        )
    }

    companion object {
        const val BASE_URL = "https://sofia.lemarc.fr/"
        const val TEST_BMU = "T_HEYM11"
        const val SOFIA_MAX_CAPACITY_MW = 1400.0
        val SOFIA_BMUS = listOf("T_SOFOW-11", "T_SOFOW-12", "T_SOFOW-21", "T_SOFOW-22")
        val HISTORY_START: Instant = Instant.parse("2026-04-01T00:00:00Z")

        fun create(): SofiaProductionRepository {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return SofiaProductionRepository(retrofit.create(SofiaApiService::class.java))
        }
    }
}

internal fun aggregateProduction(
    entries: List<PnEntryDto>,
    testMode: Boolean,
): List<ProductionPoint> {
    if (entries.isEmpty()) return emptyList()
    return entries
        .groupBy { entry ->
            Triple(entry.time_from, entry.time_to, entry.settlement_period)
        }
        .map { (_, groupedEntries) ->
            val first = groupedEntries.first()
            ProductionPoint(
                bmuId = if (testMode) SofiaProductionRepository.TEST_BMU else "SOFIA_TOTAL",
                timeFrom = parseApiUtc(first.time_from),
                timeTo = parseApiUtc(first.time_to),
                settlementPeriod = first.settlement_period,
                levelMw = groupedEntries.sumOf { it.level_mw },
            )
        }
        .sortedBy { it.timeFrom }
}

private fun deriveTestCapacity(points: List<ProductionPoint>): Double {
    val peak = points.maxOfOrNull { it.levelMw } ?: 0.0
    return if (peak <= 0.0) {
        1.0
    } else {
        ceil(peak / 50.0) * 50.0
    }
}

private fun PnTopProductionWindowsDto.toTopProductionWindows(): TopProductionWindows =
    TopProductionWindows(
        allTime = all_time.toTopProductionPoint(),
        last7Days = last_7_days.toTopProductionPoint(),
        last30Days = last_30_days.toTopProductionPoint(),
        last90Days = last_90_days.toTopProductionPoint(),
    )

private fun PnTopProductionPointDto.toTopProductionPoint(): TopProductionPoint =
    TopProductionPoint(
        maxMw = max_mw,
        maxDate = parseApiInstant(max_date),
    )
