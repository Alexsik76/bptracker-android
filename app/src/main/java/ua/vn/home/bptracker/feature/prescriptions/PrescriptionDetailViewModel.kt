package ua.vn.home.bptracker.feature.prescriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.core.ui.ListUiState
import ua.vn.home.bptracker.data.dto.MedicationItemReadDto
import ua.vn.home.bptracker.data.dto.PrescriptionReadDto

data class PrescriptionDetailPayload(
    val prescription: PrescriptionReadDto,
    val items: List<MedicationItemReadDto>
)

@OptIn(ExperimentalCoroutinesApi::class)
class PrescriptionDetailViewModel : ViewModel() {
    private val repository = ServiceLocator.prescriptionRepository
    private val _prescriptionId = MutableStateFlow<String?>(null)

    val state: StateFlow<ListUiState<PrescriptionDetailPayload>> = _prescriptionId
        .filterNotNull()
        .flatMapLatest { id ->
            combine(
                repository.getPrescriptions().map { list -> list.find { it.id == id } },
                repository.getItems(id)
            ) { prescription, items ->
                if (prescription == null) {
                    ListUiState.Empty
                } else {
                    ListUiState.Content(PrescriptionDetailPayload(prescription, items))
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ListUiState.Empty
        )

    fun setPrescriptionId(id: String) {
        _prescriptionId.value = id
    }

    fun deletePrescription() {
        val id = _prescriptionId.value ?: return
        viewModelScope.launch {
            try {
                repository.deletePrescription(id)
                _prescriptionId.value = null
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun deleteItem(itemId: String) {
        val pId = _prescriptionId.value ?: return
        viewModelScope.launch {
            try {
                repository.deleteItem(pId, itemId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
