package ua.vn.home.bptracker.data.api

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import ua.vn.home.bptracker.data.dto.*

// PLAIN retrofit (no auth interceptor, no authenticator)
interface AuthApi {
    @POST("auth/webauthn/authenticate/options")
    suspend fun authenticateOptions(): JsonElement

    @POST("auth/webauthn/authenticate/verify")
    suspend fun authenticateVerify(@Body body: JsonElement): TokenResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokenResponse

    @POST("auth/magic-link/request")
    suspend fun requestMagicLink(@Body body: MagicLinkRequest)

    @POST("auth/magic-link/confirm")
    suspend fun confirmMagicLink(@Body body: MagicLinkConfirmRequest): TokenResponse
}

// AUTHED retrofit
interface SessionApi {
    @POST("auth/logout")
    suspend fun logout(@Body body: RefreshRequest)

    @POST("auth/webauthn/register/options")
    suspend fun registerOptions(): JsonElement

    @POST("auth/webauthn/register/verify")
    suspend fun registerVerify(@Body body: JsonElement): WebAuthnCredentialDto
}

interface UserApi {
    @GET("users/me")
    suspend fun me(): MeResponse

    @PATCH("users/me")
    suspend fun updateMe(@Body body: UserUpdateRequest): MeResponse
}
