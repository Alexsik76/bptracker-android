package ua.vn.home.bptracker.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import ua.vn.home.bptracker.data.dto.CreateMeasurementRequest
import ua.vn.home.bptracker.data.dto.MeasurementDto

interface MeasurementApi {
    @GET("measurements")
    suspend fun getMeasurements(@Query("days") days: Int): List<MeasurementDto>

    @POST("measurements")
    suspend fun createMeasurement(@Body body: CreateMeasurementRequest): MeasurementDto
}
