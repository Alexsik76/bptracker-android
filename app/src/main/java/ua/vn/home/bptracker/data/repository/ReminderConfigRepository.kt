package ua.vn.home.bptracker.data.repository

import retrofit2.HttpException
import ua.vn.home.bptracker.data.api.ReminderConfigApi
import ua.vn.home.bptracker.data.dto.ReminderConfigDto
import ua.vn.home.bptracker.data.local.dao.ReminderConfigDao
import ua.vn.home.bptracker.data.local.entity.toDto
import ua.vn.home.bptracker.data.local.entity.toEntity

interface ReminderConfigRepository {
    suspend fun getConfig(): ReminderConfigDto?
    suspend fun saveConfig(config: ReminderConfigDto): ReminderConfigDto
    suspend fun getCachedConfig(): ReminderConfigDto?
}

class RealReminderConfigRepository(
    private val api: ReminderConfigApi,
    private val dao: ReminderConfigDao
) : ReminderConfigRepository {
    override suspend fun getConfig(): ReminderConfigDto? {
        return try {
            val remote = api.getConfig()
            remote?.let {
                dao.upsert(it.toEntity())
            }
            remote
        } catch (e: HttpException) {
            if (e.code() == 404) null else throw e
        }
    }

    override suspend fun saveConfig(config: ReminderConfigDto): ReminderConfigDto {
        val saved = api.saveConfig(config)
        dao.upsert(saved.toEntity())
        return saved
    }

    override suspend fun getCachedConfig(): ReminderConfigDto? {
        return dao.getConfig()?.toDto()
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

    override suspend fun getCachedConfig(): ReminderConfigDto? = mockConfig
}
