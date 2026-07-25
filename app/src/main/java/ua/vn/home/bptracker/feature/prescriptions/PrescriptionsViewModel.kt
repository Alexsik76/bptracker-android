package ua.vn.home.bptracker.feature.prescriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.core.ui.ListUiState
import ua.vn.home.bptracker.data.dto.PrescriptionReadDto

class PrescriptionsViewModel : ViewModel() {
    private val repository = ServiceLocator.prescriptionRepository
    private val _refreshing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val state: StateFlow<ListUiState<List<PrescriptionReadDto>>> = combine(
        repository.getPrescriptions(),
        _refreshing,
        _error
    ) { list, refreshing, error ->
        when {
            error != null && list.isEmpty() -> ListUiState.Error(error)
            list.isEmpty() && !refreshing -> ListUiState.Empty
            else -> ListUiState.Content(list, refreshing)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ListUiState.Content(emptyList(), isRefreshing = true)
    )

    fun refresh(isManual: Boolean = false) {
        viewModelScope.launch {
            if (isManual) _refreshing.value = true
            _error.value = null
            try {
                repository.refresh()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to refresh prescriptions"
            } finally {
                _refreshing.value = false
            }
        }
    }
}
