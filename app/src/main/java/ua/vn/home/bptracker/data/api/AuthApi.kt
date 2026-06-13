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
