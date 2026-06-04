package com.lemarc.sofia.data.repository

import com.lemarc.sofia.BASE_URL
import com.lemarc.sofia.HISTORY_START
import com.lemarc.sofia.SOFIA_BMUS
import com.lemarc.sofia.TEST_BMU
import com.lemarc.sofia.data.api.B1610EntryDto
import com.lemarc.sofia.data.api.B1610TopProductionPointDto
import com.lemarc.sofia.data.api.B1610TopProductionWindowsDto
import com.lemarc.sofia.data.api.SofiaApiService
import com.lemarc.sofia.data.model.B1610Snapshot
import com.lemarc.sofia.data.model.GraphPoint
import com.lemarc.sofia.data.model.TopPoint
import com.lemarc.sofia.data.model.TopWindows
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
    suspend fun fetchB1610(testMode: Boolean): B1610Snapshot = withContext(Dispatchers.IO) {
        val requestedBmus = if (testMode) listOf(TEST_BMU) else SOFIA_BMUS
        val (entries, topB1610) = coroutineScope {
            val b1610Deferred = async {
                requestedBmus.map { bmuId ->
                    async {
                        apiService.getB1610(
                            bmuId = bmuId,
                            timeFrom = HISTORY_START.toString(),
                            timeTo = Instant.now().toString(),
                        )
                    }
                }.awaitAll().flatten()
            }
            val topB1610Deferred = async {
                runCatching { apiService.getB1610TopProduction() }
                    .getOrNull()
                    ?.toTopWindows()
                    ?: TopWindows.Empty
            }
            b1610Deferred.await() to topB1610Deferred.await()
        }
        val aggregatedPoints = aggregateB1610(entries, testMode)
        B1610Snapshot(
            points = aggregatedPoints,
            latestDataTimestamp = aggregatedPoints.lastOrNull()?.timeTo,
            topB1610 = topB1610,
        )
    }

    companion object {
        fun create(): SofiaB1610Repository {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return SofiaB1610Repository(retrofit.create(SofiaApiService::class.java))
        }
    }
}

internal fun aggregateB1610(
    entries: List<B1610EntryDto>,
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
                quantity = groupedEntries.sumOf { it.quantity },
            )
        }
        .sortedBy { it.timeFrom }
}

private fun B1610TopProductionWindowsDto.toTopWindows(): TopWindows =
    TopWindows(
        allTime = all_time.toTopPoint(),
        last7Days = last_7_days.toTopPoint(),
        last30Days = last_30_days.toTopPoint(),
        last90Days = last_90_days.toTopPoint(),
    )

private fun B1610TopProductionPointDto.toTopPoint(): TopPoint =
    TopPoint(
        maxQuantity = quantity,
        maxDate = parseApiInstant(max_date),
    )
