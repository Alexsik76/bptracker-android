package ua.vn.home.bptracker.data.api

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import ua.vn.home.bptracker.data.dto.MeResponse
import ua.vn.home.bptracker.data.dto.RefreshRequest
import ua.vn.home.bptracker.data.dto.TokenResponse

// PLAIN retrofit (no auth interceptor, no authenticator)
interface AuthApi {
    @POST("auth/webauthn/authenticate/options")
    suspend fun authenticateOptions(): JsonElement

    @POST("auth/webauthn/authenticate/verify")
    suspend fun authenticateVerify(@Body body: JsonElement): TokenResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokenResponse
}

// AUTHED retrofit
interface SessionApi {
    @POST("auth/logout")
    suspend fun logout(@Body body: RefreshRequest)
}

interface UserApi {
    @GET("users/me")
    suspend fun me(): MeResponse
}
