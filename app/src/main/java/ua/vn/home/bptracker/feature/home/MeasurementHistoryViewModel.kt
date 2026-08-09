package ua.vn.home.bptracker.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val isRefreshing: Boolean = false
)

class MeasurementHistoryViewModel : ViewModel() {

    private val repository = ServiceLocator.measurementRepository
    private val prescriptionRepo = ServiceLocator.prescriptionRepository

    private val _period = MutableStateFlow(MeasurementPeriod.MONTH)
    private val _offset = MutableStateFlow(0)
    private val _fetchedCount = MutableStateFlow(0)
    private val _isLoadingMore = MutableStateFlow(false)
    private val _isRefreshing = MutableStateFlow(false)
    private val _totalCount = MutableStateFlow(0)
    private val _error = MutableStateFlow<String?>(null)

    private val _prescriptionStartDate = prescriptionRepo.getPrescriptions()
        .map { list ->
            list.filter { it.isActive }
                .maxByOrNull { it.prescribedOn }
                ?.let { TimeUtils.parseToLocal(it.prescribedOn) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _internalState = combine(
        _period,
        _isLoadingMore,
        _totalCount,
        _error,
        _isRefreshing
    ) { period, loadingMore, total, error, refreshing ->
        InternalState(period, loadingMore, total, error, refreshing)
    }

    private data class InternalState(
        val period: MeasurementPeriod,
        val loadingMore: Boolean,
        val total: Int,
        val error: String?,
        val refreshing: Boolean
    )

    val state: StateFlow<ListUiState<HistoryState>> = combine(
        repository.observeMeasurements(),
        _internalState,
        _prescriptionStartDate,
        _fetchedCount
    ) { measurements, internal, prescriptionStart, fetchedCount ->
        val filtered = filterByPeriod(measurements, internal.period, prescriptionStart)
        val historyState = HistoryState(
            measurements = filtered,
            period = internal.period,
            isLoadingMore = internal.loadingMore,
            hasMore = fetchedCount < internal.total,
            totalCount = internal.total,
            error = internal.error,
            isRefreshing = internal.refreshing
        )
        
        if (internal.error != null && filtered.isEmpty()) {
            ListUiState.Error(internal.error)
        } else if (filtered.isEmpty() && !internal.refreshing) {
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
        load(reset = true)
    }

    fun setPeriod(period: MeasurementPeriod) {
        if (_period.value == period) return
        _period.value = period
        load(reset = true)
    }

    fun refresh() {
        load(reset = true)
    }

    fun loadMore() {
        val currentState = (state.value as? ListUiState.Content)?.data ?: return
        if (currentState.isLoadingMore || !currentState.hasMore) return
        load(reset = false)
    }

    private fun load(reset: Boolean) {
        viewModelScope.launch {
            try {
                if (reset) {
                    _offset.value = 0
                    _fetchedCount.value = 0
                    _isRefreshing.value = true
                } else {
                    _isLoadingMore.value = true
                }
                _error.value = null

                val prescriptionStart = _prescriptionStartDate.value
                val dateFrom = _period.value.getDateFrom(prescriptionStart)
                
                val page = repository.loadPage(
                    dateFrom = dateFrom,
                    dateTo = null,
                    offset = _offset.value
                )

                _totalCount.value = page.total
                if (reset) {
                    _offset.value = page.items.size
                    _fetchedCount.value = page.items.size
                } else {
                    _offset.value += page.items.size
                    _fetchedCount.value += page.items.size
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load history"
            } finally {
                _isRefreshing.value = false
                _isLoadingMore.value = false
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
