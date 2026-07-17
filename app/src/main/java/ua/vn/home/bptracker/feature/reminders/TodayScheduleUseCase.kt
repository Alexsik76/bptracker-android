package ua.vn.home.bptracker.feature.reminders

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import ua.vn.home.bptracker.data.dto.*
import ua.vn.home.bptracker.data.repository.*

class TodayScheduleUseCase(
    private val prescriptionRepository: PrescriptionRepository,
    private val reminderConfigRepository: ReminderConfigRepository,
    private val intakeReportRepository: IntakeReportRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeToday(date: String): Flow<TodaySchedule> = combine(
        prescriptionRepository.getPrescriptions().flatMapLatest { prescriptions ->
            val active = prescriptions.filter { it.isActive }
            if (active.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(active.map { prescriptionRepository.getItems(it.id) }) { arrays ->
                    arrays.flatMap { it }
                }
            }
        },
        intakeReportRepository.observeForDate(date)
    ) { activeItems, intakes ->
        val config = reminderConfigRepository.getCachedConfig()
        buildTodaySchedule(date, config, activeItems, intakes)
    }

    suspend fun getTodayOnce(date: String): TodaySchedule {
        val prescriptions = prescriptionRepository.getPrescriptions().first()
        val active = prescriptions.filter { it.isActive }
        val activeItems = if (active.isEmpty()) {
            emptyList()
        } else {
            active.flatMap { prescriptionRepository.getItems(it.id).first() }
        }
        val intakes = intakeReportRepository.observeForDate(date).first()
        val config = reminderConfigRepository.getCachedConfig()
        return buildTodaySchedule(date, config, activeItems, intakes)
    }

    internal fun buildTodaySchedule(
        date: String,
        config: ReminderConfigDto?,
        activeItems: List<MedicationItemReadDto>,
        intakes: List<LocalIntake>
    ): TodaySchedule {
        if (config == null) {
            return TodaySchedule(configured = false, date = date, slots = emptyList())
        }

        val slotTimes = mapOf(
            WhenSlot.Morning to config.morningTime.take(5),
            WhenSlot.Day to config.dayTime.take(5),
            WhenSlot.Evening to config.eveningTime.take(5)
        )

        val slots = WhenSlot.entries.mapNotNull { slot ->
            val itemsForSlot = activeItems.filter { item ->
                val includedByCourse = when (item.courseType) {
                    CourseType.Ongoing -> true
                    CourseType.Course -> (item.courseStart == null) || (item.courseStart.take(10) <= date)
                }
                includedByCourse && item.whenSlots.contains(slot)
            }

            if (itemsForSlot.isEmpty()) return@mapNotNull null

            val intake = intakes.find { it.period == slot && it.date == date }
            val taken = intake != null && !intake.pendingDelete

            TodaySlot(
                slot = slot,
                time = slotTimes[slot] ?: "",
                meds = itemsForSlot.map { item ->
                    TodayMed(
                        medicine = item.medicine,
                        doseAmount = item.doseAmount,
                        doseUnit = item.doseUnit,
                        condition = item.condition,
                    )
                },
                taken = taken,
                takenAt = if (taken) intake.takenAt else null,
            )
        }

        return TodaySchedule(
            configured = true,
            date = date,
            slots = slots
        )
    }
}
