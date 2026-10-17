package com.lemarc.sofia.ui.graph

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lemarc.sofia.R
import com.lemarc.sofia.TimeWindow
import com.lemarc.sofia.data.model.GbContext
import com.lemarc.sofia.data.model.GraphPoint
import com.lemarc.sofia.data.model.UouDay
import com.lemarc.sofia.data.repository.SofiaB1610Repository
import com.lemarc.sofia.data.repository.SofiaContextRepository
import com.lemarc.sofia.data.repository.SofiaMelRepository
import com.lemarc.sofia.data.repository.SofiaProductionRepository
import com.lemarc.sofia.data.repository.SofiaWeatherRepository
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

enum class GraphDataset(@StringRes val labelRes: Int) {
    Weather(R.string.dataset_weather),
    PN(R.string.dataset_pn),
    B1610(R.string.dataset_b1610),
    MEL(R.string.dataset_mel),
}

data class GraphUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedWindow: TimeWindow = TimeWindow.ALL,
    val selectedDatasets: Set<GraphDataset> = setOf(GraphDataset.PN),
    // Raw data
    val pnPoints: List<GraphPoint> = emptyList(), // MW
    val b1610PointsMwh: List<GraphPoint> = emptyList(), // MWh per 30min
    val weatherPoints: List<GraphPoint> = emptyList(), // km/h (affiché en m/s)
    val melPoints: List<GraphPoint> = emptyList(), // MW
    // Cartes sous le graphique
    val uouDays: List<UouDay> = emptyList(),
    val gbContext: GbContext? = null,
    // Meta
    val latestDataTimestamp: Instant? = null,
    val lastFetchTimestamp: Instant? = null,
    @StringRes val errorMessageRes: Int? = null,
)

class GraphViewModel(
    private val productionRepository: SofiaProductionRepository,
    private val b1610Repository: SofiaB1610Repository,
    private val weatherRepository: SofiaWeatherRepository,
    private val melRepository: SofiaMelRepository,
    private val contextRepository: SofiaContextRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GraphUiState())
    val uiState: StateFlow<GraphUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var observeJob: Job? = null
    private var observeContextJob: Job? = null

    init {
        viewModelScope.launch {
            _uiState.update { current -> current.copy() }
            startObserving()
            refresh(forceLoading = _uiState.value.pnPoints.isEmpty() && _uiState.value.b1610PointsMwh.isEmpty() && _uiState.value.weatherPoints.isEmpty())
        }
        viewModelScope.launch {
            while (true) {
                delay(60_000.milliseconds)
                refresh()
            }
        }
    }

    private fun startObserving() {
        observeContextJob?.cancel()
        observeContextJob = viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                melRepository.observeMel(),
                contextRepository.observeUou(),
                contextRepository.observeGbContext(),
            ) { mel, uou, gb -> Triple(mel, uou, gb) }
                .collect { (mel, uou, gb) ->
                    _uiState.update { it.copy(melPoints = mel, uouDays = uou, gbContext = gb) }
                }
        }
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                productionRepository.observeProduction(),
                b1610Repository.observeB1610(),
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
                    errorMessageRes  = null,
                )
            }

            runCatching {
                coroutineScope {
                    val pnRefresh = launch { productionRepository.refreshProduction() }
                    val b1610Refresh = launch { b1610Repository.refreshB1610() }
                    val weatherRefresh = launch { weatherRepository.refreshWeather() }
                    val melRefresh = launch { melRepository.refreshMel() }
                    val uouRefresh = launch { contextRepository.refreshUou() }
                    val gbRefresh = launch { contextRepository.refreshGbContext() }
                    listOf(pnRefresh, b1610Refresh, weatherRefresh, melRefresh, uouRefresh, gbRefresh).joinAll()
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        lastFetchTimestamp = Instant.now(),
                        errorMessageRes  = null,
                    )
                }
            }.onFailure { throwable ->
                if (throwable is CancellationException) return@onFailure
                android.util.Log.w("GraphViewModel", "Refresh failed", throwable)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        lastFetchTimestamp = Instant.now(),
                        errorMessageRes = R.string.error_unable_to_refresh_data,
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessageRes  = null) }
    }

    class Factory(
        private val productionRepository: SofiaProductionRepository,
        private val b1610Repository: SofiaB1610Repository,
        private val weatherRepository: SofiaWeatherRepository,
        private val melRepository: SofiaMelRepository,
        private val contextRepository: SofiaContextRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GraphViewModel(
                productionRepository,
                b1610Repository,
                weatherRepository,
                melRepository,
                contextRepository,
            ) as T
    }
}
