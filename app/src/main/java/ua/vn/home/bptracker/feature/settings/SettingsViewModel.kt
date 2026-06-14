package ua.vn.home.bptracker.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.config.AppLanguage
import ua.vn.home.bptracker.core.config.AppTheme
import ua.vn.home.bptracker.core.di.ServiceLocator

data class SettingsState(
    val theme: AppTheme = AppTheme.AUTO,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val ocrImprovement: Boolean = true,
    val version: String = "1.0.0"
)

class SettingsViewModel : ViewModel() {
    private val settingsStore = ServiceLocator.settingsStore

    val state: StateFlow<SettingsState> = combine(
        settingsStore.theme,
        settingsStore.language,
        settingsStore.ocrImprovement
    ) { theme, lang, ocr ->
        SettingsState(theme, lang, ocr)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { settingsStore.setTheme(theme) }
    }

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch { settingsStore.setLanguage(lang) }
    }

    fun setOcrImprovement(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setOcrImprovement(enabled) }
    }
}
