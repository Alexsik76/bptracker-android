package ua.vn.home.bptracker.data.api

import retrofit2.http.*
import ua.vn.home.bptracker.data.dto.CreateMeasurementRequest
import ua.vn.home.bptracker.data.dto.MeasurementDto

interface MeasurementApi {
    @GET("measurements")
    suspend fun getMeasurements(@Query("days") days: Int): List<MeasurementDto>

    @POST("measurements")
    suspend fun createMeasurement(@Body body: CreateMeasurementRequest): MeasurementDto

    @DELETE("measurements/{id}")
    suspend fun deleteMeasurement(@Path("id") id: String)
}
