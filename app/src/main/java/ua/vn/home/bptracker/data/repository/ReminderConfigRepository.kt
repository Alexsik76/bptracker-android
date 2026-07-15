package ua.vn.home.bptracker.data.repository

import retrofit2.HttpException
import ua.vn.home.bptracker.data.api.ReminderConfigApi
import ua.vn.home.bptracker.data.dto.ReminderConfigDto

interface ReminderConfigRepository {
    suspend fun getConfig(): ReminderConfigDto?
    suspend fun saveConfig(config: ReminderConfigDto): ReminderConfigDto
}

class RealReminderConfigRepository(
    private val api: ReminderConfigApi
) : ReminderConfigRepository {
    override suspend fun getConfig(): ReminderConfigDto? {
        return try {
            api.getConfig()
        } catch (e: HttpException) {
            if (e.code() == 404) null else throw e
        }
    }

    override suspend fun saveConfig(config: ReminderConfigDto): ReminderConfigDto {
        return api.saveConfig(config)
    }
}

class MockReminderConfigRepository : ReminderConfigRepository {
    private var mockConfig = ReminderConfigDto(
        morningTime = "08:00:00",
        dayTime = "14:00:00",
        eveningTime = "20:00:00",
        maxReminders = 3,
        durationMinutes = 60
    )

    override suspend fun getConfig(): ReminderConfigDto? = mockConfig

    override suspend fun saveConfig(config: ReminderConfigDto): ReminderConfigDto {
        mockConfig = config
        return mockConfig
    }
}
