package ua.vn.home.bptracker.feature.prescriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.dto.PrescriptionReadDto

data class PrescriptionsState(
    val list: List<PrescriptionReadDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class PrescriptionsViewModel : ViewModel() {
    private val repository = ServiceLocator.prescriptionRepository
    private val _refreshing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val state: StateFlow<PrescriptionsState> = combine(
        repository.getPrescriptions(),
        _refreshing,
        _error
    ) { list, refreshing, error ->
        PrescriptionsState(
            list = list,
            isLoading = refreshing,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrescriptionsState())

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
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
