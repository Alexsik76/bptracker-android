package ua.vn.home.bptracker.core.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.authDataStore by preferencesDataStore(name = "auth")
private val OLD_TOKEN_KEY = stringPreferencesKey("device_token")
private val ACCESS_KEY = stringPreferencesKey("access_token")
private val REFRESH_KEY = stringPreferencesKey("refresh_token")

class TokenStore(private val context: Context) {

    @Volatile
    var cachedAccessToken: String? = null
        private set

    @Volatile
    var cachedRefreshToken: String? = null
        private set

    suspend fun load() {
        val prefs = context.authDataStore.data.first()
        
        // Remove old invalid token if present
        if (prefs.contains(OLD_TOKEN_KEY)) {
            context.authDataStore.edit { it.remove(OLD_TOKEN_KEY) }
        }

        cachedAccessToken = prefs[ACCESS_KEY]
        cachedRefreshToken = prefs[REFRESH_KEY]
    }

    suspend fun save(access: String, refresh: String) {
        context.authDataStore.edit {
            it[ACCESS_KEY] = access
            it[REFRESH_KEY] = refresh
        }
        cachedAccessToken = access
        cachedRefreshToken = refresh
    }

    suspend fun clear() {
        context.authDataStore.edit {
            it.remove(ACCESS_KEY)
            it.remove(REFRESH_KEY)
        }
        cachedAccessToken = null
        cachedRefreshToken = null
    }
}
