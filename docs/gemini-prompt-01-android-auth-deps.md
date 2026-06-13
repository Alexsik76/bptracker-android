# Gemini task — BP Tracker Android: auth-slice dependencies + package skeleton

You operate in the repo at `D:\dev\bp_tracker\mobile_app` (a fresh Android Studio project: AGP 9.2.1, Kotlin 2.2.10, Compose BOM 2026.02.01, compileSdk 36, minSdk 28, namespace `ua.vn.home.bptracker`). The app module is `app/`.

## RULES (read before editing)
- These are **atomic, additive edits**. Do **NOT** rewrite or reformat any file. Only insert the exact lines specified, at the specified places.
- Do **NOT** change any existing version in `libs.versions.toml` (leave `coreKtx`, `lifecycleRuntimeKtx`, `activityCompose`, `agp`, `kotlin`, `composeBom`, etc. exactly as they are).
- Do **NOT** touch the `android { compileSdk { ... } }` block, `buildTypes`, `compileOptions`, or any existing dependency line.
- Do not add libraries beyond those listed here.
- No local run needed; the user runs Gradle sync in Android Studio.

## Edit 1 — `gradle/libs.versions.toml`

Under `[versions]`, append:
```toml
retrofit = "3.0.0"
okhttp = "4.12.0"
kotlinxSerialization = "1.9.0"
datastore = "1.1.1"
credentials = "1.5.0"
```

Under `[libraries]`, append:
```toml
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-converter-kotlinx = { group = "com.squareup.retrofit2", name = "converter-kotlinx-serialization", version.ref = "retrofit" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
androidx-credentials = { group = "androidx.credentials", name = "credentials", version.ref = "credentials" }
androidx-credentials-play-services = { group = "androidx.credentials", name = "credentials-play-services-auth", version.ref = "credentials" }
```

Under `[plugins]`, append (reuses the existing `kotlin` version ref — do not hardcode a version):
```toml
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

## Edit 2 — `app/build.gradle.kts`

In the `plugins { }` block, append after the existing aliases:
```kotlin
    alias(libs.plugins.kotlin.serialization)
```

Inside `android { defaultConfig { ... } }`, append (after `testInstrumentationRunner`):
```kotlin
        buildConfigField("String", "API_BASE_URL", "\"https://api-bptracker.home.vn.ua/api/v1/\"")
```

In `android { buildFeatures { ... } }`, add alongside `compose = true`:
```kotlin
        buildConfig = true
```

In `dependencies { }`, append after the existing `implementation(...)` lines:
```kotlin
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
```

## Edit 3 — create package skeleton (new files)

Base dir: `app/src/main/java/ua/vn/home/bptracker/`. Create exactly these files with exactly this content.

`BpTrackerApp.kt`
```kotlin
package ua.vn.home.bptracker

import android.app.Application

class BpTrackerApp : Application()
```

`core/di/ServiceLocator.kt`
```kotlin
package ua.vn.home.bptracker.core.di

// Manual DI: lazy singletons (api, tokenStore, repositories) wired here later.
object ServiceLocator
```

`core/network/ApiClient.kt`
```kotlin
package ua.vn.home.bptracker.core.network

import ua.vn.home.bptracker.BuildConfig

// TODO: build a Retrofit instance (OkHttp + kotlinx-serialization converter) in a later step.
object ApiClient {
    const val BASE_URL: String = BuildConfig.API_BASE_URL
}
```

`core/network/AuthInterceptor.kt`
```kotlin
package ua.vn.home.bptracker.core.network

import okhttp3.Interceptor
import okhttp3.Response

// Attaches "Authorization: Bearer <token>" once a device token is stored.
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // TODO: read token from TokenStore and add the header when present.
        return chain.proceed(chain.request())
    }
}
```

`core/auth/TokenStore.kt`
```kotlin
package ua.vn.home.bptracker.core.auth

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

private val Context.authDataStore by preferencesDataStore(name = "auth")

// TODO: read / write / clear the device token via authDataStore.
class TokenStore(private val context: Context)
```

`data/dto/AuthDtos.kt`
```kotlin
package ua.vn.home.bptracker.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

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
```

`data/api/AuthApi.kt`
```kotlin
package ua.vn.home.bptracker.data.api

import ua.vn.home.bptracker.data.dto.NativeLoginBeginResponse
import ua.vn.home.bptracker.data.dto.NativeLoginCompleteRequest
import ua.vn.home.bptracker.data.dto.NativeLoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/native/login/begin")
    suspend fun loginBegin(): NativeLoginBeginResponse

    @POST("auth/native/login/complete")
    suspend fun loginComplete(@Body body: NativeLoginCompleteRequest): NativeLoginResponse
}
```

## Edit 4 — `app/src/main/AndroidManifest.xml`

- Add, above the `<application>` tag, inside `<manifest>`:
```xml
    <uses-permission android:name="android.permission.INTERNET" />
```
- On the existing `<application>` element, add the attribute `android:name=".BpTrackerApp"` (do not remove or change other attributes).

## Done when
- The new files exist with the exact content above.
- `libs.versions.toml` and `app/build.gradle.kts` contain the additions, with **no existing version or line modified**.
- (User action) Gradle sync completes with no errors and the project builds (`@Serializable` DTOs and the Retrofit interface compile, confirming the serialization plugin, Retrofit, OkHttp, DataStore, and Credentials dependencies all resolve).

## Do NOT do
- Do not add navigation-compose, lifecycle-viewmodel-compose, Room, CameraX, ONNX, WorkManager, or any UI/login implementation — those come in later steps.
- Do not modify `MainActivity.kt` or the `ui/theme/` files.
- Do not bump AGP, Kotlin, Compose BOM, or any wizard-generated version.
