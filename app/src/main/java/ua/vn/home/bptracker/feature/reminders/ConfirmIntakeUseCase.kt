package ua.vn.home.bptracker.feature.reminders

import ua.vn.home.bptracker.data.dto.WhenSlot
import ua.vn.home.bptracker.data.repository.IntakeReportRepository
import java.time.LocalDate

class ConfirmIntakeUseCase(
    private val repo: IntakeReportRepository,
    private val notificationHelper: NotificationHelper
) {
    suspend operator fun invoke(slot: WhenSlot, date: LocalDate, takenAt: String?) {
        notificationHelper.cancelNotification(date, slot)
        repo.confirm(slot, date.toString(), takenAt)
    }
}
