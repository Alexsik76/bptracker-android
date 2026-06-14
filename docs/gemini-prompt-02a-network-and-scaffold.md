# Gemini task 2a — BP Tracker Android: network plumbing + auth scaffold (no passkey yet)

Repo: `D:\dev\bp_tracker\mobile_app`. App module `app/`. AGP 9.2.1, Kotlin 2.2.10, Compose BOM 2026.02.01, minSdk 28, namespace `ua.vn.home.bptracker`. The auth-slice dependencies (Retrofit 3.0.0, converter-kotlinx-serialization, okhttp logging, kotlinx-serialization-json 1.9.0, datastore-preferences, credentials) are ALREADY present.

This step wires the real network stack and a 2-screen scaffold (Login / Home) switched by auth state. **Passkey / Credential Manager is the NEXT step (2b) — do not implement it here.** The login button is a placeholder for now.

## RULES
- Atomic edits to existing config files (catalog, build.gradle.kts) — do not reformat or bump unrelated versions.
- For the stub `.kt` files created earlier, **replace their full contents** with the versions below.
- No local run; the user syncs and runs in Android Studio.
- English-only comments.

## Edit 1 — `gradle/libs.versions.toml`
- Under `[versions]`, change `lifecycleRuntimeKtx = "2.6.1"` to `lifecycleRuntimeKtx = "2.10.0"` (bump so the whole lifecycle group is current and consistent with viewmodel-compose).
- Under `[libraries]`, append:
```toml
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
```

## Edit 2 — `app/build.gradle.kts`
In `dependencies { }`, append after the existing `implementation(...)` lines:
```kotlin
    implementation(libs.androidx.lifecycle.viewmodel.compose)
```

## Edit 3 — replace `data/dto/AuthDtos.kt`
```kotlin
package ua.vn.home.bptracker.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// Mirrors backend records (System.Text.Json camelCase).

@Serializable
data class NativeLoginBeginResponse(
    val challengeId: String,
    val options: JsonElement
)

@Serializable
data class NativeLoginCompleteRequest(
    val challengeId: String,
    val assertion: JsonElement
)

@Serializable
data class NativeLoginResponse(
    val token: String,
    val userId: String,
    val email: String,
    val expiresAt: String
)

@Serializable
data class MeResponse(
    val id: String,
    val email: String
)
```

## Edit 4 — replace `data/api/AuthApi.kt`
```kotlin
package ua.vn.home.bptracker.data.api

import ua.vn.home.bptracker.data.dto.MeResponse
import ua.vn.home.bptracker.data.dto.NativeLoginBeginResponse
import ua.vn.home.bptracker.data.dto.NativeLoginCompleteRequest
import ua.vn.home.bptracker.data.dto.NativeLoginResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/native/login/begin")
    suspend fun loginBegin(): NativeLoginBeginResponse

    @POST("auth/native/login/complete")
    suspend fun loginComplete(@Body body: NativeLoginCompleteRequest): NativeLoginResponse

    @GET("auth/me")
    suspend fun me(): MeResponse

    @POST("auth/logout")
    suspend fun logout()
}
```

## Edit 5 — replace `core/auth/TokenStore.kt`
DataStore-backed token, plus an in-memory cache the OkHttp interceptor can read synchronously.
```kotlin
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
```

## Edit 6 — replace `core/network/AuthInterceptor.kt`
```kotlin
package ua.vn.home.bptracker.core.network

import okhttp3.Interceptor
import okhttp3.Response
import ua.vn.home.bptracker.core.auth.TokenStore

class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.cachedToken
        val request = if (token.isNullOrEmpty()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
```

## Edit 7 — replace `core/network/ApiClient.kt`
```kotlin
package ua.vn.home.bptracker.core.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import ua.vn.home.bptracker.BuildConfig
import ua.vn.home.bptracker.core.auth.TokenStore

object ApiClient {

    private val json = Json {
        ignoreUnknownKeys = true   // tolerate extra fields from Fido2 / backend
        explicitNulls = false
    }

    fun retrofit(tokenStore: TokenStore): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .addInterceptor(logging)
            .build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
```
If the IDE cannot resolve `asConverterFactory`, accept its auto-import (package `retrofit2.converter.kotlinx.serialization`).

