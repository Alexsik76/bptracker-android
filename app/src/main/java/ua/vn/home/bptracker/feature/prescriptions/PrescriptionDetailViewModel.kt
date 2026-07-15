package ua.vn.home.bptracker.feature.prescriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.dto.MedicationItemReadDto
import ua.vn.home.bptracker.data.dto.PrescriptionReadDto

data class PrescriptionDetailState(
    val prescription: PrescriptionReadDto? = null,
    val items: List<MedicationItemReadDto> = emptyList(),
    val isLoading: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class PrescriptionDetailViewModel : ViewModel() {
    private val repository = ServiceLocator.prescriptionRepository
    private val _prescriptionId = MutableStateFlow<String?>(null)

    val state: StateFlow<PrescriptionDetailState> = _prescriptionId
        .filterNotNull()
        .flatMapLatest { id ->
            combine(
                repository.getPrescriptions().map { list -> list.find { it.id == id } },
                repository.getItems(id)
            ) { prescription, items ->
                PrescriptionDetailState(
                    prescription = prescription,
                    items = items
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrescriptionDetailState())

    fun setPrescriptionId(id: String) {
        _prescriptionId.value = id
    }

    fun deletePrescription() {
        val id = _prescriptionId.value ?: return
        viewModelScope.launch {
            try {
                repository.deletePrescription(id)
                _prescriptionId.value = null
                // We don't have a direct "deleted" state that closes the screen, 
                // but the UI can observe when prescription becomes null or use a flag.
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
