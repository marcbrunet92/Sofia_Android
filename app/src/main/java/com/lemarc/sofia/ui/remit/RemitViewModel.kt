package com.lemarc.sofia.ui.remit

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lemarc.sofia.R
import com.lemarc.sofia.SOFIA_BMUS
import com.lemarc.sofia.data.model.RemitNotice
import com.lemarc.sofia.data.repository.SofiaRemitRepository
import com.lemarc.sofia.widget.modern.UnitsData
import com.lemarc.sofia.widget.modern.computeUnitStatuses
import java.time.Instant
import kotlinx.coroutines.CancellationException
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
    val remits: List<RemitNotice> = emptyList(),
    /** Avis en vigueur puis à venir, regroupés quand ils concernent tout le parc. */
    val sections: RemitSections = RemitSections(),
    val units: UnitsData? = null,
    val lastFetchTimestamp: Instant? = null,
    @StringRes val errorMessageRes: Int? = null,
)

class RemitViewModel(
    private val repository: SofiaRemitRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RemitUiState())
    val uiState: StateFlow<RemitUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var observeJob: Job? = null

    init {
        viewModelScope.launch {
            _uiState.update { current -> current.copy() }
            startObserving()
            refresh(forceLoading = _uiState.value.remits.isEmpty())
        }
        viewModelScope.launch {
            while (true) {
                delay(120_000.milliseconds)
                refresh()
            }
        }
    }

    private fun startObserving() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repository.observeRemits().collect { remits ->
                val now = Instant.now()
                val bmus = SOFIA_BMUS
                _uiState.update {
                    it.copy(
                        remits = remits,
                        sections = groupRemits(remits, bmus, now),
                        units = UnitsData(computeUnitStatuses(remits, now), now, fromCache = false),
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
                    errorMessageRes = R.string.error_unable_to_refresh_remit,
                )
            }
            runCatching {
                repository.refreshRemits()
            }.onSuccess {
                _uiState.update { it.copy(lastFetchTimestamp = Instant.now(), errorMessageRes = null) }
            }.onFailure { throwable ->
                if (throwable is CancellationException) return@onFailure
                Log.w("RemitViewModel", "Refresh failed", throwable)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        lastFetchTimestamp = Instant.now(),
                        errorMessageRes = R.string.error_unable_to_refresh_remit,
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessageRes = null) }
    }

    class Factory(
        private val repository: SofiaRemitRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RemitViewModel(repository) as T
        }
    }
}
