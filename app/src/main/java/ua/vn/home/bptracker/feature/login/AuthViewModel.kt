package ua.vn.home.bptracker.feature.login

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import ua.vn.home.bptracker.core.config.MOCK_MODE
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.dto.RefreshRequest

sealed interface AuthState {
    data object Loading : AuthState
    data class LoggedOut(val info: String? = null, val signingIn: Boolean = false) : AuthState
    data class LoggedIn(val email: String) : AuthState
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
                } catch (e: Exception) {
                    AuthState.LoggedOut(info = null) // expected when no token: backend 401
                }
            }
        }
    }

    fun signIn(activity: Activity) {
        _state.value = AuthState.LoggedOut(signingIn = true)
        viewModelScope.launch {
            try {
                val begin = api.authenticateOptions()
                val requestJson = toCredentialManagerRequestJson(begin)

                val credentialManager = CredentialManager.create(activity)
                val response = credentialManager.getCredential(
                    context = activity,
                    request = GetCredentialRequest(
                        listOf(GetPublicKeyCredentialOption(requestJson))
                    )
                )

                val publicKeyCredential = response.credential as PublicKeyCredential
                val assertionJson = publicKeyCredential.authenticationResponseJson
                val assertionElement = Json.parseToJsonElement(assertionJson)

                val result = api.authenticateVerify(assertionElement)
                tokenStore.save(result.accessToken, result.refreshToken)
                
                // Get user info to show email
                val me = userApi.me()
                _state.value = AuthState.LoggedIn(me.email)
            } catch (e: GetCredentialCancellationException) {
                _state.value = AuthState.LoggedOut(info = null) // user dismissed the sheet
            } catch (e: GetCredentialException) {
                _state.value = AuthState.LoggedOut(info = "Passkey: ${e.type} | ${e.errorMessage}")
            } catch (e: Exception) {
                _state.value = AuthState.LoggedOut(info = "Sign-in failed: ${e.message}")
            }
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

    /**
     * Fido2NetLib's assertion options include non-standard "status"/"errorMessage"
     * fields. Credential Manager expects a clean WebAuthn PublicKeyCredentialRequestOptions,
     * so strip those keys. (If CM still rejects the request, also try removing "extensions".)
     */
    private fun toCredentialManagerRequestJson(begin: JsonElement): String {
        // TODO(auth): the C# stack (Fido2NetLib) emitted non-standard keys that Credential
        // Manager rejects. The Python `webauthn` library likely does not. Remove this once
        // a live authentication against the new backend confirms it.
        val obj: JsonObject = begin.jsonObject
        val cleaned = JsonObject(obj.filterKeys { it != "status" && it != "errorMessage" && it != "hints" })
        return cleaned.toString()
    }
}
