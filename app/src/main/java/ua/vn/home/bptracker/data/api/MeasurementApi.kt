package ua.vn.home.bptracker.data.api

import retrofit2.http.*
import ua.vn.home.bptracker.data.dto.CreateMeasurementRequest
import ua.vn.home.bptracker.data.dto.MeasurementDto
import ua.vn.home.bptracker.data.dto.MeasurementPageDto

interface MeasurementApi {
    @GET("measurements")
    suspend fun getMeasurements(
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): MeasurementPageDto

    @POST("measurements")
    suspend fun createMeasurement(@Body body: CreateMeasurementRequest): MeasurementDto

    @DELETE("measurements/{id}")
    suspend fun deleteMeasurement(@Path("id") id: String)
}
