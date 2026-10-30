package ua.vn.home.bptracker.core.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.authDataStore by preferencesDataStore(name = "auth")
private val OLD_TOKEN_KEY = stringPreferencesKey("device_token")
private val ACCESS_KEY = stringPreferencesKey("access_token")
private val REFRESH_KEY = stringPreferencesKey("refresh_token")
private val EMAIL_KEY = stringPreferencesKey("cached_email")

open class TokenStore(private val context: Context? = null) {

    private val loadMutex = Mutex()

    @Volatile
    private var isLoaded: Boolean = false

    @Volatile
    open var cachedAccessToken: String? = null
        protected set

    @Volatile
    open var cachedRefreshToken: String? = null
        protected set

    @Volatile
    open var cachedEmail: String? = null
        protected set

    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    open val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    open fun notifySessionExpired() {
        _sessionExpired.tryEmit(Unit)
    }

    open suspend fun ensureLoaded() {
        if (isLoaded) return
        loadMutex.withLock {
            if (!isLoaded) {
                context?.let { ctx ->
                    val prefs = ctx.authDataStore.data.first()
                    // Remove old invalid token if present
                    if (prefs.contains(OLD_TOKEN_KEY)) {
                        ctx.authDataStore.edit { it.remove(OLD_TOKEN_KEY) }
                    }
                    cachedAccessToken = prefs[ACCESS_KEY]
                    cachedRefreshToken = prefs[REFRESH_KEY]
                    cachedEmail = prefs[EMAIL_KEY]
                }
                isLoaded = true
            }
        }
    }

    open fun ensureLoadedBlocking() {
        if (isLoaded) return
        runBlocking {
            ensureLoaded()
        }
    }

    open suspend fun load() = ensureLoaded()

    open suspend fun getAccessToken(): String? {
        ensureLoaded()
        return cachedAccessToken
    }

    open suspend fun getRefreshToken(): String? {
        ensureLoaded()
        return cachedRefreshToken
    }

    open suspend fun getEmail(): String? {
        ensureLoaded()
        return cachedEmail
    }

    open suspend fun save(access: String, refresh: String, email: String? = null) {
        context?.let { ctx ->
            ctx.authDataStore.edit {
                it[ACCESS_KEY] = access
                it[REFRESH_KEY] = refresh
                if (email != null) {
                    it[EMAIL_KEY] = email
                }
            }
        }
        cachedAccessToken = access
        cachedRefreshToken = refresh
        if (email != null) {
            cachedEmail = email
        }
        isLoaded = true
    }

    open suspend fun saveEmail(email: String) {
        context?.let { ctx ->
            ctx.authDataStore.edit {
                it[EMAIL_KEY] = email
            }
        }
        cachedEmail = email
    }

    open suspend fun clear() {
        context?.let { ctx ->
            ctx.authDataStore.edit {
                it.remove(ACCESS_KEY)
                it.remove(REFRESH_KEY)
                it.remove(EMAIL_KEY)
            }
        }
        cachedAccessToken = null
        cachedRefreshToken = null
        cachedEmail = null
        isLoaded = true
    }
}
