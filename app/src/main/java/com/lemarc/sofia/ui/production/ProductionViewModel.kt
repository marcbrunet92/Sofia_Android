package com.lemarc.sofia.ui.production

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lemarc.sofia.data.model.ProductionPoint
import com.lemarc.sofia.data.model.TopProductionWindows
import com.lemarc.sofia.data.repository.SofiaProductionRepository
import com.lemarc.sofia.data.settings.SettingsRepository
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TimeWindow(val label: String, val duration: Duration?) {
    HOURS_6("6h", Duration.ofHours(6)),
    HOURS_24("24h", Duration.ofHours(24)),
    HOURS_48("48h", Duration.ofHours(48)),
    DAYS_7("7d", Duration.ofDays(7)),
    ALL("All", null),
}

data class ProductionUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val testMode: Boolean = false,
    val points: List<ProductionPoint> = emptyList(),
    val currentMw: Double = 0.0,
    val maxCapacityMw: Double = SofiaProductionRepository.SOFIA_MAX_CAPACITY_MW,
    val selectedWindow: TimeWindow = TimeWindow.HOURS_24,
    val latestDataTimestamp: Instant? = null,
    val lastFetchTimestamp: Instant? = null,
    val topProduction: TopProductionWindows = TopProductionWindows.Empty,
    val errorMessage: String? = null,
)

class ProductionViewModel(
    private val repository: SofiaProductionRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductionUiState())
    val uiState: StateFlow<ProductionUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.testMode.collect { enabled ->
                _uiState.update { current -> current.copy(testMode = enabled) }
                refresh(forceLoading = _uiState.value.points.isEmpty())
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(60_000)
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
            val hasExistingData = _uiState.value.points.isNotEmpty()
            _uiState.update {
                it.copy(
                    isLoading = forceLoading || !hasExistingData,
                    isRefreshing = hasExistingData,
                    errorMessage = null,
                )
            }
            runCatching {
                repository.fetchProduction(_uiState.value.testMode)
            }.onSuccess { snapshot ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        points = snapshot.points,
                        currentMw = snapshot.currentMw,
                        maxCapacityMw = snapshot.maxCapacityMw,
                        latestDataTimestamp = snapshot.latestDataTimestamp,
                        lastFetchTimestamp = Instant.now(),
                        topProduction = snapshot.topProduction,
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        lastFetchTimestamp = Instant.now(),
                        errorMessage = throwable.message ?: "Unable to refresh Sofia production data.",
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    class Factory(
        private val repository: SofiaProductionRepository,
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProductionViewModel(repository, settingsRepository) as T
        }
    }
}
