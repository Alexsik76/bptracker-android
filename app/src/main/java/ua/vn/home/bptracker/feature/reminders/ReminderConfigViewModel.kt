package ua.vn.home.bptracker.feature.reminders

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.core.ui.OperationUiState
import ua.vn.home.bptracker.data.dto.ReminderConfigDto
import ua.vn.home.bptracker.data.repository.ReminderConfigRepository
import java.util.Locale

data class ReminderConfigState(
    val morningTime: String = "08:00:00",
    val dayTime: String = "14:00:00",
    val eveningTime: String = "20:00:00",
    val maxReminders: String = "3",
    val durationMinutes: String = "60",
    val isLoading: Boolean = false,
    val saveOperation: OperationUiState = OperationUiState.Idle
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

class ReminderConfigViewModel(
    private val repository: ReminderConfigRepository = ServiceLocator.reminderConfigRepository
) : ViewModel() {

    private fun getString(@androidx.annotation.StringRes resId: Int): String {
        return try {
            ServiceLocator.applicationContext.getString(resId)
        } catch (_: Exception) {
            "Error"
        }
    }

    private val _state = MutableStateFlow(ReminderConfigState())
    val state: StateFlow<ReminderConfigState> = _state.asStateFlow()

    init {
        loadConfig()
    }

    fun loadConfig() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, saveOperation = OperationUiState.Idle)
            try {
                var networkError: Exception? = null
                val config = try {
                    repository.getConfig()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    networkError = e
                    repository.resolveConfig().config
                }
                if (config != null) {
                    applyConfig(config)
                    _state.value = _state.value.copy(isLoading = false)
                } else if (networkError != null) {
                    throw networkError
                } else {
                    _state.value = _state.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.value = _state.value.copy(
                    isLoading = false,
                    saveOperation = OperationUiState.Error(getString(R.string.error_load_config_failed))
                )
            }
        }
    }

    private fun applyConfig(config: ReminderConfigDto) {
        _state.value = _state.value.copy(
            morningTime = config.morningTime,
            dayTime = config.dayTime,
            eveningTime = config.eveningTime,
            maxReminders = config.maxReminders.toString(),
            durationMinutes = config.durationMinutes.toString()
        )
    }

    fun onTimeChange(slot: String, hour: Int, minute: Int) {
        val timeStr = String.format(Locale.ROOT, "%02d:%02d:00", hour, minute)
        when (slot) {
            "morning" -> _state.value = _state.value.copy(morningTime = timeStr)
            "day" -> _state.value = _state.value.copy(dayTime = timeStr)
            "evening" -> _state.value = _state.value.copy(eveningTime = timeStr)
        }
        _state.value = _state.value.copy(saveOperation = OperationUiState.Idle)
    }

    fun onMaxRemindersChange(value: String) {
        _state.value = _state.value.copy(maxReminders = value.filter { it.isDigit() }, saveOperation = OperationUiState.Idle)
    }

    fun onDurationMinutesChange(value: String) {
        _state.value = _state.value.copy(durationMinutes = value.filter { it.isDigit() }, saveOperation = OperationUiState.Idle)
    }

    fun save() {
        val s = _state.value
        if (!s.isValid) return

        viewModelScope.launch {
            _state.value = _state.value.copy(saveOperation = OperationUiState.InProgress)
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
                if (ServiceLocator.settingsStore.remindersEnabled.first()) {
                    Log.i("ReminderDiag", "call=ReminderConfigViewModel thread=${Thread.currentThread().name}")
                    ServiceLocator.reminderScheduler.rescheduleAll()
                }
                _state.value = _state.value.copy(saveOperation = OperationUiState.Success)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.value = _state.value.copy(saveOperation = OperationUiState.Error(getString(R.string.error_save_failed)))
            }
        }
    }
}
