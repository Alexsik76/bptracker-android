package ua.vn.home.bptracker.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.dto.PeriodConfig
import ua.vn.home.bptracker.data.dto.UpdateTemplateRequest
import ua.vn.home.bptracker.feature.reminders.ReminderScheduler

sealed interface ScheduleEditState {
    data object Loading : ScheduleEditState
    data object Empty : ScheduleEditState
    data class Error(val message: String) : ScheduleEditState
    data class Ready(
        val id: String,
        val durationMinutes: String,
        val maxReminders: String,
        val periods: Map<String, PeriodConfig>,
        val saving: Boolean = false,
        val saved: Boolean = false,
        val error: String? = null
    ) : ScheduleEditState {
        val isValid: Boolean
            get() = durationMinutes.toIntOrNull()?.let { it > 0 } == true &&
                    maxReminders.toIntOrNull()?.let { it > 0 } == true &&
                    periods.values.all { validateTime(it.time) }

        companion object {
            fun validateTime(time: String): Boolean {
                val parts = time.split(":")
                if (parts.size != 2) return false
                if (parts[0].length != 2 || parts[1].length != 2) return false
                val h = parts[0].toIntOrNull() ?: return false
                val m = parts[1].toIntOrNull() ?: return false
                return h in 0..23 && m in 0..59
            }
        }
    }
}

class ScheduleEditViewModel : ViewModel() {
    private val repository = ServiceLocator.reminderRepository
    private val _state = MutableStateFlow<ScheduleEditState>(ScheduleEditState.Loading)
    val state: StateFlow<ScheduleEditState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = ScheduleEditState.Loading
            try {
                val template = repository.getActiveTemplate()
                if (template == null) {
                    _state.value = ScheduleEditState.Empty
                } else {
                    _state.value = ScheduleEditState.Ready(
                        id = template.id,
                        durationMinutes = template.durationMinutes.toString(),
                        maxReminders = template.maxReminders.toString(),
                        periods = template.periods
                    )
                }
            } catch (e: Exception) {
                _state.value = ScheduleEditState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun onTimeChange(period: String, time: String) {
        val current = _state.value as? ScheduleEditState.Ready ?: return
        val updatedPeriods = current.periods.toMutableMap()
        val config = updatedPeriods[period] ?: return
        updatedPeriods[period] = config.copy(time = time)
        _state.value = current.copy(periods = updatedPeriods, error = null)
    }

    fun onDurationChange(value: String) {
        val current = _state.value as? ScheduleEditState.Ready ?: return
        _state.value = current.copy(durationMinutes = value.filter { it.isDigit() }, error = null)
    }

    fun onMaxRemindersChange(value: String) {
        val current = _state.value as? ScheduleEditState.Ready ?: return
        _state.value = current.copy(maxReminders = value.filter { it.isDigit() }, error = null)
    }

    fun save() {
        val current = _state.value as? ScheduleEditState.Ready ?: return
        if (!current.isValid) return

        viewModelScope.launch {
            _state.value = current.copy(saving = true, error = null)
            try {
                repository.updateTemplate(
                    current.id,
                    UpdateTemplateRequest(
                        periods = current.periods,
                        durationMinutes = current.durationMinutes.toInt(),
                        maxReminders = current.maxReminders.toInt()
                    )
                )
                _state.value = current.copy(saving = false, saved = true)
                
                val scheduler = ReminderScheduler(ServiceLocator.applicationContext)
                scheduler.cancelAllReminders()
                scheduler.rescheduleAll()
            } catch (e: Exception) {
                _state.value = current.copy(saving = false, error = e.message ?: "Save failed")
            }
        }
    }
}
