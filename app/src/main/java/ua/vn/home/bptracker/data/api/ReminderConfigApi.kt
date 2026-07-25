package ua.vn.home.bptracker.data.api

import retrofit2.http.*
import ua.vn.home.bptracker.data.dto.ReminderConfigDto

interface ReminderConfigApi {
    @GET("reminders/config")
    suspend fun getConfig(): ReminderConfigDto

    @PUT("reminders/config")
    suspend fun saveConfig(@Body body: ReminderConfigDto): ReminderConfigDto
}
