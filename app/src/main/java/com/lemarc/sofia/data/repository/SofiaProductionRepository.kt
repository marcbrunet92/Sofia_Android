package com.lemarc.sofia.data.repository

import com.lemarc.sofia.HISTORY_START
import com.lemarc.sofia.TEST_BMU
import com.lemarc.sofia.data.model.GraphPoint
import com.lemarc.sofia.data.model.TopPoint
import com.lemarc.sofia.data.model.TopWindows
import com.lemarc.sofia.data.api.PnEntryDto
import com.lemarc.sofia.data.api.SofiaApiService
import com.lemarc.sofia.data.api.getProductionFor
import com.lemarc.sofia.data.local.ProductionPointEntity
import com.lemarc.sofia.data.local.dao.SofiaDao
import com.lemarc.sofia.data.model.ProductionSnapshot
import com.lemarc.sofia.util.parseApiUtc
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SofiaProductionRepository(
    private val apiService: SofiaApiService,
    private val sofiaDao: SofiaDao,
) {
    fun observeProduction(testMode: Boolean): Flow<ProductionSnapshot> {
        return sofiaDao.getProductionPoints(testMode).map { entities ->
            val points = entities.map { it.toModel() }
            val topProduction = calculateTopProduction(points)
            ProductionSnapshot(
                points = points,
                currentMw = points.lastOrNull()?.quantity ?: 0.0,
                latestDataTimestamp = points.lastOrNull()?.timeTo,
                topProduction = topProduction,
            )
        }
    }

    suspend fun refreshProduction(testMode: Boolean) = withContext(Dispatchers.IO) {
        // Une seule série : la somme Sofia est calculée par le serveur
        val entries = apiService.getProductionFor(
            testMode = testMode,
            timeFrom = HISTORY_START.toString(),
            timeTo = Instant.now().toString(),
        )

        val aggregatedPoints = aggregateProduction(entries, testMode)
        val entities: List<ProductionPointEntity> = aggregatedPoints.map { it.toProductionEntity(testMode) }
        sofiaDao.refreshProductionPoints(testMode, entities)
    }

    companion object {
        fun create(apiService: SofiaApiService, sofiaDao: SofiaDao): SofiaProductionRepository {
            return SofiaProductionRepository(apiService, sofiaDao)
        }
    }
}

fun GraphPoint.toProductionEntity(testMode: Boolean): ProductionPointEntity =
    ProductionPointEntity(
        id = id,
        timeFrom = timeFrom,
        timeTo = timeTo,
        quantity = quantity,
        isTestMode = testMode
    )

fun ProductionPointEntity.toModel(): GraphPoint =
    GraphPoint(
        id = id,
        timeFrom = timeFrom,
        timeTo = timeTo,
        quantity = quantity
    )
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

