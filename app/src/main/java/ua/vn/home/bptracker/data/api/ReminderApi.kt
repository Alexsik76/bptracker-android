package ua.vn.home.bptracker.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import ua.vn.home.bptracker.data.dto.ConfirmIntakeRequest
import ua.vn.home.bptracker.data.dto.TodayMeds

interface ReminderApi {
    @GET("reminders/today")
    suspend fun getToday(@Query("timezone") timezone: String): TodayMeds

    @POST("reminders/confirm")
    suspend fun confirm(@Body body: ConfirmIntakeRequest)
}
