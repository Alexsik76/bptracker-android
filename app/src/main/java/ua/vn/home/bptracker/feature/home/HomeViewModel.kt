package ua.vn.home.bptracker.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.bp.BpZone
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.dto.MeasurementDto
import java.time.OffsetDateTime

sealed interface HomeState {
    data object Loading : HomeState
    data object Empty : HomeState
    data class Error(val message: String) : HomeState
    data class Content(val latest: MeasurementDto, val zone: BpZone) : HomeState
}

class HomeViewModel : ViewModel() {

    private val repository = ServiceLocator.measurementRepository
    private val _state = MutableStateFlow<HomeState>(HomeState.Loading)
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = HomeState.Loading
            try {
                val list = repository.getMeasurements(days = 7)
                if (list.isEmpty()) {
                    _state.value = HomeState.Empty
                } else {
                    val latest = list.maxBy { OffsetDateTime.parse(it.recordedAt) }
                    val zone = BpZone.classify(latest.sys, latest.dia)
                    _state.value = HomeState.Content(latest, zone)
                }
            } catch (e: Exception) {
                _state.value = HomeState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
