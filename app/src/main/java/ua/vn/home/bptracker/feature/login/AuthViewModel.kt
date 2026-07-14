package ua.vn.home.bptracker.feature.login

import android.app.Activity
import android.net.Uri
import androidx.credentials.*
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import ua.vn.home.bptracker.core.config.MOCK_MODE
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.dto.MagicLinkConfirmRequest
import ua.vn.home.bptracker.data.dto.MagicLinkRequest
import ua.vn.home.bptracker.data.dto.RefreshRequest

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

class AuthViewModel : ViewModel() {

    private val api = ServiceLocator.authApi
    private val userApi = ServiceLocator.userApi
    private val sessionApi = ServiceLocator.sessionApi
    private val tokenStore = ServiceLocator.tokenStore

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (MOCK_MODE) {
                _state.value = AuthState.LoggedIn("mock@local")
            } else {
                tokenStore.load()
                _state.value = try {
                    AuthState.LoggedIn(userApi.me().email)
                } catch (_: Exception) {
                    AuthState.LoggedOut(info = null)
                }
            }
        }
    }

    fun handleIntent(uri: Uri?) {
        val token = uri?.getQueryParameter("token") ?: return
        confirmMagicLink(token)
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
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = api.confirmMagicLink(MagicLinkConfirmRequest(token))
                tokenStore.save(result.accessToken, result.refreshToken)
                val me = userApi.me()
                _state.value = AuthState.LoggedIn(me.email, showEnrollPrompt = true)
            } catch (e: Exception) {
                _state.value = AuthState.LoggedOut(info = "Link expired or already used.")
            }
        }
    }

    fun signIn(activity: Activity) {
        _state.value = AuthState.LoggedOut(signingIn = true)
        viewModelScope.launch {
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
                _state.value = AuthState.LoggedIn(me.email)
            } catch (e: GetCredentialCancellationException) {
                _state.value = AuthState.LoggedOut(info = null)
            } catch (e: GetCredentialException) {
                _state.value = AuthState.LoggedOut(info = "Passkey: ${e.type} | ${e.errorMessage}")
            } catch (e: Exception) {
                _state.value = AuthState.LoggedOut(info = "Sign-in failed: ${e.message}")
            }
        }
    }

    fun registerPasskey(activity: Activity, onComplete: (String?) -> Unit) {
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
                onComplete(null) // Success
            } catch (e: CreateCredentialCancellationException) {
                onComplete(null) // User canceled, not an error to show
            } catch (e: CreateCredentialException) {
                onComplete("Failed to register: ${e.type} ${e.errorMessage}")
            } catch (e: Exception) {
                onComplete("Failed to register: ${e.message}")
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
        viewModelScope.launch {
            val refresh = tokenStore.cachedRefreshToken
            if (refresh != null) {
                try { sessionApi.logout(RefreshRequest(refresh)) } catch (_: Exception) { }
            }
            tokenStore.clear()
            _state.value = AuthState.LoggedOut()
        }
    }
}
