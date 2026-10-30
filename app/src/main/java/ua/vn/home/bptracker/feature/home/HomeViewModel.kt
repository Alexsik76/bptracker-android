package ua.vn.home.bptracker.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.core.bp.BpZone
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.core.ui.ListUiState
import ua.vn.home.bptracker.core.utils.TimeUtils
import ua.vn.home.bptracker.data.dto.MeasurementDto
import ua.vn.home.bptracker.data.repository.MeasurementRepository
import ua.vn.home.bptracker.data.repository.SYNC_WINDOW_DAYS
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
) {
    companion object {
        val EMPTY = HomePayload(
            latest = MeasurementDto("", "", 0, 0, 0),
            zone = BpZone.NORMAL,
            recent = emptyList(),
            avgSys = 0, avgDia = 0, avgPulse = 0,
            inRangePercent = 0, sysChange = 0, diaChange = 0
        )
    }
}

class HomeViewModel(
    private val repository: MeasurementRepository = ServiceLocator.measurementRepository
) : ViewModel() {

    private fun getString(@androidx.annotation.StringRes resId: Int): String {
        return try {
            ServiceLocator.applicationContext.getString(resId)
        } catch (_: Exception) {
            "Failed to sync data"
        }
    }
    
    private val _refreshError = MutableStateFlow<String?>(null)
    private val _isRefreshing = MutableStateFlow(false)

    val state: StateFlow<ListUiState<HomePayload>> = repository.observeMeasurements()
        .map { list -> computeHomeState(list) }
        .flowOn(Dispatchers.Default)
        .combine(_refreshError) { uiState, error ->
            if (error != null && uiState is ListUiState.Empty) {
                ListUiState.Error(error)
            } else {
                uiState
            }
        }
        .combine(_isRefreshing) { uiState, refreshing ->
            if (uiState is ListUiState.Content) {
                uiState.copy(isRefreshing = refreshing)
            } else {
                uiState
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ListUiState.Content(HomePayload.EMPTY, isRefreshing = true)
        )

    init {
        refresh(isManual = false)
    }

    fun refresh(isManual: Boolean = false) {
        viewModelScope.launch {
            try {
                if (isManual) _isRefreshing.value = true
                _refreshError.value = null
                repository.syncPending()
                repository.syncRecent()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _refreshError.value = getString(R.string.error_sync_failed)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    companion object {
        internal fun computeHomeState(measurements: List<MeasurementDto>): ListUiState<HomePayload> {
            if (measurements.isEmpty()) {
                return ListUiState.Empty
            }

            val parsed = measurements.map { it to TimeUtils.parseToLocal(it.recordedAt) }
            val sorted = parsed.sortedByDescending { it.second }
            val allList = sorted.map { it.first }
            
            val now = OffsetDateTime.now()
            val weekAgo = now.minusDays(7)
            val twoWeeksAgo = now.minusDays(14)

            val thisWeekParsed = sorted.filter { it.second.isAfter(weekAgo) }
            val lastWeekParsed = sorted.filter { it.second.isAfter(twoWeeksAgo) && it.second.isBefore(weekAgo) }

            if (thisWeekParsed.isEmpty()) {
                val latest = allList.first()
                return ListUiState.Content(
                    HomePayload(
                        latest = latest,
                        zone = BpZone.classify(latest.sys, latest.dia),
                        recent = allList,
                        avgSys = 0, avgDia = 0, avgPulse = 0, inRangePercent = 0, sysChange = 0, diaChange = 0
                    )
                )
            }

            val latest = thisWeekParsed.first().first
            val zone = BpZone.classify(latest.sys, latest.dia)

            val thisWeek = thisWeekParsed.map { it.first }
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
            if (lastWeekParsed.isNotEmpty()) {
                val lastWeek = lastWeekParsed.map { it.first }
                val prevAvgSys = lastWeek.map { it.sys }.average().toInt()
                val prevAvgDia = lastWeek.map { it.dia }.average().toInt()
                sysChange = avgSys - prevAvgSys
                diaChange = avgDia - prevAvgDia
            }

            return ListUiState.Content(
                HomePayload(
                    latest = latest,
                    zone = zone,
                    recent = allList,
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
