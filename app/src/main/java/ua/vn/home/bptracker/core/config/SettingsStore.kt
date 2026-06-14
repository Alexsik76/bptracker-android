package ua.vn.home.bptracker.core.config

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

enum class AppTheme { AUTO, LIGHT, DARK }
enum class AppLanguage { SYSTEM, UA, EN }

class SettingsStore(private val context: Context) {
    private val themeKey = stringPreferencesKey("app_theme")
    private val langKey = stringPreferencesKey("app_lang")
    private val ocrImproveKey = booleanPreferencesKey("ocr_improvement")

    val theme: Flow<AppTheme> = context.settingsDataStore.data.map { prefs ->
        try {
            AppTheme.valueOf(prefs[themeKey] ?: AppTheme.AUTO.name)
        } catch (e: Exception) {
            AppTheme.AUTO
        }
    }

    val language: Flow<AppLanguage> = context.settingsDataStore.data.map { prefs ->
        try {
            AppLanguage.valueOf(prefs[langKey] ?: AppLanguage.SYSTEM.name)
        } catch (e: Exception) {
            AppLanguage.SYSTEM
        }
    }

    val ocrImprovement: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[ocrImproveKey] ?: true
    }

    suspend fun setTheme(theme: AppTheme) {
        context.settingsDataStore.edit { it[themeKey] = theme.name }
    }

    suspend fun setLanguage(lang: AppLanguage) {
        context.settingsDataStore.edit { it[langKey] = lang.name }
    }

    suspend fun setOcrImprovement(enabled: Boolean) {
        context.settingsDataStore.edit { it[ocrImproveKey] = enabled }
    }
}
