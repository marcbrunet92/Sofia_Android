package com.lemarc.sofia.data.repository

import com.lemarc.sofia.BASE_URL
import com.lemarc.sofia.HISTORY_START
import com.lemarc.sofia.data.api.SofiaApiService
import com.lemarc.sofia.data.api.WeatherEntryDto
import com.lemarc.sofia.data.local.WeatherPointEntity
import com.lemarc.sofia.data.local.dao.SofiaDao
import com.lemarc.sofia.data.model.GraphPoint
import com.lemarc.sofia.data.model.WeatherSnapshot
import com.lemarc.sofia.util.parseApiUtc
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SofiaWeatherRepository(
    private val apiService: SofiaApiService,
    private val sofiaDao: SofiaDao,
) {
    fun observeWeather(): Flow<WeatherSnapshot> {
        return sofiaDao.getWeatherPoints().map { entities ->
            val points: List<GraphPoint> = entities.map { it.toModel() }
            WeatherSnapshot(
                points = points,
                latestWindSpeed = points.lastOrNull()?.quantity,
                latestDataTimestamp = points.lastOrNull()?.timeTo,
            )
        }
    }

    suspend fun refreshWeather() = withContext(Dispatchers.IO) {
        val entries = apiService.getWeather(
            timeFrom = HISTORY_START.toString(),
            timeTo = Instant.now().toString(),
        )
        val points = entries.toGraphPoint()
        val entities: List<WeatherPointEntity> = points.map { it.toWeatherEntity() }
        sofiaDao.refreshWeatherPoints(entities)
    }

    companion object {
        fun create(apiService: SofiaApiService, sofiaDao: SofiaDao): SofiaWeatherRepository {
            return SofiaWeatherRepository(apiService, sofiaDao)
        }
    }
}

fun GraphPoint.toWeatherEntity(): WeatherPointEntity =
    WeatherPointEntity(
        id = id,
        timeFrom = timeFrom,
        timeTo = timeTo,
        quantity = quantity
    )

fun WeatherPointEntity.toModel(): GraphPoint =
    GraphPoint(
        id = id,
        timeFrom = timeFrom,
        timeTo = timeTo,
        quantity = quantity
    )

internal fun List<WeatherEntryDto>.toGraphPoint(): List<GraphPoint> =
    map { dto ->
        GraphPoint(
            id = "WEATHER",
            timeFrom = parseApiUtc(dto.time_from),
            timeTo = parseApiUtc(dto.time_to),
            quantity = dto.wind_speed,
        )
    }.sortedBy { it.timeFrom }