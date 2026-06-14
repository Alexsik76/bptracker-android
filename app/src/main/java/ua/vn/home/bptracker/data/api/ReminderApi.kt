package ua.vn.home.bptracker.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import ua.vn.home.bptracker.data.dto.*

interface ReminderApi {
    @GET("reminders/today")
    suspend fun getToday(@Query("timezone") timezone: String): TodayMeds

    @POST("reminders/confirm")
    suspend fun confirm(@Body body: ConfirmIntakeRequest)

    @GET("reminders/template/active")
    suspend fun getActiveTemplate(): ReminderTemplate

    @PATCH("reminders/template/{id}")
    suspend fun updateTemplate(
        @Path("id") id: String,
        @Body body: UpdateTemplateRequest
    ): ReminderTemplate
}
