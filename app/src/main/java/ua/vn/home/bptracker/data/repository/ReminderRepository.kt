package ua.vn.home.bptracker.data.repository

import ua.vn.home.bptracker.data.api.ReminderApi
import ua.vn.home.bptracker.data.dto.*
import ua.vn.home.bptracker.data.local.dao.MedIntakeDao
import ua.vn.home.bptracker.data.local.entity.toDto
import ua.vn.home.bptracker.data.local.entity.toEntity
import java.time.OffsetDateTime

interface ReminderRepository {
    suspend fun getToday(timezone: String): TodayMeds
    suspend fun confirm(period: String, timezone: String)
    suspend fun getActiveTemplate(): ReminderTemplate?
    suspend fun updateTemplate(id: String, req: UpdateTemplateRequest): ReminderTemplate
}

class RealReminderRepository(
    private val api: ReminderApi,
    private val dao: MedIntakeDao
) : ReminderRepository {
    override suspend fun getToday(timezone: String): TodayMeds {
        return try {
            val remote = api.getToday(timezone)
            // Clear current date and older to avoid duplicates/stale data
            dao.deleteOld(remote.date)
            // Explicitly clear today's cache before inserting fresh remote data
            remote.intakes.forEach { 
                dao.deleteByDateAndPeriod(remote.date, it.period)
            }
            dao.insertAll(remote.intakes.map { it.toEntity(remote.date) })
            remote
        } catch (e: Exception) {
            android.util.Log.e("ReminderRepository", "Failed to fetch today meds", e)
            val date = java.time.LocalDate.now().toString()
            val local = dao.getByDate(date)
            TodayMeds(date, local.map { it.toDto() })
        }
    }

    override suspend fun confirm(period: String, timezone: String) {
        val date = OffsetDateTime.now().toLocalDate().toString()
        try {
            api.confirm(ConfirmIntakeRequest(period, timezone))
            dao.updateStatus(date, period, "Confirmed", OffsetDateTime.now().toString())
        } catch (e: Exception) {
            // Offline confirm - will need sync later
            dao.updateStatus(date, period, "Confirmed", OffsetDateTime.now().toString())
        }
    }

    override suspend fun getActiveTemplate(): ReminderTemplate? {
        return try {
            api.getActiveTemplate()
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 404) null else throw e
        }
    }

    override suspend fun updateTemplate(id: String, req: UpdateTemplateRequest): ReminderTemplate {
        return api.updateTemplate(id, req)
    }
}

class MockReminderRepository : ReminderRepository {
    private var mockTemplate = ReminderTemplate(
        id = "mock-id",
        schemaId = "mock-schema",
        isActive = true,
        durationMinutes = 15,
        maxReminders = 1,
        periods = mapOf(
            "Morning" to PeriodConfig("08:00", listOf("Bisoprolol 5mg")),
            "Evening" to PeriodConfig("20:00", listOf("Atorvastatin 20mg"))
        )
    )

    private var mockIntakes = mutableListOf(
        TodayIntake("Morning", "08:00", listOf("Bisoprolol 5mg", "Aspirin 75mg"), "Confirmed", OffsetDateTime.now().minusHours(4).toString()),
        TodayIntake("Day", "14:00", listOf("Omega-3"), null, null),
        TodayIntake("Evening", "20:00", listOf("Atorvastatin 20mg"), null, null)
    )

    override suspend fun getToday(timezone: String): TodayMeds {
        return TodayMeds(date = OffsetDateTime.now().toLocalDate().toString(), intakes = mockIntakes)
    }

    override suspend fun confirm(period: String, timezone: String) {
        val index = mockIntakes.indexOfFirst { it.period == period }
        if (index != -1) {
            val current = mockIntakes[index]
            mockIntakes[index] = current.copy(
                status = "Confirmed",
                timeTaken = OffsetDateTime.now().toString()
            )
        }
    }

    override suspend fun getActiveTemplate(): ReminderTemplate? {
        return mockTemplate
    }

    override suspend fun updateTemplate(id: String, req: UpdateTemplateRequest): ReminderTemplate {
        mockTemplate = mockTemplate.copy(
            isActive = req.isActive ?: mockTemplate.isActive,
            durationMinutes = req.durationMinutes ?: mockTemplate.durationMinutes,
            maxReminders = req.maxReminders ?: mockTemplate.maxReminders,
            periods = req.periods ?: mockTemplate.periods
        )
        return mockTemplate
    }
}
