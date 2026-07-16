package ua.vn.home.bptracker.data.api

import retrofit2.Response
import retrofit2.http.*
import ua.vn.home.bptracker.data.dto.IntakeReportCreateDto
import ua.vn.home.bptracker.data.dto.IntakeReportReadDto

interface IntakeReportApi {
    @POST("reminders/intake-reports")
    suspend fun createIntakeReport(@Body body: IntakeReportCreateDto): IntakeReportReadDto

    @GET("reminders/intake-reports")
    suspend fun getIntakeReports(): List<IntakeReportReadDto>

    @GET("reminders/intake-reports/{id}")
    suspend fun getIntakeReport(@Path("id") id: String): IntakeReportReadDto

    @DELETE("reminders/intake-reports/{id}")
    suspend fun deleteIntakeReport(@Path("id") id: String): Response<Unit>
}
