package ua.vn.home.bptracker.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.BuildConfig
import ua.vn.home.bptracker.core.config.AppLanguage
import ua.vn.home.bptracker.core.config.AppTheme
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.repository.ExportResult
import java.time.ZoneId

data class SettingsState(
    val theme: AppTheme = AppTheme.AUTO,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val ocrImprovement: Boolean = true,
    val version: String = BuildConfig.VERSION_NAME,
    val remindersActive: Boolean? = null,
    val templateId: String? = null
)

enum class ExportPeriod(val months: Long) { ONE(1), THREE(3), SIX(6), ALL(-1) }

class SettingsViewModel : ViewModel() {
    private val settingsStore = ServiceLocator.settingsStore
    private val exportRepository = ServiceLocator.exportRepository

    private val _templateState = MutableStateFlow<Pair<String?, Boolean?>>(null to false)
    private val _exportOperation = MutableStateFlow<ExportResult?>(null)
    val exportOperation: StateFlow<ExportResult?> = _exportOperation.asStateFlow()

    private val _exportPeriod = MutableStateFlow(ExportPeriod.THREE)
    val exportPeriod: StateFlow<ExportPeriod> = _exportPeriod.asStateFlow()

    val state: StateFlow<SettingsState> = combine(
        settingsStore.theme,
        settingsStore.language,
        settingsStore.ocrImprovement,
        settingsStore.remindersEnabled,
        _templateState
    ) { theme, lang, ocr, reminders, template ->
        SettingsState(
            theme = theme,
            language = lang,
            ocrImprovement = ocr,
            version = BuildConfig.VERSION_NAME,
            remindersActive = reminders,
            templateId = template.first
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val config = ServiceLocator.reminderConfigRepository.getCachedConfig()
            _templateState.value = (if (config != null) "active" else null) to state.value.remindersActive
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { settingsStore.setTheme(theme) }
    }

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch { settingsStore.setLanguage(lang) }
    }

    fun setOcrImprovement(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setOcrImprovement(enabled) }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setRemindersEnabled(enabled)
            if (enabled) {
                ServiceLocator.notificationHelper.createNotificationChannel()
                ServiceLocator.reminderScheduler.rescheduleAll()
            } else {
                ServiceLocator.reminderScheduler.cancelAllReminders()
            }
        }
    }

    fun setExportPeriod(period: ExportPeriod) {
        _exportPeriod.value = period
    }

    fun exportCsv() {
        viewModelScope.launch {
            _exportOperation.value = null // reset
            val period = _exportPeriod.value
            val (dateFrom, dateTo) = if (period == ExportPeriod.ALL) {
                null to null
            } else {
                val today = java.time.LocalDate.now()
                val from = today.minusMonths(period.months)
                from.toString() to today.toString()
            }

            val result = exportRepository.exportCsv(
                timezoneId = ZoneId.systemDefault().id,
                dateFrom = dateFrom,
                dateTo = dateTo
            )
            _exportOperation.value = result
        }
    }

    fun consumeExportResult() {
        _exportOperation.value = null
    }
}
