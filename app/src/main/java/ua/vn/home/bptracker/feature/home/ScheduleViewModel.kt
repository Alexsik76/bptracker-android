package ua.vn.home.bptracker.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.dto.TodayIntake
import java.util.TimeZone

sealed interface ScheduleState {
    data object Loading : ScheduleState
    data object Empty : ScheduleState
    data class Error(val message: String) : ScheduleState
    data class Content(val intakes: List<TodayIntake>) : ScheduleState
}

class ScheduleViewModel : ViewModel() {
    private val repository = ServiceLocator.reminderRepository
    private val _state = MutableStateFlow<ScheduleState>(ScheduleState.Loading)
    val state: StateFlow<ScheduleState> = _state.asStateFlow()

    private val timezone = TimeZone.getDefault().id

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = ScheduleState.Loading
            try {
                val todayMeds = repository.getToday(timezone)
                if (todayMeds.intakes.isEmpty()) {
                    _state.value = ScheduleState.Empty
                } else {
                    _state.value = ScheduleState.Content(todayMeds.intakes)
                }
            } catch (e: Exception) {
                _state.value = ScheduleState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun confirm(period: String) {
        viewModelScope.launch {
            try {
                repository.confirm(period, timezone)
                refresh() // Refresh after confirmation
            } catch (e: Exception) {
                // Optionally handle confirmation error, e.g., show a snackbar
            }
        }
    }
}
