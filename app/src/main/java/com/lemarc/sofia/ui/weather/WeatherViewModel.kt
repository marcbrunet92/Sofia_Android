package com.lemarc.sofia.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lemarc.sofia.TimeWindow
import com.lemarc.sofia.data.model.GraphPoint
import com.lemarc.sofia.data.repository.SofiaProductionRepository
import com.lemarc.sofia.data.repository.SofiaWeatherRepository
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class WeatherUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val weatherPoints: List<GraphPoint> = emptyList(),
    val latestWindSpeed: Double? = null,
    val latestDataTimestamp: Instant? = null,
    val lastFetchTimestamp: Instant? = null,
    val selectedWindow: TimeWindow = TimeWindow.HOURS_24,
    val errorMessage: String? = null,
)

class WeatherViewModel(
    private val weatherRepository: SofiaWeatherRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    init {
        refresh(forceLoading = true)
        viewModelScope.launch {
            while (true) {
                delay(60_000.milliseconds)
                refresh()
            }
        }
    }

    fun selectWindow(window: TimeWindow) {
        _uiState.update { it.copy(selectedWindow = window) }
    }

    fun refresh(forceLoading: Boolean = false) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val hasExistingData = _uiState.value.weatherPoints.isNotEmpty()
            _uiState.update {
                it.copy(
                    isLoading = forceLoading || !hasExistingData,
                    isRefreshing = hasExistingData,
                    errorMessage = null,
                )
            }
            runCatching {
                coroutineScope {
                    val weatherDeferred = async { weatherRepository.fetchWeather() }
                    weatherDeferred.await()
                }
            }.onSuccess { weatherSnapshot ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        weatherPoints = weatherSnapshot.points,
                        latestWindSpeed = weatherSnapshot.latestWindSpeed,
                        latestDataTimestamp = weatherSnapshot.latestDataTimestamp,
                        lastFetchTimestamp = Instant.now(),
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        lastFetchTimestamp = Instant.now(),
                        errorMessage = throwable.message ?: "Unable to refresh weather data.",
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    class Factory(
        private val weatherRepository: SofiaWeatherRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WeatherViewModel(weatherRepository) as T
    }
}