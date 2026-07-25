package ua.vn.home.bptracker.feature.prescriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.core.ui.OperationUiState
import java.time.LocalDate

data class PrescriptionFormState(
    val id: String? = null,
    val doctor: String = "",
    val prescribedOn: String = LocalDate.now().toString(),
    val isActive: Boolean = true,
    val saveOperation: OperationUiState = OperationUiState.Idle,
    val savedId: String? = null
) {
    val isValid = doctor.isNotBlank() && prescribedOn.isNotBlank()
}

class PrescriptionFormViewModel : ViewModel() {
    private val repository = ServiceLocator.prescriptionRepository
    private val _state = MutableStateFlow(PrescriptionFormState())
    val state: StateFlow<PrescriptionFormState> = _state.asStateFlow()

    fun init(id: String?) {
        if (id == null) {
            _state.value = PrescriptionFormState()
            return
        }
        viewModelScope.launch {
            // In a real app we'd fetch or find in cache
            repository.getPrescriptions().take(1).collect { list ->
                list.find { it.id == id }?.let { p ->
                    _state.value = PrescriptionFormState(
                        id = p.id,
                        doctor = p.doctor,
                        prescribedOn = p.prescribedOn,
                        isActive = p.isActive
                    )
                }
            }
        }
    }

    fun onDoctorChange(value: String) {
        _state.value = _state.value.copy(doctor = value, saveOperation = OperationUiState.Idle)
    }

    fun onDateChange(value: String) {
        _state.value = _state.value.copy(prescribedOn = value, saveOperation = OperationUiState.Idle)
    }

    fun onIsActiveChange(value: Boolean) {
        _state.value = _state.value.copy(isActive = value, saveOperation = OperationUiState.Idle)
    }

    fun save() {
        val s = _state.value
        if (!s.isValid) return

        viewModelScope.launch {
            _state.value = _state.value.copy(saveOperation = OperationUiState.InProgress)
            try {
                val savedId = if (s.id == null) {
                    val created = repository.createPrescription(s.doctor, s.prescribedOn)
                    created.id
                } else {
                    repository.updatePrescription(s.id, s.doctor, s.prescribedOn, s.isActive)
                    null
                }
                _state.value = _state.value.copy(saveOperation = OperationUiState.Success, savedId = savedId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(saveOperation = OperationUiState.Error(e.message ?: "Save failed"))
            }
        }
    }
}
