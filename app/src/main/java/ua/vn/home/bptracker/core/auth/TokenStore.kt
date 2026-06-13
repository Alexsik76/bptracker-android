package ua.vn.home.bptracker.core.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.authDataStore by preferencesDataStore(name = "auth")
private val TOKEN_KEY = stringPreferencesKey("device_token")

class TokenStore(private val context: Context) {

    @Volatile
    var cachedToken: String? = null
        private set

    /** Load the persisted token into the in-memory cache (call once at startup). */
    suspend fun load() {
        cachedToken = context.authDataStore.data.first()[TOKEN_KEY]
    }

    suspend fun save(token: String) {
        context.authDataStore.edit { it[TOKEN_KEY] = token }
        cachedToken = token
    }

    suspend fun clear() {
        context.authDataStore.edit { it.remove(TOKEN_KEY) }
        cachedToken = null
    }
}
