package com.gryffindor.smartshopping.core.common

/**
 * Shared sealed interface representing standard UI states for data-loading screens.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
