package com.lemarc.sofia.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lemarc.sofia.data.settings.SettingsRepository
import com.lemarc.sofia.widget.SofiaWidgetsUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val testMode: Boolean = false,
)

class SettingsViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.testMode.collect { enabled ->

                _uiState.value = SettingsUiState(
                    testMode = enabled
                )

                SofiaWidgetsUpdater.updateAll(
                    getApplication<Application>().applicationContext
                )
            }
        }
    }

    fun setTestMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setTestMode(enabled)
        }
    }

    class Factory(
        private val application: Application,
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                application,
                settingsRepository
            ) as T
        }
    }
}