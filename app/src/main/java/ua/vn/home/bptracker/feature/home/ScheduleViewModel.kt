package ua.vn.home.bptracker.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.core.ui.ListUiState
import ua.vn.home.bptracker.data.dto.WhenSlot
import ua.vn.home.bptracker.feature.reminders.TodaySchedule
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModel : ViewModel() {
    private val currentDate = MutableStateFlow(LocalDate.now())
    private val useCase = ServiceLocator.todayScheduleUseCase
    private val confirmUseCase = ServiceLocator.confirmIntakeUseCase
    private val intakeRepo = ServiceLocator.intakeReportRepository
    private val prescriptionRepo = ServiceLocator.prescriptionRepository

    private val _refreshing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val state: StateFlow<ListUiState<TodaySchedule>> = combine(
        currentDate.flatMapLatest { date -> useCase.observeToday(date.toString()) },
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
        initialValue = ListUiState.Content(TodaySchedule.empty(LocalDate.now().toString()), isRefreshing = true)
    )

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(60_000L)
                currentDate.value = LocalDate.now()
            }
        }
    }

    fun refresh(isManual: Boolean = false) {
        currentDate.value = LocalDate.now()
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
            confirmUseCase(slot, currentDate.value, takenAt = null)
        }
    }

    fun editTime(slot: WhenSlot, takenAtIso: String) {
        viewModelScope.launch {
            confirmUseCase(slot, currentDate.value, takenAt = takenAtIso)
        }
    }

    fun deleteIntake(slot: WhenSlot) {
        viewModelScope.launch {
            intakeRepo.delete(slot, currentDate.value.toString())
        }
    }
}
