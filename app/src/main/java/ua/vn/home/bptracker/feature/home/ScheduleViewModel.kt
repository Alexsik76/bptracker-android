package ua.vn.home.bptracker.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.dto.WhenSlot
import ua.vn.home.bptracker.feature.reminders.TodaySchedule
import java.time.LocalDate

sealed interface ScheduleState {
    data object Loading : ScheduleState
    data object NotConfigured : ScheduleState
    data class Content(val schedule: TodaySchedule) : ScheduleState
    data class Error(val message: String) : ScheduleState
}

class ScheduleViewModel : ViewModel() {
    private val today = LocalDate.now().toString()
    private val useCase = ServiceLocator.todayScheduleUseCase
    private val intakeRepo = ServiceLocator.intakeReportRepository
    private val prescriptionRepo = ServiceLocator.prescriptionRepository

    private val _state = MutableStateFlow<ScheduleState>(ScheduleState.Loading)
    val state: StateFlow<ScheduleState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            useCase.observeToday(today).collect { schedule ->
                _state.value = if (!schedule.configured) {
                    ScheduleState.NotConfigured
                } else {
                    ScheduleState.Content(schedule)
                }
            }
        }
    }

    fun refresh() {
        if (_state.value !is ScheduleState.Content) {
            _state.value = ScheduleState.Loading
        }
        viewModelScope.launch {
            try {
                intakeRepo.refresh()
                prescriptionRepo.refresh()
            } catch (e: Exception) {
                if (_state.value !is ScheduleState.Content) {
                    _state.value = ScheduleState.Error(e.message ?: "Refresh failed")
                }
            }
        }
    }

    fun confirmSlot(slot: WhenSlot) {
        viewModelScope.launch {
            intakeRepo.confirm(slot, today, takenAt = null)
        }
    }

    fun editTime(slot: WhenSlot, takenAtIso: String) {
        viewModelScope.launch {
            intakeRepo.confirm(slot, today, takenAt = takenAtIso)
        }
    }

    fun deleteIntake(slot: WhenSlot) {
        viewModelScope.launch {
            intakeRepo.delete(slot, today)
        }
    }
}
