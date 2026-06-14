package ua.vn.home.bptracker.data.repository

import ua.vn.home.bptracker.data.api.ReminderApi
import ua.vn.home.bptracker.data.dto.ConfirmIntakeRequest
import ua.vn.home.bptracker.data.dto.TodayIntake
import ua.vn.home.bptracker.data.dto.TodayMeds
import java.time.OffsetDateTime

interface ReminderRepository {
    suspend fun getToday(timezone: String): TodayMeds
    suspend fun confirm(period: String, timezone: String)
}

class RealReminderRepository(private val api: ReminderApi) : ReminderRepository {
    override suspend fun getToday(timezone: String): TodayMeds = api.getToday(timezone)
    override suspend fun confirm(period: String, timezone: String) {
        api.confirm(ConfirmIntakeRequest(period, timezone))
    }
}

class MockReminderRepository : ReminderRepository {
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
}
