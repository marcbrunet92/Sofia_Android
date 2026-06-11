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
import com.lemarc.sofia.data.local.B1610PointEntity
import com.lemarc.sofia.data.local.dao.SofiaDao
import com.lemarc.sofia.util.parseApiInstant
import com.lemarc.sofia.util.parseApiUtc
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
import java.time.Duration

class SofiaB1610Repository(
    private val apiService: SofiaApiService,
    private val sofiaDao: SofiaDao,
) {
    fun observeB1610(testMode: Boolean): Flow<B1610Snapshot> {
        return sofiaDao.getB1610Points(testMode).map { entities ->
            val points: List<GraphPoint> = entities.map { it.toModel() }
            B1610Snapshot(
                points = points,
                latestDataTimestamp = points.lastOrNull()?.timeTo,
                topB1610 = calculateTopB1610(points)
            )
        }
    }

    suspend fun refreshB1610(testMode: Boolean) = withContext(Dispatchers.IO) {
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
        val entities: List<B1610PointEntity> = aggregatedPoints.map { it.toB1610Entity(testMode) }
        sofiaDao.refreshB1610Points(testMode, entities)
    }

    companion object {
        fun create(apiService: SofiaApiService, sofiaDao: SofiaDao): SofiaB1610Repository {
            return SofiaB1610Repository(apiService, sofiaDao)
        }
    }
}

fun GraphPoint.toB1610Entity(testMode: Boolean): B1610PointEntity =
    B1610PointEntity(
        id = id,
        timeFrom = timeFrom,
        timeTo = timeTo,
        quantity = quantity,
        isTestMode = testMode
    )

fun B1610PointEntity.toModel(): GraphPoint =
    GraphPoint(
        id = id,
        timeFrom = timeFrom,
        timeTo = timeTo,
        quantity = quantity
    )

fun calculateTopB1610(points: List<GraphPoint>): TopWindows {
    if (points.isEmpty()) return TopWindows.Empty

    fun findMax(filteredPoints: List<GraphPoint>): TopPoint {
        val maxPoint = filteredPoints.maxByOrNull { it.quantity }
        return TopPoint(
            maxQuantity = maxPoint?.quantity ?: 0.0,
            maxDate = maxPoint?.timeFrom
        )
    }

    val now = Instant.now()
    val last7Days = points.filter { it.timeFrom >= now.minus(Duration.ofDays(7)) }
    val last30Days = points.filter { it.timeFrom >= now.minus(Duration.ofDays(30)) }
    val last90Days = points.filter { it.timeFrom >= now.minus(Duration.ofDays(90)) }

    return TopWindows(
        allTime = findMax(points),
        last7Days = findMax(last7Days),
        last30Days = findMax(last30Days),
        last90Days = findMax(last90Days)
    )
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
