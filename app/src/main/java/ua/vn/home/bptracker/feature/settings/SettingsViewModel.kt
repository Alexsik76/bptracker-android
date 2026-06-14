package ua.vn.home.bptracker.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.config.AppLanguage
import ua.vn.home.bptracker.core.config.AppTheme
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.feature.reminders.ReminderScheduler

data class SettingsState(
    val theme: AppTheme = AppTheme.AUTO,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val ocrImprovement: Boolean = true,
    val version: String = "1.0.0",
    val remindersActive: Boolean? = null, // null if no template
    val templateId: String? = null
)

class SettingsViewModel : ViewModel() {
    private val settingsStore = ServiceLocator.settingsStore
    private val reminderRepository = ServiceLocator.reminderRepository

    private val _templateState = MutableStateFlow<Pair<String?, Boolean?>>(null to null)

    val state: StateFlow<SettingsState> = combine(
        settingsStore.theme,
        settingsStore.language,
        settingsStore.ocrImprovement,
        _templateState
    ) { theme, lang, ocr, template ->
        SettingsState(theme, lang, ocr, remindersActive = template.second, templateId = template.first)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                val t = reminderRepository.getActiveTemplate()
                // Explicitly set template ID first to ensure switch becomes enabled
                _templateState.value = (t?.id) to (t?.isActive)
            } catch (e: Exception) {
                // Keep current state
            }
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
        val currentId = state.value.templateId ?: return
        viewModelScope.launch {
            try {
                // Update state immediately for better UI response
                _templateState.value = currentId to enabled
                
                reminderRepository.updateTemplate(
                    currentId,
                    ua.vn.home.bptracker.data.dto.UpdateTemplateRequest(isActive = enabled)
                )

                val scheduler = ReminderScheduler(ServiceLocator.applicationContext)
                if (enabled) {
                    scheduler.rescheduleAll()
                } else {
                    scheduler.cancelAllReminders()
                }
            } catch (e: Exception) {
                // Rollback state on error
                refresh()
            }
        }
    }
}
