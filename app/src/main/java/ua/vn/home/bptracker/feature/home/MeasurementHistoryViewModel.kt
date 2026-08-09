package ua.vn.home.bptracker.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.core.ui.ListUiState
import ua.vn.home.bptracker.core.utils.TimeUtils
import ua.vn.home.bptracker.data.dto.MeasurementDto
import java.time.OffsetDateTime

data class HistoryState(
    val measurements: List<MeasurementDto> = emptyList(),
    val period: MeasurementPeriod = MeasurementPeriod.MONTH,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val totalCount: Int = 0,
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val isFilling: Boolean = false
)

class MeasurementHistoryViewModel : ViewModel() {

    private val repository = ServiceLocator.measurementRepository
    private val prescriptionRepo = ServiceLocator.prescriptionRepository

    private val _period = MutableStateFlow(MeasurementPeriod.MONTH)
    private val _isRefreshing = MutableStateFlow(false)
    private val _isFilling = MutableStateFlow(false)
    private val _totalCount = MutableStateFlow(0)
    private val _error = MutableStateFlow<String?>(null)

    private var fillJob: Job? = null
    private var fetchedForCurrentPeriod = 0

    private val _prescriptionStartDate = prescriptionRepo.getPrescriptions()
        .map { list ->
            list.filter { it.isActive }
                .maxByOrNull { it.prescribedOn }
                ?.let { TimeUtils.parseToLocal(it.prescribedOn) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _internalState = combine(
        _period,
        _totalCount,
        _error,
        _isRefreshing,
        _isFilling
    ) { period, total, error, refreshing, filling ->
        InternalState(period, total, error, refreshing, filling)
    }

    private data class InternalState(
        val period: MeasurementPeriod,
        val total: Int,
        val error: String?,
        val refreshing: Boolean,
        val filling: Boolean
    )

    val state: StateFlow<ListUiState<HistoryState>> = combine(
        repository.observeMeasurements(),
        _internalState,
        _prescriptionStartDate
    ) { measurements, internal, prescriptionStart ->
        val filtered = filterByPeriod(measurements, internal.period, prescriptionStart)
        val historyState = HistoryState(
            measurements = filtered,
            period = internal.period,
            isLoadingMore = false, // Paging removed
            hasMore = filtered.size < internal.total,
            totalCount = internal.total,
            error = internal.error,
            isRefreshing = internal.refreshing,
            isFilling = internal.filling
        )
        
        if (internal.error != null && filtered.isEmpty()) {
            ListUiState.Error(internal.error)
        } else if (filtered.isEmpty() && !internal.refreshing && !internal.filling) {
            ListUiState.Empty
        } else {
            ListUiState.Content(historyState, isRefreshing = internal.refreshing)
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ListUiState.Content(HistoryState(), isRefreshing = true)
    )

    init {
        startFillLoop()
    }

    fun setPeriod(period: MeasurementPeriod) {
        if (_period.value == period) return
        _period.value = period
        startFillLoop()
    }

    fun refresh() {
        fillJob?.cancel()
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                _error.value = null
                val prescriptionStart = _prescriptionStartDate.value
                val dateFrom = _period.value.getDateFrom(prescriptionStart)
                repository.reconcilePeriod(dateFrom, null)
                _totalCount.value = 0 
            } catch (e: Exception) {
                _error.value = e.message ?: "Reconciliation failed"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun startFillLoop() {
        fillJob?.cancel()
        fetchedForCurrentPeriod = 0
        fillJob = viewModelScope.launch {
            try {
                _isFilling.value = true
                _error.value = null
                _totalCount.value = Int.MAX_VALUE 

                val prescriptionStart = _prescriptionStartDate.value
                val dateFrom = _period.value.getDateFrom(prescriptionStart)

                while (fetchedForCurrentPeriod < _totalCount.value) {
                    val page = repository.loadPage(
                        dateFrom = dateFrom,
                        dateTo = null,
                        offset = fetchedForCurrentPeriod
                    )
                    _totalCount.value = page.total
                    fetchedForCurrentPeriod += page.items.size
                    if (page.items.isEmpty()) break
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Background sync failed"
            } finally {
                _isFilling.value = false
            }
        }
    }

    private fun filterByPeriod(
        list: List<MeasurementDto>,
        period: MeasurementPeriod,
        prescriptionStart: OffsetDateTime?
    ): List<MeasurementDto> {
        val dateFrom = period.getDateFrom(prescriptionStart) ?: return list
        return list.filter { 
            !TimeUtils.parseToLocal(it.recordedAt).isBefore(dateFrom) 
        }.sortedByDescending { it.recordedAt }
    }
}