## Edit 8 — replace `core/di/ServiceLocator.kt`
```kotlin
package ua.vn.home.bptracker.core.di

import android.content.Context
import retrofit2.create
import ua.vn.home.bptracker.core.auth.TokenStore
import ua.vn.home.bptracker.core.network.ApiClient
import ua.vn.home.bptracker.data.api.AuthApi

object ServiceLocator {

    lateinit var tokenStore: TokenStore
        private set

    val authApi: AuthApi by lazy { ApiClient.retrofit(tokenStore).create() }

    fun init(context: Context) {
        tokenStore = TokenStore(context.applicationContext)
    }
}
```

## Edit 9 — replace `BpTrackerApp.kt`
```kotlin
package ua.vn.home.bptracker

import android.app.Application
import ua.vn.home.bptracker.core.di.ServiceLocator

class BpTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
```

## Edit 10 — create `feature/login/AuthViewModel.kt`
```kotlin
package ua.vn.home.bptracker.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.di.ServiceLocator

sealed interface AuthState {
    data object Loading : AuthState
    data class LoggedOut(val info: String? = null) : AuthState
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
                val me = api.me()
                AuthState.LoggedIn(me.email)
            } catch (e: Exception) {
                // No/invalid token -> backend returns 401 -> Retrofit throws. Expected when signed out.
                AuthState.LoggedOut(info = e.message)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try { api.logout() } catch (_: Exception) { }
            tokenStore.clear()
            _state.value = AuthState.LoggedOut()
        }
    }
}
```

## Edit 11 — create `feature/login/LoginScreen.kt`
```kotlin
package ua.vn.home.bptracker.feature.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    info: String? = null,
    onSignIn: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("BP Tracker")
        Button(onClick = onSignIn, modifier = Modifier.padding(top = 24.dp)) {
            Text("Sign in with passkey")
        }
        if (info != null) {
            Text(
                text = info,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
```

## Edit 12 — create `feature/home/HomeScreen.kt`
```kotlin
package ua.vn.home.bptracker.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    email: String,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Signed in as $email")
        Button(onClick = onLogout, modifier = Modifier.padding(top = 24.dp)) {
            Text("Log out")
        }
    }
}
```

## Edit 13 — replace the body of `MainActivity.kt`
Keep the generated theme wrapper composable (whatever the wizard named it in `ui/theme/Theme.kt`, e.g. `BPTrackerTheme`). Replace the `setContent { ... }` content so it observes auth state and switches screens. Keep `enableEdgeToEdge()` / `Scaffold` if present.

```kotlin
package ua.vn.home.bptracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import ua.vn.home.bptracker.feature.home.HomeScreen
import ua.vn.home.bptracker.feature.login.AuthState
import ua.vn.home.bptracker.feature.login.AuthViewModel
import ua.vn.home.bptracker.feature.login.LoginScreen
import ua.vn.home.bptracker.ui.theme.BPTrackerTheme  // adjust to the generated theme name if different

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BPTrackerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val vm: AuthViewModel = viewModel()
                    val state by vm.state.collectAsState()
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        when (val s = state) {
                            is AuthState.Loading ->
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            is AuthState.LoggedOut ->
                                LoginScreen(info = s.info, onSignIn = { /* TODO 2b: passkey */ })
                            is AuthState.LoggedIn ->
                                HomeScreen(email = s.email, onLogout = vm::logout)
                        }
                    }
                }
            }
        }
    }
}
```

## Done when
- Gradle sync clean; `Build > Make Project` (Ctrl+F9) is **BUILD SUCCESSFUL**.
- App runs (emulator or device) and shows the Login screen with "BP Tracker" + "Sign in with passkey".
- **Live check:** the startup `/auth/me` call (no token) reaches the real backend and returns 401, landing on `LoggedOut`. In Logcat (filter by `OkHttp`) you should see the request to `https://api-bptracker.home.vn.ua/api/v1/auth/me` and a `401` response — proving Retrofit + serialization + interceptor are wired correctly.

## Notes / do NOT do
- Do NOT implement Credential Manager / passkey — that's step 2b. The "Sign in" button does nothing yet.
- Do NOT add navigation-compose — screen switching is state-based for now.
- The backend is behind a Cloudflare WAF "allow only from home" — **test from the home network** (emulator on the home machine egresses via the home IP and passes). From other networks you'd get a 403/HTML instead of 401.
- If a coroutines import (`kotlinx.coroutines.*`) fails to resolve, it should be transitive via lifecycle-viewmodel; report the error rather than adding a dependency blindly.
