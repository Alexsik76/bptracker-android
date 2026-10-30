package ua.vn.home.bptracker.feature.home

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.dto.MeasurementDto
import ua.vn.home.bptracker.data.repository.MeasurementRepository

data class MeasurementDetailState(
    val measurement: MeasurementDto? = null,
    val deleting: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null
)

class MeasurementDetailViewModel @JvmOverloads constructor(
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val repository: MeasurementRepository = ServiceLocator.measurementRepository
) : ViewModel() {
    private val _state = MutableStateFlow(MeasurementDetailState())
    val state: StateFlow<MeasurementDetailState> = _state.asStateFlow()

    private fun getString(@androidx.annotation.StringRes resId: Int): String {
        return try {
            ServiceLocator.applicationContext.getString(resId)
        } catch (_: Exception) {
            "Delete failed"
        }
    }

    init {
        val measurementId = savedStateHandle.get<String>("measurementId")
        if (measurementId != null) {
            viewModelScope.launch {
                val measurement = repository.getMeasurement(measurementId)
                if (measurement != null) {
                    _state.value = _state.value.copy(measurement = measurement)
                }
            }
        }
    }

    fun setMeasurement(m: MeasurementDto) {
        _state.value = _state.value.copy(measurement = m, error = null)
    }

    fun delete() {
        val id = _state.value.measurement?.id
            ?: savedStateHandle.get<String>("measurementId")
            ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(deleting = true, error = null)
            try {
                repository.deleteMeasurement(id)
                _state.value = _state.value.copy(deleting = false, deleted = true)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.value = _state.value.copy(deleting = false, error = getString(R.string.error_delete_failed))
            }
        }
    }
}
