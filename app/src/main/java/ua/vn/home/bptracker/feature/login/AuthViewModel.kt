package ua.vn.home.bptracker.feature.login

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.credentials.*
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import ua.vn.home.bptracker.R
import ua.vn.home.bptracker.core.auth.TokenStore
import ua.vn.home.bptracker.core.config.MOCK_MODE
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.api.AuthApi
import ua.vn.home.bptracker.data.api.SessionApi
import ua.vn.home.bptracker.data.api.UserApi
import ua.vn.home.bptracker.data.dto.MagicLinkConfirmRequest
import ua.vn.home.bptracker.data.dto.MagicLinkRequest
import ua.vn.home.bptracker.data.dto.RefreshRequest
import ua.vn.home.bptracker.data.local.BpDatabase
import ua.vn.home.bptracker.feature.reminders.NotificationHelper
import ua.vn.home.bptracker.feature.reminders.ReminderScheduler

sealed interface AuthState {
    data object Loading : AuthState
    data class LoggedOut(
        val info: String? = null,
        val signingIn: Boolean = false,
        val linkSent: Boolean = false
    ) : AuthState
    data class LoggedIn(
        val email: String,
        val showEnrollPrompt: Boolean = false
    ) : AuthState
}

class AuthViewModel(
    private val api: AuthApi = ServiceLocator.authApi,
    private val userApi: UserApi = ServiceLocator.userApi,
    private val sessionApi: SessionApi = ServiceLocator.sessionApi,
    private val tokenStore: TokenStore = ServiceLocator.tokenStore,
    private val database: BpDatabase? = null,
    private val reminderScheduler: ReminderScheduler? = null,
    private val notificationHelper: NotificationHelper? = null,
    private val appContext: Context? = null,
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _passkeyResult = MutableStateFlow<String?>(null)
    val passkeyResult: StateFlow<String?> = _passkeyResult.asStateFlow()

    // Single-use: a magic-link token must never be confirmed twice, even if the
    // activity is recreated and re-delivers the same intent.
    private var handledToken: String? = null
    private var startupJob: Job? = null
    private val authMutex = Mutex()

    init {
        viewModelScope.launch {
            tokenStore.sessionExpired.collect {
                clearUserData()
                _state.value = AuthState.LoggedOut()
            }
        }

        startupJob = viewModelScope.launch {
            authMutex.withLock {
                if (!isActive) return@withLock
                if (MOCK_MODE) {
                    _state.value = AuthState.LoggedIn("mock@local")
                    return@withLock
                }

                tokenStore.ensureLoaded()
                if (!isActive) return@withLock

                val refresh = tokenStore.cachedRefreshToken
                if (refresh.isNullOrEmpty()) {
                    _state.value = AuthState.LoggedOut()
                    return@withLock
                }

                val cachedEmail = tokenStore.cachedEmail ?: ""
                _state.value = AuthState.LoggedIn(cachedEmail)

                try {
                    val me = userApi.me()
                    if (!isActive) return@withLock
                    tokenStore.saveEmail(me.email)
                    _state.value = AuthState.LoggedIn(me.email)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    if (tokenStore.cachedRefreshToken == null || (e is HttpException && e.code() == 403)) {
                        if (e is HttpException && e.code() == 403) {
                            tokenStore.clear()
                            clearUserData()
                        }
                        _state.value = AuthState.LoggedOut()
                    }
                    // Network errors and 5xx keep LoggedIn
                }
            }
        }
    }

    private suspend fun clearUserData() {
        val db = database ?: try { ServiceLocator.database } catch (_: Throwable) { null }
        db?.let {
            try {
                withContext(Dispatchers.IO) {
                    it.clearAllTables()
                }
            } catch (_: Throwable) { }
        }

        val scheduler = reminderScheduler ?: try { ServiceLocator.reminderScheduler } catch (_: Throwable) { null }
        try {
            scheduler?.cancelAllReminders()
        } catch (_: Throwable) { }

        val ctx = appContext ?: try { ServiceLocator.applicationContext } catch (_: Throwable) { null }
        ctx?.let {
            try {
                WorkManager.getInstance(it).cancelAllWork()
            } catch (_: Throwable) { }
        }

        val notifHelper = notificationHelper ?: try { ServiceLocator.notificationHelper } catch (_: Throwable) { null }
        try {
            notifHelper?.cancelAll()
        } catch (_: Throwable) { }
    }

    fun handleIntent(uri: Uri?) {
        val token = uri?.getQueryParameter("token") ?: return
        if (token == handledToken) return
        handledToken = token
        confirmMagicLink(token)
    }

    fun consumePasskeyResult() {
        _passkeyResult.value = null
    }

    fun requestMagicLink(email: String) {
        viewModelScope.launch {
            try {
                api.requestMagicLink(MagicLinkRequest(email))
                _state.value = AuthState.LoggedOut(linkSent = true)
            } catch (e: Exception) {
                _state.value = AuthState.LoggedOut(info = "Failed to send link: ${e.message}")
            }
        }
    }

    private fun confirmMagicLink(token: String) {
        startupJob?.cancel()
        _state.value = AuthState.Loading
        viewModelScope.launch {
            authMutex.withLock {
                try {
                    val result = api.confirmMagicLink(MagicLinkConfirmRequest(token))
                    tokenStore.save(result.accessToken, result.refreshToken)
                    val me = userApi.me()
                    tokenStore.saveEmail(me.email)
                    _state.value = AuthState.LoggedIn(me.email, showEnrollPrompt = true)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    _state.value = AuthState.LoggedOut(info = "Link expired or already used.")
                }
            }
        }
    }

    fun signIn(activity: Activity) {
        startupJob?.cancel()
        _state.value = AuthState.LoggedOut(signingIn = true)
        viewModelScope.launch {
            authMutex.withLock {
                try {
                    val begin = api.authenticateOptions()
                    val requestJson = begin.toString()

                    val credentialManager = CredentialManager.create(activity)
                    val response = credentialManager.getCredential(
                        context = activity,
                        request = GetCredentialRequest(
                            listOf(GetPublicKeyCredentialOption(requestJson))
                        )
                    )

                    val publicKeyCredential = response.credential as PublicKeyCredential
                    val assertionElement = Json.parseToJsonElement(publicKeyCredential.authenticationResponseJson)

                    val result = api.authenticateVerify(assertionElement)
                    tokenStore.save(result.accessToken, result.refreshToken)
                    
                    val me = userApi.me()
                    tokenStore.saveEmail(me.email)
                    _state.value = AuthState.LoggedIn(me.email)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: GetCredentialCancellationException) {
                    _state.value = AuthState.LoggedOut(info = null)
                } catch (_: NoCredentialException) {
                    _state.value = AuthState.LoggedOut(info = "No passkeys found for this account.")
                } catch (e: GetCredentialException) {
                    _state.value = AuthState.LoggedOut(info = "Passkey: ${e.type} | ${e.errorMessage}")
                } catch (e: Exception) {
                    _state.value = AuthState.LoggedOut(info = "Sign-in failed: ${e.message}")
                }
            }
        }
    }

    fun registerPasskey(activity: Activity) {
        viewModelScope.launch {
            try {
                val options = sessionApi.registerOptions()
                val requestJson = options.toString()
                
                val credentialManager = CredentialManager.create(activity)
                val response = credentialManager.createCredential(
                    context = activity,
                    request = CreatePublicKeyCredentialRequest(requestJson)
                )
                
                val credential = response as CreatePublicKeyCredentialResponse
                val registrationElement = Json.parseToJsonElement(credential.registrationResponseJson)
                
                sessionApi.registerVerify(registrationElement)
                _passkeyResult.value = activity.getString(R.string.auth_passkey_registered)
            } catch (e: CreateCredentialCancellationException) {
                Log.e("PasskeyReg", "Registration cancelled", e)
                // User canceled, no feedback needed
            } catch (e: CreateCredentialException) {
                val msg = "Passkey: ${e.type} | ${e.errorMessage}"
                Log.e("PasskeyReg", "Credential creation failed: $msg", e)
                if (e is CreatePublicKeyCredentialDomException) {
                    Log.e("PasskeyReg", "DOM Error: ${e.domError}", e)
                }
                _passkeyResult.value = msg
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: ""
                val msg = "HTTP ${e.code()} | $errorBody"
                Log.e("PasskeyReg", "Network error during registration: $msg", e)
                _passkeyResult.value = msg
            } catch (e: Exception) {
                val msg = "Registration failed: ${e.javaClass.simpleName} | ${e.message}"
                Log.e("PasskeyReg", "Unexpected error during registration", e)
                _passkeyResult.value = msg
            }
        }
    }

    fun dismissEnrollPrompt() {
        val current = _state.value
        if (current is AuthState.LoggedIn) {
            _state.value = current.copy(showEnrollPrompt = false)
        }
    }

    fun logout() {
        startupJob?.cancel()
        viewModelScope.launch {
            authMutex.withLock {
                val refresh = tokenStore.cachedRefreshToken
                if (refresh != null) {
                    try { sessionApi.logout(RefreshRequest(refresh)) } catch (_: Exception) { }
                }
                tokenStore.clear()
                clearUserData()
                _state.value = AuthState.LoggedOut()
            }
        }
    }
}
