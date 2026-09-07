package com.techvisiondz.app.core.ui

/**
 * Generic UI state for async data loads.
 * Used by ViewModels to drive the UI between loading, success, error and empty
 * states in a consistent, predictable way.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>

    /** Convenience: an empty result set rendered as a distinct empty state. */
    data class Empty(val message: String = "") : UiState<Nothing>
}
