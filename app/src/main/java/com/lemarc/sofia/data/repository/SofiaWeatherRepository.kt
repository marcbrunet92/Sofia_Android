package com.lemarc.sofia.data.repository

import com.lemarc.sofia.data.api.SofiaApiService
import com.lemarc.sofia.data.api.WeatherEntryDto
import com.lemarc.sofia.data.model.WeatherPoint
import com.lemarc.sofia.data.model.WeatherSnapshot
import com.lemarc.sofia.util.parseApiUtc
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SofiaWeatherRepository(
    private val apiService: SofiaApiService,
) {
    suspend fun fetchWeather(): WeatherSnapshot = withContext(Dispatchers.IO) {
        val entries = apiService.getWeather(
            timeFrom = HISTORY_START.toString(),
            timeTo = Instant.now().toString(),
        )
        val points = entries.toWeatherPoints()
        WeatherSnapshot(
            points = points,
            latestWindSpeed = points.lastOrNull()?.windSpeed,
            latestDataTimestamp = points.lastOrNull()?.timeTo,
        )
    }

    companion object {
        val HISTORY_START: Instant = Instant.parse("2026-04-01T00:00:00Z")

        fun create(): SofiaWeatherRepository {
            val retrofit = Retrofit.Builder()
                .baseUrl(SofiaProductionRepository.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return SofiaWeatherRepository(retrofit.create(SofiaApiService::class.java))
        }
    }
}

internal fun List<WeatherEntryDto>.toWeatherPoints(): List<WeatherPoint> =
    map { dto ->
        WeatherPoint(
            timeFrom = parseApiUtc(dto.time_from),
            timeTo = parseApiUtc(dto.time_to),
            windSpeed = dto.wind_speed,
        )
    }.sortedBy { it.timeFrom }