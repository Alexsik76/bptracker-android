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
