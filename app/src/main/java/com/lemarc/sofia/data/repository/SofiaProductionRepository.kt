package com.lemarc.sofia.data.repository

import com.lemarc.sofia.BASE_URL
import com.lemarc.sofia.HISTORY_START
import com.lemarc.sofia.SOFIA_BMUS
import com.lemarc.sofia.TEST_BMU
import com.lemarc.sofia.data.model.GraphPoint
import com.lemarc.sofia.data.model.TopPoint
import com.lemarc.sofia.data.model.TopWindows
import com.lemarc.sofia.data.api.PnEntryDto
import com.lemarc.sofia.data.api.PnTopProductionPointDto
import com.lemarc.sofia.data.api.PnTopProductionWindowsDto
import com.lemarc.sofia.data.api.SofiaApiService
import com.lemarc.sofia.data.model.ProductionSnapshot
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

class SofiaProductionRepository(
    private val apiService: SofiaApiService,
) {
    suspend fun fetchProduction(testMode: Boolean): ProductionSnapshot = withContext(Dispatchers.IO) {
        val requestedBmus = if (testMode) listOf(TEST_BMU) else SOFIA_BMUS
        var (entries, topProduction) = coroutineScope {
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
                    ?.toTopWindows()
                    ?: TopWindows.Empty
            }
            productionDeferred.await() to topProductionDeferred.await()
        }
        val aggregatedPoints = aggregateProduction(entries, testMode)
        if (topProduction.allTime.maxQuantity == 0.toDouble() && topProduction.last30Days.maxQuantity == 0.toDouble() && topProduction.last90Days.maxQuantity == 0.toDouble() && topProduction.last7Days.maxQuantity == 0.toDouble()) {
            topProduction = calculateTopProduction(aggregatedPoints)
        }
        ProductionSnapshot(
            points = aggregatedPoints,
            currentMw = aggregatedPoints.lastOrNull()?.quantity ?: 0.0,
            latestDataTimestamp = aggregatedPoints.lastOrNull()?.timeTo,
            topProduction = topProduction,
        )
    }

    companion object {
        fun create(): SofiaProductionRepository {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return SofiaProductionRepository(retrofit.create(SofiaApiService::class.java))
        }
    }
}
fun calculateTopProduction(points: List<GraphPoint>): TopWindows {
    if (points.isEmpty()) return TopWindows.Empty

    fun findMax(filteredPoints: List<GraphPoint>): TopPoint {
        val maxPoint = filteredPoints.maxByOrNull { it.quantity }
        return TopPoint(
            maxQuantity = maxPoint?.quantity ?: 0.0,
            maxDate = maxPoint?.timeFrom
        )
    }

    val now = Instant.now()
    val last7Days = points.filter { it.timeFrom >= now.minus(java.time.Duration.ofDays(7)) }
    val last30Days = points.filter { it.timeFrom >= now.minus(java.time.Duration.ofDays(30)) }
    val last90Days = points.filter { it.timeFrom >= now.minus(java.time.Duration.ofDays(90)) }

    return TopWindows(
        allTime = findMax(points),
        last7Days = findMax(last7Days),
        last30Days = findMax(last30Days),
        last90Days = findMax(last90Days)
    )
}
internal fun aggregateProduction(
    entries: List<PnEntryDto>,
    testMode: Boolean,
): List<GraphPoint> {
    if (entries.isEmpty()) return emptyList()
    return entries
        .groupBy { entry ->
            Triple(entry.time_from, entry.time_to, entry.settlement_period)
        }
        .map { (_, groupedEntries) ->
            val first = groupedEntries.first()
            GraphPoint(
                id = if (testMode) TEST_BMU else "SOFIA_TOTAL",
                timeFrom = parseApiUtc(first.time_from),
                timeTo = parseApiUtc(first.time_to),
                quantity = groupedEntries.sumOf { it.level_mw },
            )
        }
        .sortedBy { it.timeFrom }
}

private fun PnTopProductionWindowsDto.toTopWindows(): TopWindows =
    TopWindows(
        allTime = all_time.toTopPoint(),
        last7Days = last_7_days.toTopPoint(),
        last30Days = last_30_days.toTopPoint(),
        last90Days = last_90_days.toTopPoint(),
    )

private fun PnTopProductionPointDto.toTopPoint(): TopPoint =
    TopPoint(
        maxQuantity = max_mw,
        maxDate = parseApiInstant(max_date),
    )
