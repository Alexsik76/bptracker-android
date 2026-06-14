# Gemini task 2b — BP Tracker Android: passkey login via Credential Manager

Repo: `D:\dev\bp_tracker\mobile_app`. Builds on 2a (network stack + Login/Home scaffold are live; startup `/auth/me` returns 401 → LoggedOut). Now implement the real passkey sign-in.

Flow: tap "Sign in with passkey" → `POST /auth/native/login/begin` → adapt options JSON → Android **Credential Manager** gets a passkey assertion for RP `bptracker.home.vn.ua` → `POST /auth/native/login/complete` → save token → state `LoggedIn`.

## RULES
- Atomic/additive; replace full contents only for the files named. English-only comments.
- Dependencies `androidx.credentials` + `androidx.credentials:credentials-play-services-auth` are already present.
- No local run; user tests on a real device (Wi-Fi/USB) on the home network.

## Background the implementer must respect
- The backend `begin` returns `NativeLoginBeginResponse(challengeId, options)` where `options` is Fido2NetLib's assertion options JSON. It contains the standard WebAuthn request fields (`challenge`, `rpId`, `allowCredentials`, `userVerification`, `timeout`, `extensions`) **plus two non-standard fields `status` and `errorMessage`**. Credential Manager's `GetPublicKeyCredentialOption(requestJson)` wants a clean WebAuthn `PublicKeyCredentialRequestOptions`. So we **strip `status` and `errorMessage`** before passing it on. (If Credential Manager still rejects it, the next thing to try is also stripping `extensions` — leave a comment noting that.)
- Credential Manager returns `PublicKeyCredential.authenticationResponseJson` — a standard WebAuthn assertion. We parse it to a JSON element and send it as `assertion` in the complete request; the backend deserializes it into `AuthenticatorAssertionRawResponse`.
- `CredentialManager.getCredential(...)` needs an **Activity** context (it shows system UI). Pass the Activity into the sign-in function; do not store it in the ViewModel.

## Edit 1 — replace `feature/login/AuthViewModel.kt`
Add the sign-in flow to the existing ViewModel. Keep the existing startup `/auth/me` logic.
```kotlin
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.dto.NativeLoginBeginResponse
import ua.vn.home.bptracker.data.dto.NativeLoginCompleteRequest

sealed interface AuthState {
    data object Loading : AuthState
    data class LoggedOut(val info: String? = null, val signingIn: Boolean = false) : AuthState
    data class LoggedIn(val email: String) : AuthState
}

class AuthViewModel : ViewModel() {

    private val api = ServiceLocator.authApi
    private val tokenStore = ServiceLocator.tokenStore

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            tokenStore.load()
            _state.value = try {
                AuthState.LoggedIn(api.me().email)
            } catch (e: Exception) {
                AuthState.LoggedOut(info = null) // expected when no token: backend 401
            }
        }
    }

    fun signIn(activity: Activity) {
        _state.value = AuthState.LoggedOut(signingIn = true)
        viewModelScope.launch {
            try {
                val begin = api.loginBegin()
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

                val result = api.loginComplete(
                    NativeLoginCompleteRequest(begin.challengeId, assertionElement)
                )
                tokenStore.save(result.token)
                _state.value = AuthState.LoggedIn(result.email)
            } catch (e: GetCredentialCancellationException) {
                _state.value = AuthState.LoggedOut(info = null) // user dismissed the sheet
            } catch (e: GetCredentialException) {
                _state.value = AuthState.LoggedOut(info = "Passkey error: ${e.javaClass.simpleName}")
            } catch (e: Exception) {
                _state.value = AuthState.LoggedOut(info = "Sign-in failed: ${e.message}")
            }
        }
    }

    /**
     * Fido2NetLib's assertion options include non-standard "status"/"errorMessage"
     * fields. Credential Manager expects a clean WebAuthn PublicKeyCredentialRequestOptions,
     * so strip those keys. (If CM still rejects the request, also try removing "extensions".)
     */
    private fun toCredentialManagerRequestJson(begin: NativeLoginBeginResponse): String {
        val obj: JsonObject = begin.options.jsonObject
        val cleaned = JsonObject(obj.filterKeys { it != "status" && it != "errorMessage" })
        return cleaned.toString()
    }
}
```

## Edit 2 — replace `feature/login/LoginScreen.kt`
Wire the button to `signIn`, using the Activity from the Compose context. Show a spinner while signing in.
```kotlin
package ua.vn.home.bptracker.feature.login

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    info: String? = null,
    signingIn: Boolean = false,
    onSignIn: (Activity) -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("BP Tracker")
        if (signingIn) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
        } else {
            Button(onClick = { onSignIn(activity) }, modifier = Modifier.padding(top = 24.dp)) {
                Text("Sign in with passkey")
            }
        }
        if (info != null) {
            Text(text = info, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
        }
    }
}
```

## Edit 3 — `MainActivity.kt`, the `LoggedOut` branch
Update the call site to pass `signingIn` and the new `onSignIn` signature:
```kotlin
is AuthState.LoggedOut ->
    LoginScreen(
        info = s.info,
        signingIn = s.signingIn,
        onSignIn = { activity -> vm.signIn(activity) }
    )
```
Leave the `Loading` and `LoggedIn` branches unchanged.

## Done when
- `Build > Make Project` is BUILD SUCCESSFUL.
- On a real device on the home network (with the user's Google account / passkey for `bptracker.home.vn.ua` available), tapping "Sign in with passkey" shows the system passkey sheet; on success the app navigates to the Home screen showing the signed-in email; relaunching the app stays logged in (token persisted, `/auth/me` → 200).
- In Logcat (`OkHttp`): `POST /auth/native/login/begin` → 200, then `POST /auth/native/login/complete` → 200 returning the token.

## Notes / contingencies (do not pre-solve; report if hit)
- Requires the backend change `FIDO2_ANDROID_ORIGINS` to be deployed first, otherwise `complete` returns 401 (origin rejected).
- If `getCredential` throws "no credentials available", the device's Google account has no passkey for `bptracker.home.vn.ua` yet — that's the registration gap (separate future step), not a code bug.
- If Credential Manager rejects the request JSON, try additionally stripping `extensions` in `toCredentialManagerRequestJson` (noted in code).
- Do NOT implement native passkey *registration* — login only for now.
