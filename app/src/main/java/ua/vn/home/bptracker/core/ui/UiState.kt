package ua.vn.home.bptracker.core.ui

/**
 * Generic UI state for screens displaying a list or collection of data.
 */
sealed interface ListUiState<out T> {
    data object Empty : ListUiState<Nothing>
    data class Error(val message: String) : ListUiState<Nothing>
    data class Content<T>(
        val data: T,
        val isRefreshing: Boolean = false
    ) : ListUiState<T>
}

/**
 * Generic UI state for tracking the status of an asynchronous operation (e.g., Save, Delete).
 */
sealed interface OperationUiState {
    data object Idle : OperationUiState
    data object InProgress : OperationUiState
    data object Success : OperationUiState
    data class Error(val message: String) : OperationUiState
}
