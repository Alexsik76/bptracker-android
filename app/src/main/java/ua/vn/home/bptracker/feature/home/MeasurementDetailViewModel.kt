package ua.vn.home.bptracker.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.dto.MeasurementDto

data class MeasurementDetailState(
    val measurement: MeasurementDto? = null,
    val deleting: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null
)

class MeasurementDetailViewModel : ViewModel() {
    private val repository = ServiceLocator.measurementRepository
    private val _state = MutableStateFlow(MeasurementDetailState())
    val state: StateFlow<MeasurementDetailState> = _state.asStateFlow()

    fun setMeasurement(m: MeasurementDto) {
        _state.value = _state.value.copy(measurement = m, error = null)
    }

    fun delete() {
        val id = _state.value.measurement?.id ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(deleting = true, error = null)
            try {
                repository.deleteMeasurement(id)
                _state.value = _state.value.copy(deleting = false, deleted = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(deleting = false, error = e.message ?: "Delete failed")
            }
        }
    }
}
