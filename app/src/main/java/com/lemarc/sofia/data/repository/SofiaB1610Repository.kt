package com.lemarc.sofia.data.repository

import com.lemarc.sofia.data.api.B1610EntryDto
import com.lemarc.sofia.data.api.B1610TopProductionPointDto
import com.lemarc.sofia.data.api.B1610TopProductionWindowsDto
import com.lemarc.sofia.data.api.SofiaApiService
import com.lemarc.sofia.data.model.ProductionPoint
import com.lemarc.sofia.data.model.ProductionSnapshot
import com.lemarc.sofia.data.model.TopProductionPoint
import com.lemarc.sofia.data.model.TopProductionWindows
import com.lemarc.sofia.util.parseApiInstant
import com.lemarc.sofia.util.parseApiUtc
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SofiaB1610Repository(
    private val apiService: SofiaApiService,
) {
    suspend fun fetchB1610(testMode: Boolean): ProductionSnapshot = withContext(Dispatchers.IO) {
        val requestedBmus = if (testMode) listOf(SofiaProductionRepository.TEST_BMU) else SofiaProductionRepository.SOFIA_BMUS
        val (entries, topProduction) = coroutineScope {
            val b1610Deferred = async {
                requestedBmus.map { bmuId ->
                    async {
                        apiService.getB1610(
                            bmuId = bmuId,
                            timeFrom = SofiaProductionRepository.HISTORY_START.toString(),
                            timeTo = Instant.now().toString(),
                        )
                    }
                }.awaitAll().flatten()
            }
            val topProductionDeferred = async {
                runCatching { apiService.getB1610TopProduction() }
                    .getOrNull()
                    ?.toTopProductionWindows()
                    ?: TopProductionWindows.Empty
            }
            b1610Deferred.await() to topProductionDeferred.await()
        }
        val aggregatedPoints = aggregateB1610(entries, testMode)
        ProductionSnapshot(
            points = aggregatedPoints,
            currentMw = aggregatedPoints.lastOrNull()?.levelMw ?: 0.0,
            maxCapacityMw = SofiaProductionRepository.SOFIA_MAX_CAPACITY_MW,
            latestDataTimestamp = aggregatedPoints.lastOrNull()?.timeTo,
            topProduction = topProduction,
        )
    }

    companion object {
        fun create(): SofiaB1610Repository {
            val retrofit = Retrofit.Builder()
                .baseUrl(SofiaProductionRepository.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return SofiaB1610Repository(retrofit.create(SofiaApiService::class.java))
        }
    }
}

internal fun aggregateB1610(
    entries: List<B1610EntryDto>,
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
                levelMw = groupedEntries.sumOf { it.quantity },
            )
        }
        .sortedBy { it.timeFrom }
}

private fun B1610TopProductionWindowsDto.toTopProductionWindows(): TopProductionWindows =
    TopProductionWindows(
        allTime = all_time.toTopProductionPoint(),
        last7Days = last_7_days.toTopProductionPoint(),
        last30Days = last_30_days.toTopProductionPoint(),
        last90Days = last_90_days.toTopProductionPoint(),
    )

private fun B1610TopProductionPointDto.toTopProductionPoint(): TopProductionPoint =
    TopProductionPoint(
        maxMw = quantity,
        maxDate = parseApiInstant(max_date),
    )
