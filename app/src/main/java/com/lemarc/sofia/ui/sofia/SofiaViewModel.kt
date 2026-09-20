package com.lemarc.sofia.ui.sofia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SofiaUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class SofiaViewModel : ViewModel() {

    private val _state = MutableStateFlow(SofiaUiState())
    val state: StateFlow<SofiaUiState> = _state.asStateFlow()

    fun onDismissError() {
        _state.update { it.copy(errorMessage = null) }
    }

    class Factory(
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SofiaViewModel(
            ) as T
        }
    }
}