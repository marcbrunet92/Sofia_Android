package com.lemarc.sofia.ui.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lemarc.sofia.TimeWindow
import com.lemarc.sofia.data.model.GraphPoint
import com.lemarc.sofia.data.repository.SofiaB1610Repository
import com.lemarc.sofia.data.repository.SofiaProductionRepository
import com.lemarc.sofia.data.repository.SofiaWeatherRepository
import com.lemarc.sofia.data.settings.SettingsRepository
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

enum class GraphDataset(val label: String) { Weather("Weather"), PN("PN"), B1610("B1610") }

data class GraphUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedWindow: TimeWindow = TimeWindow.HOURS_24,
    val selectedDatasets: Set<GraphDataset> = setOf(GraphDataset.PN),
    // Raw data
    val pnPoints: List<GraphPoint> = emptyList(), // MW
    val b1610PointsMwh: List<GraphPoint> = emptyList(), // MWh per 30min
    val weatherPoints: List<GraphPoint> = emptyList(), // m/s
    // Meta
    val latestDataTimestamp: Instant? = null,
    val lastFetchTimestamp: Instant? = null,
    val errorMessage: String? = null,
    val testMode: Boolean = false,
)

class GraphViewModel(
    private val productionRepository: SofiaProductionRepository,
    private val b1610Repository: SofiaB1610Repository,
    private val weatherRepository: SofiaWeatherRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GraphUiState())
    val uiState: StateFlow<GraphUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var observeJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.testMode.collect { enabled ->
                _uiState.update { current -> current.copy(testMode = enabled) }
                startObserving(enabled)
                refresh(forceLoading = _uiState.value.pnPoints.isEmpty() && _uiState.value.b1610PointsMwh.isEmpty() && _uiState.value.weatherPoints.isEmpty())
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(60_000.milliseconds)
                refresh()
            }
        }
    }

    private fun startObserving(testMode: Boolean) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                productionRepository.observeProduction(testMode),
                b1610Repository.observeB1610(testMode),
                weatherRepository.observeWeather()
            ) { pn, b1610, weather ->
                Triple(pn, b1610, weather)
            }.collect { (pn, b1610, weather) ->
                val latestTs = listOfNotNull(
                    pn.latestDataTimestamp,
                    b1610.latestDataTimestamp,
                    weather.latestDataTimestamp,
                ).maxOrNull()
                _uiState.update {
                    it.copy(
                        pnPoints = pn.points,
                        b1610PointsMwh = b1610.points,
                        weatherPoints = weather.points,
                        latestDataTimestamp = latestTs,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            }
        }
    }

    fun selectWindow(window: TimeWindow) {
        _uiState.update { it.copy(selectedWindow = window) }
    }

    fun toggleDataset(dataset: GraphDataset) {
        _uiState.update { state ->
            val newSet = state.selectedDatasets.toMutableSet().apply {
                if (contains(dataset)) remove(dataset) else add(dataset)
            }
            // Ensure at least one dataset remains selected
            val finalSet = if (newSet.isEmpty()) setOf(dataset) else newSet
            state.copy(selectedDatasets = finalSet)
        }
    }

    fun refresh(forceLoading: Boolean = false) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val hasExistingData = _uiState.value.pnPoints.isNotEmpty() ||
                _uiState.value.b1610PointsMwh.isNotEmpty() ||
                _uiState.value.weatherPoints.isNotEmpty()

            _uiState.update {
                it.copy(
                    isLoading = forceLoading || !hasExistingData,
                    isRefreshing = hasExistingData,
                    errorMessage = null,
                )
            }

            runCatching {
                coroutineScope {
                    val pnRefresh = launch { productionRepository.refreshProduction(_uiState.value.testMode) }
                    val b1610Refresh = launch { b1610Repository.refreshB1610(_uiState.value.testMode) }
                    val weatherRefresh = launch { weatherRepository.refreshWeather() }
                    listOf(pnRefresh, b1610Refresh, weatherRefresh).forEach { it.join() }
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(
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
                        errorMessage = throwable.message ?: "Unable to refresh data.",
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // Helper: convert B1610 MWh (per 30 min) to MW
    private fun toMwFromMwh30(points: List<GraphPoint>): List<GraphPoint> =
        points.map { p -> p.copy(quantity = p.quantity * 2) }

    class Factory(
        private val productionRepository: SofiaProductionRepository,
        private val b1610Repository: SofiaB1610Repository,
        private val weatherRepository: SofiaWeatherRepository,
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GraphViewModel(productionRepository, b1610Repository, weatherRepository, settingsRepository) as T
    }
}
