package ua.vn.home.bptracker.data.api

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import ua.vn.home.bptracker.data.dto.OcrResponse

interface OcrApi {
    @Multipart
    @POST("measurements/analyze")
    suspend fun analyze(@Part image: MultipartBody.Part): OcrResponse
}
