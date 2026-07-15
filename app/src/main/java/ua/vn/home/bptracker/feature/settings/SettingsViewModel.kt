package ua.vn.home.bptracker.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.BuildConfig
import ua.vn.home.bptracker.core.config.AppLanguage
import ua.vn.home.bptracker.core.config.AppTheme
import ua.vn.home.bptracker.core.di.ServiceLocator

data class SettingsState(
    val theme: AppTheme = AppTheme.AUTO,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val ocrImprovement: Boolean = true,
    val version: String = BuildConfig.VERSION_NAME,
    val remindersActive: Boolean? = null,
    val templateId: String? = null
)

class SettingsViewModel : ViewModel() {
    private val settingsStore = ServiceLocator.settingsStore

    private val _templateState = MutableStateFlow<Pair<String?, Boolean?>>(null to false)

    val state: StateFlow<SettingsState> = combine(
        settingsStore.theme,
        settingsStore.language,
        settingsStore.ocrImprovement,
        _templateState
    ) { theme, lang, ocr, template ->
        SettingsState(
            theme = theme,
            language = lang,
            ocrImprovement = ocr,
            version = BuildConfig.VERSION_NAME,
            remindersActive = template.second,
            templateId = template.first
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    init {
        refresh()
    }

    fun refresh() {
        // No-op for reminders until Part 2
        _templateState.value = null to false
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
        // No-op until Part 2
    }
}
