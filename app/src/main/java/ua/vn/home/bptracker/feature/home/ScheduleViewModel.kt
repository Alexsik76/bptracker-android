package ua.vn.home.bptracker.feature.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ua.vn.home.bptracker.data.dto.TodayIntake

sealed interface ScheduleState {
    data object Loading : ScheduleState
    data object Empty : ScheduleState
    data class Error(val message: String) : ScheduleState
    data class Content(val intakes: List<TodayIntake>) : ScheduleState
    data object ComingSoon : ScheduleState
}

class ScheduleViewModel : ViewModel() {
    private val _state = MutableStateFlow<ScheduleState>(ScheduleState.ComingSoon)
    val state: StateFlow<ScheduleState> = _state.asStateFlow()

    fun refresh() {
        // No-op until Part 2
    }

    fun confirm(period: String) {
        // No-op until Part 2
    }
}
