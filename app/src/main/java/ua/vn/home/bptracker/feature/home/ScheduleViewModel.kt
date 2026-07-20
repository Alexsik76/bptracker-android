package ua.vn.home.bptracker.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.core.ui.ListUiState
import ua.vn.home.bptracker.data.dto.WhenSlot
import ua.vn.home.bptracker.feature.reminders.TodaySchedule
import java.time.LocalDate

class ScheduleViewModel : ViewModel() {
    private val today = LocalDate.now().toString()
    private val useCase = ServiceLocator.todayScheduleUseCase
    private val intakeRepo = ServiceLocator.intakeReportRepository
    private val prescriptionRepo = ServiceLocator.prescriptionRepository

    private val _refreshing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val state: StateFlow<ListUiState<TodaySchedule>> = combine(
        useCase.observeToday(today),
        _refreshing,
        _error
    ) { schedule, refreshing, error ->
        when {
            error != null && schedule.slots.isEmpty() -> ListUiState.Error(error)
            else -> ListUiState.Content(schedule, refreshing)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ListUiState.Idle
    )

    fun refresh(isManual: Boolean = false) {
        viewModelScope.launch {
            if (isManual) _refreshing.value = true
            _error.value = null
            try {
                intakeRepo.refresh()
                intakeRepo.syncPending()
                prescriptionRepo.refresh()
            } catch (e: Exception) {
                _error.value = e.message ?: "Refresh failed"
            } finally {
                _refreshing.value = false
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
