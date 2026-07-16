package ua.vn.home.bptracker.feature.prescriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.dto.*
import java.time.OffsetDateTime

data class MedicationItemFormState(
    val id: String? = null,
    val prescriptionId: String = "",
    val medicine: String = "",
    val condition: String? = null,
    val whenSlots: List<WhenSlot> = emptyList(),
    val doseAmount: String = "",
    val doseUnit: DoseUnit? = null,
    val freqCount: Int = 1,
    val freqPeriod: Int = 1,
    val freqPeriodUnit: FreqPeriodUnit = FreqPeriodUnit.Day,
    val courseType: CourseType = CourseType.Ongoing,
    val courseStart: String? = null,
    val courseIntakes: Int? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
) {
    val isValid: Boolean
        get() {
            val baseValid = medicine.isNotBlank() && whenSlots.isNotEmpty() && doseAmount.isNotBlank()
            return if (courseType == CourseType.Course) {
                baseValid && !courseStart.isNullOrBlank() && (courseIntakes ?: 0) > 0
            } else {
                baseValid
            }
        }
}

class MedicationItemFormViewModel : ViewModel() {
    private val repository = ServiceLocator.prescriptionRepository
    private val _state = MutableStateFlow(MedicationItemFormState())
    val state: StateFlow<MedicationItemFormState> = _state.asStateFlow()

    fun init(prescriptionId: String, itemId: String?) {
        if (itemId == null) {
            _state.value = MedicationItemFormState(
                prescriptionId = prescriptionId,
                doseUnit = DoseUnit.Mg
            )
            return
        }
        viewModelScope.launch {
            repository.getItems(prescriptionId).take(1).collect { list ->
                list.find { it.id == itemId }?.let { item ->
                    _state.value = MedicationItemFormState(
                        id = item.id,
                        prescriptionId = item.prescriptionId,
                        medicine = item.medicine,
                        condition = item.condition,
                        whenSlots = item.whenSlots,
                        doseAmount = item.doseAmount,
                        doseUnit = item.doseUnit,
                        freqCount = item.freqCount,
                        freqPeriod = item.freqPeriod,
                        freqPeriodUnit = item.freqPeriodUnit,
                        courseType = item.courseType,
                        courseStart = item.courseStart,
                        courseIntakes = item.courseIntakes
                    )
                }
            }
        }
    }

    fun onMedicineChange(value: String) { _state.value = _state.value.copy(medicine = value, error = null) }
    fun onConditionChange(value: String) { _state.value = _state.value.copy(condition = value, error = null) }
    fun onWhenSlotsChange(slot: WhenSlot, checked: Boolean) {
        val current = _state.value.whenSlots.toMutableList()
        if (checked) {
            if (!current.contains(slot)) current.add(slot)
        } else {
            current.remove(slot)
        }
        _state.value = _state.value.copy(whenSlots = current, error = null)
    }
    fun onDoseAmountChange(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        val finalValue = if (filtered.count { it == '.' } > 1) {
            val firstDotIndex = filtered.indexOf('.')
            filtered.substring(0, firstDotIndex + 1) + filtered.substring(firstDotIndex + 1).replace(".", "")
        } else {
            filtered
        }
        _state.value = _state.value.copy(doseAmount = finalValue, error = null)
    }
    fun onDoseUnitChange(value: DoseUnit?) { _state.value = _state.value.copy(doseUnit = value, error = null) }
    fun onFreqCountChange(value: Int) { _state.value = _state.value.copy(freqCount = value, error = null) }
    fun onFreqPeriodChange(value: Int) { _state.value = _state.value.copy(freqPeriod = value, error = null) }
    fun onFreqPeriodUnitChange(value: FreqPeriodUnit) { _state.value = _state.value.copy(freqPeriodUnit = value, error = null) }
    fun onCourseTypeChange(value: CourseType) {
        _state.value = _state.value.copy(
            courseType = value,
            courseStart = if (value == CourseType.Course) OffsetDateTime.now().toString() else null,
            courseIntakes = if (value == CourseType.Course) _state.value.courseIntakes ?: 1 else null,
            error = null
        )
    }
    fun onCourseStartChange(value: String) { _state.value = _state.value.copy(courseStart = value, error = null) }
    fun onCourseIntakesChange(value: Int?) { _state.value = _state.value.copy(courseIntakes = value, error = null) }

    fun save() {
        val s = _state.value
        if (!s.isValid) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            try {
                if (s.id == null) {
                    repository.createItem(s.prescriptionId, MedicationItemCreateDto(
                        medicine = s.medicine,
                        condition = s.condition,
                        whenSlots = s.whenSlots,
                        doseAmount = s.doseAmount,
                        doseUnit = s.doseUnit,
                        freqCount = s.freqCount,
                        freqPeriod = s.freqPeriod,
                        freqPeriodUnit = s.freqPeriodUnit,
                        courseType = s.courseType,
                        courseStart = s.courseStart,
                        courseIntakes = s.courseIntakes
                    ))
                } else {
                    repository.updateItem(s.prescriptionId, s.id, MedicationItemPatchDto(
                        medicine = s.medicine,
                        condition = s.condition,
                        whenSlots = s.whenSlots,
                        doseAmount = s.doseAmount,
                        doseUnit = s.doseUnit,
                        freqCount = s.freqCount,
                        freqPeriod = s.freqPeriod,
                        freqPeriodUnit = s.freqPeriodUnit,
                        courseType = s.courseType,
                        courseStart = s.courseStart,
                        courseIntakes = s.courseIntakes
                    ))
                }
                _state.value = _state.value.copy(isSaving = false, isSaved = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message ?: "Save failed")
            }
        }
    }
}
