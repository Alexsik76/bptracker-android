package ua.vn.home.bptracker.feature.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.dto.ReminderConfigDto

data class ReminderConfigState(
    val morningTime: String = "08:00:00",
    val dayTime: String = "14:00:00",
    val eveningTime: String = "20:00:00",
    val maxReminders: String = "3",
    val durationMinutes: String = "60",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
) {
    val morningHour = morningTime.substringBefore(":").toIntOrNull() ?: 8
    val morningMinute = morningTime.substringAfter(":").substringBefore(":").toIntOrNull() ?: 0

    val dayHour = dayTime.substringBefore(":").toIntOrNull() ?: 14
    val dayMinute = dayTime.substringAfter(":").substringBefore(":").toIntOrNull() ?: 0

    val eveningHour = eveningTime.substringBefore(":").toIntOrNull() ?: 20
    val eveningMinute = eveningTime.substringAfter(":").substringBefore(":").toIntOrNull() ?: 0

    val maxRemindersInt = maxReminders.toIntOrNull() ?: 0
    val durationMinutesInt = durationMinutes.toIntOrNull() ?: 0

    val isValid = maxRemindersInt > 0 && durationMinutesInt > 0
}

class ReminderConfigViewModel : ViewModel() {
    private val repository = ServiceLocator.reminderConfigRepository
    private val _state = MutableStateFlow(ReminderConfigState())
    val state: StateFlow<ReminderConfigState> = _state.asStateFlow()

    init {
        loadConfig()
    }

    fun loadConfig() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val config = repository.getConfig()
                if (config != null) {
                    _state.value = _state.value.copy(
                        morningTime = config.morningTime,
                        dayTime = config.dayTime,
                        eveningTime = config.eveningTime,
                        maxReminders = config.maxReminders.toString(),
                        durationMinutes = config.durationMinutes.toString(),
                        isLoading = false
                    )
                } else {
                    _state.value = _state.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to load config")
            }
        }
    }

    fun onTimeChange(slot: String, hour: Int, minute: Int) {
        val timeStr = String.format("%02d:%02d:00", hour, minute)
        when (slot) {
            "morning" -> _state.value = _state.value.copy(morningTime = timeStr)
            "day" -> _state.value = _state.value.copy(dayTime = timeStr)
            "evening" -> _state.value = _state.value.copy(eveningTime = timeStr)
        }
    }

    fun onMaxRemindersChange(value: String) {
        _state.value = _state.value.copy(maxReminders = value.filter { it.isDigit() })
    }

    fun onDurationMinutesChange(value: String) {
        _state.value = _state.value.copy(durationMinutes = value.filter { it.isDigit() })
    }

    fun save() {
        val s = _state.value
        if (!s.isValid) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                repository.saveConfig(
                    ReminderConfigDto(
                        morningTime = s.morningTime,
                        dayTime = s.dayTime,
                        eveningTime = s.eveningTime,
                        maxReminders = s.maxRemindersInt,
                        durationMinutes = s.durationMinutesInt
                    )
                )
                _state.value = _state.value.copy(isLoading = false, isSaved = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Save failed")
            }
        }
    }
}
