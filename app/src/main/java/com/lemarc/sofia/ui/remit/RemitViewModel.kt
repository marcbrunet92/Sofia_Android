package com.lemarc.sofia.ui.remit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lemarc.sofia.data.model.RemitNotice
import com.lemarc.sofia.data.repository.SofiaRemitRepository
import com.lemarc.sofia.data.settings.SettingsRepository
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class RemitUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val testMode: Boolean = false,
    val remits: List<RemitNotice> = emptyList(),
    val lastFetchTimestamp: Instant? = null,
    val errorMessage: String? = null,
)

class RemitViewModel(
    private val repository: SofiaRemitRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RemitUiState())
    val uiState: StateFlow<RemitUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var observeJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.testMode.collect { enabled ->
                _uiState.update { current -> current.copy(testMode = enabled) }
                startObserving(enabled)
                refresh(forceLoading = _uiState.value.remits.isEmpty())
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(120_000.milliseconds)
                refresh()
            }
        }
    }

    private fun startObserving(testMode: Boolean) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repository.observeRemits(testMode).collect { remits ->
                _uiState.update {
                    it.copy(
                        remits = remits,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            }
        }
    }

    fun refresh(forceLoading: Boolean = false) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val hasExistingData = _uiState.value.remits.isNotEmpty()
            _uiState.update {
                it.copy(
                    isLoading = forceLoading || !hasExistingData,
                    isRefreshing = hasExistingData,
                    errorMessage = null,
                )
            }
            runCatching {
                repository.refreshRemits(_uiState.value.testMode)
            }.onSuccess {
                _uiState.update { it.copy(lastFetchTimestamp = Instant.now(), errorMessage = null) }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        lastFetchTimestamp = Instant.now(),
                        errorMessage = throwable.message ?: "Unable to refresh REMIT data.",
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    class Factory(
        private val repository: SofiaRemitRepository,
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RemitViewModel(repository, settingsRepository) as T
        }
    }
}
