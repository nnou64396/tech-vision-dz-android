package com.techvisiondz.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techvisiondz.app.core.ui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Home screen state holder.
 *
 * Follows the MVVM pattern used across the app:
 *   - exposes immutable [StateFlow] for the UI
 *   - owns the loading/success/error/empty decision via [UiState]
 *   - will call into a repository (Supabase-backed) once data sources exist
 */
class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<HomeContent>>(UiState.Loading)
    val uiState: StateFlow<UiState<HomeContent>> = _uiState.asStateFlow()

    init {
        loadHome()
    }

    fun loadHome() {
        _uiState.value = UiState.Loading
        // No data source exists yet in this phase. A Supabase-backed repository
        // will be wired here so content published through the existing backend
        // surfaces automatically. Until then the screen honestly reports "empty".
        _uiState.value = UiState.Success(HomeContent(articles = emptyList()))
    }
}