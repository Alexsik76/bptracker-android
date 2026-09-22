package ua.vn.home.bptracker.feature.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.BuildConfig
import ua.vn.home.bptracker.core.config.AppLanguage
import ua.vn.home.bptracker.core.config.AppTheme
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.repository.ExportResult
import ua.vn.home.bptracker.feature.reminders.ReminderHealth
import java.time.LocalDate
import java.time.ZoneId

data class SettingsState(
    val theme: AppTheme = AppTheme.AUTO,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val ocrImprovement: Boolean = true,
    val version: String = BuildConfig.VERSION_NAME,
    val remindersActive: Boolean? = null,
    val templateId: String? = null,
    val healthConnectAvailable: Boolean = false,
    val healthConnectEnabled: Boolean = false
)

enum class ExportPeriod(val months: Long) { ONE(1), THREE(3), SIX(6), ALL(-1) }

class SettingsViewModel : ViewModel() {
    private val settingsStore = ServiceLocator.settingsStore
    private val exportRepository = ServiceLocator.exportRepository
    private val healthConnectManager = ServiceLocator.healthConnectManager
    private val measurementRepository = ServiceLocator.measurementRepository

    private val _templateState = MutableStateFlow<Pair<String?, Boolean?>>(null to false)
    private val _exportOperation = MutableStateFlow<ExportResult?>(null)
    val exportOperation: StateFlow<ExportResult?> = _exportOperation.asStateFlow()

    private val _reminderHealth = MutableStateFlow<ReminderHealth?>(null)
    val reminderHealth: StateFlow<ReminderHealth?> = _reminderHealth.asStateFlow()

    private val _exportPeriod = MutableStateFlow(ExportPeriod.THREE)
    val exportPeriod: StateFlow<ExportPeriod> = _exportPeriod.asStateFlow()

    val reminderScheduleFailed: StateFlow<Boolean> = settingsStore.reminderScheduleFailed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val state: StateFlow<SettingsState> = combine(
        settingsStore.theme,
        settingsStore.language,
        settingsStore.ocrImprovement,
        settingsStore.remindersEnabled,
        settingsStore.healthConnectEnabled,
        _templateState
    ) { flows ->
        val theme = flows[0] as AppTheme
        val lang = flows[1] as AppLanguage
        val ocr = flows[2] as Boolean
        val reminders = flows[3] as Boolean
        val hcEnabled = flows[4] as Boolean
        @Suppress("UNCHECKED_CAST")
        val template = flows[5] as Pair<String?, Boolean?>
        SettingsState(
            theme = theme,
            language = lang,
            ocrImprovement = ocr,
            version = BuildConfig.VERSION_NAME,
            remindersActive = reminders,
            templateId = template.first,
            healthConnectAvailable = healthConnectManager.isAvailable(),
            healthConnectEnabled = hcEnabled
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val resolved = ServiceLocator.reminderConfigRepository.resolveConfig()
            _templateState.value = (if (resolved.config != null) "active" else null) to state.value.remindersActive
            _reminderHealth.value = ServiceLocator.reminderScheduler.checkHealth()

            if (settingsStore.healthConnectEnabled.first() && !healthConnectManager.hasPermissions()) {
                settingsStore.setHealthConnectEnabled(false)
            }
        }
    }

    fun checkAndRequestHealthConnectPermissions(onResult: (needsRequest: Boolean) -> Unit) {
        viewModelScope.launch {
            val hasPermissions = healthConnectManager.hasPermissions()
            onResult(!hasPermissions)
        }
    }

    fun setHealthConnectEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                if (healthConnectManager.hasPermissions()) {
                    settingsStore.setHealthConnectEnabled(true)
                    measurementRepository.enqueueAllSyncedForExport()
                } else {
                    settingsStore.setHealthConnectEnabled(false)
                }
            } else {
                settingsStore.setHealthConnectEnabled(false)
            }
        }
    }

    fun repairReminders() {
        viewModelScope.launch {
            ServiceLocator.reminderScheduler.rescheduleAll()
            _reminderHealth.value = ServiceLocator.reminderScheduler.checkHealth()
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
                Log.i("ReminderDiag", "call=SettingsViewModel.setRemindersEnabled thread=${Thread.currentThread().name}")
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
                val today = LocalDate.now()
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
