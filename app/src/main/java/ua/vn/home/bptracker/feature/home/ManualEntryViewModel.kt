package ua.vn.home.bptracker.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.bp.BpZone
import ua.vn.home.bptracker.core.di.ServiceLocator

data class ManualEntryState(
    val sys: String = "",
    val dia: String = "",
    val pulse: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
) {
    val sysInt = sys.toIntOrNull()
    val diaInt = dia.toIntOrNull()
    val pulseInt = pulse.toIntOrNull()

    val sysValid = sysInt in 40..300
    val diaValid = diaInt in 20..200
    val pulseValid = pulseInt in 30..250

    val isValid = sysValid && diaValid && pulseValid
    
    val zone: BpZone? = if (sysInt != null && diaInt != null) {
        BpZone.classify(sysInt, diaInt)
    } else null
}

class ManualEntryViewModel : ViewModel() {
    private val repository = ServiceLocator.measurementRepository
    private val _state = MutableStateFlow(ManualEntryState())
    val state: StateFlow<ManualEntryState> = _state.asStateFlow()

    fun onSysChange(value: String) {
        _state.value = _state.value.copy(sys = value.filter { it.isDigit() }, error = null)
    }

    fun onDiaChange(value: String) {
        _state.value = _state.value.copy(dia = value.filter { it.isDigit() }, error = null)
    }

    fun onPulseChange(value: String) {
        _state.value = _state.value.copy(pulse = value.filter { it.isDigit() }, error = null)
    }

    fun save() {
        val s = _state.value
        if (!s.isValid) return

        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null)
            try {
                repository.createMeasurement(
                    sys = s.sysInt!!,
                    dia = s.diaInt!!,
                    pulse = s.pulseInt!!
                )
                _state.value = _state.value.copy(saving = false, saved = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false, error = e.message ?: "Save failed")
            }
        }
    }
}
