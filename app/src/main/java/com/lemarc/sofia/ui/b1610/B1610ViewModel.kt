package com.lemarc.sofia.ui.b1610

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lemarc.sofia.data.model.ProductionPoint
import com.lemarc.sofia.data.model.TopProductionWindows
import com.lemarc.sofia.data.repository.SofiaB1610Repository
import com.lemarc.sofia.data.repository.SofiaProductionRepository
import com.lemarc.sofia.data.settings.SettingsRepository
import com.lemarc.sofia.ui.production.TimeWindow
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class B1610UiState(
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

class B1610ViewModel(
    private val repository: SofiaB1610Repository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(B1610UiState())
    val uiState: StateFlow<B1610UiState> = _uiState.asStateFlow()

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
            val hasExistingData = _uiState.value.points.isNotEmpty()
            _uiState.update {
                it.copy(
                    isLoading = forceLoading || !hasExistingData,
                    isRefreshing = hasExistingData,
                    errorMessage = null,
                )
            }
            runCatching {
                repository.fetchB1610(_uiState.value.testMode)
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
                        errorMessage = throwable.message ?: "Unable to refresh B1610 real output data.",
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    class Factory(
        private val repository: SofiaB1610Repository,
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return B1610ViewModel(repository, settingsRepository) as T
        }
    }
}
