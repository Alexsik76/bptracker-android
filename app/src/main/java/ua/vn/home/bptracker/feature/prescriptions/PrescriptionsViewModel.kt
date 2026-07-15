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

    val state: StateFlow<PrescriptionsState> = combine(
        repository.getPrescriptions(),
        _refreshing
    ) { list, refreshing ->
        PrescriptionsState(
            list = list,
            isLoading = refreshing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrescriptionsState())

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            try {
                repository.refresh()
            } catch (e: Exception) {
                // Error handling can be added to state if needed
            } finally {
                _refreshing.value = false
            }
        }
    }
}
