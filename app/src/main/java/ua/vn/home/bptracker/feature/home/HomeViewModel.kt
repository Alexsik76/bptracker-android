package ua.vn.home.bptracker.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.bp.BpZone
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.core.ui.ListUiState
import ua.vn.home.bptracker.core.utils.TimeUtils
import ua.vn.home.bptracker.data.dto.MeasurementDto
import java.time.OffsetDateTime

data class HomePayload(
    val latest: MeasurementDto,
    val zone: BpZone,
    val recent: List<MeasurementDto>,
    val avgSys: Int,
    val avgDia: Int,
    val avgPulse: Int,
    val inRangePercent: Int,
    val sysChange: Int,
    val diaChange: Int
)

class HomeViewModel : ViewModel() {

    private val repository = ServiceLocator.measurementRepository
    
    private val _refreshError = MutableStateFlow<String?>(null)

    val state: StateFlow<ListUiState<HomePayload>> = repository.observeMeasurements()
        .map { list -> computeHomeState(list) }
        .combine(_refreshError) { uiState, error ->
            if (error != null && uiState is ListUiState.Empty) {
                ListUiState.Error(error)
            } else {
                uiState
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ListUiState.Empty
        )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                _refreshError.value = null
                repository.syncPending()
                repository.getMeasurements(days = 14)
            } catch (e: Exception) {
                _refreshError.value = e.message ?: "Unknown error"
            }
        }
    }

    companion object {
        internal fun computeHomeState(measurements: List<MeasurementDto>): ListUiState<HomePayload> {
            if (measurements.isEmpty()) {
                return ListUiState.Empty
            }

            val allList = measurements.sortedByDescending { TimeUtils.parseToLocal(it.recordedAt) }
            
            val now = OffsetDateTime.now()
            val thisWeek = allList.filter { 
                TimeUtils.parseToLocal(it.recordedAt).isAfter(now.minusDays(7)) 
            }
            val lastWeek = allList.filter {
                val dt = TimeUtils.parseToLocal(it.recordedAt)
                dt.isAfter(now.minusDays(14)) && dt.isBefore(now.minusDays(7))
            }

            if (thisWeek.isEmpty()) {
                val latest = allList.first()
                return ListUiState.Content(
                    HomePayload(
                        latest = latest,
                        zone = BpZone.classify(latest.sys, latest.dia),
                        recent = allList.take(50),
                        avgSys = 0, avgDia = 0, avgPulse = 0, inRangePercent = 0, sysChange = 0, diaChange = 0
                    )
                )
            }

            val latest = thisWeek.first()
            val zone = BpZone.classify(latest.sys, latest.dia)

            val avgSys = thisWeek.map { it.sys }.average().toInt()
            val avgDia = thisWeek.map { it.dia }.average().toInt()
            val avgPulse = thisWeek.map { it.pulse }.average().toInt()

            val inRangeCount = thisWeek.count { 
                val z = BpZone.classify(it.sys, it.dia)
                z == BpZone.OPTIMAL || z == BpZone.NORMAL 
            }
            val inRangePercent = (inRangeCount.toFloat() / thisWeek.size * 100).toInt()

            var sysChange = 0
            var diaChange = 0
            if (lastWeek.isNotEmpty()) {
                val prevAvgSys = lastWeek.map { it.sys }.average().toInt()
                val prevAvgDia = lastWeek.map { it.dia }.average().toInt()
                sysChange = avgSys - prevAvgSys
                diaChange = avgDia - prevAvgDia
            }

            return ListUiState.Content(
                HomePayload(
                    latest = latest,
                    zone = zone,
                    recent = allList.take(50),
                    avgSys = avgSys,
                    avgDia = avgDia,
                    avgPulse = avgPulse,
                    inRangePercent = inRangePercent,
                    sysChange = sysChange,
                    diaChange = diaChange
                )
            )
        }
    }
}
