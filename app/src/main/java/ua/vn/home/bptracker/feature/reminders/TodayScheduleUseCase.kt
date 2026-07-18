package ua.vn.home.bptracker.feature.reminders

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import ua.vn.home.bptracker.data.dto.*
import ua.vn.home.bptracker.data.repository.*

import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

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
                    CourseType.Course -> {
                        val isDaily = item.freqPeriodUnit == FreqPeriodUnit.Day && item.freqPeriod == 1
                        if (isDaily && item.courseStart != null && item.courseIntakes != null) {
                            courseSlotActive(date, slot, item, slotTimes)
                        } else {
                            // v1 limitation: non-daily courses (weekly, etc.) or courses with 
                            // incomplete data fallback to simple start-gate logic.
                            (item.courseStart == null) || (item.courseStart.take(10) <= date)
                        }
                    }
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

    internal fun courseSlotActive(
        date: String,
        slot: WhenSlot,
        item: MedicationItemReadDto,
        slotTimes: Map<WhenSlot, String>
    ): Boolean {
        val courseStart = item.courseStart ?: return true
        val intakes = item.courseIntakes ?: return courseStart.take(10) <= date
        val checkDate = try { LocalDate.parse(date) } catch (_: DateTimeParseException) { return false }

        val startDT = try {
            OffsetDateTime.parse(courseStart)
        } catch (_: DateTimeParseException) {
            try {
                LocalDate.parse(courseStart.take(10)).atStartOfDay().atOffset(OffsetDateTime.now().offset)
            } catch (_: Exception) {
                return false
            }
        }

        val startDate = startDT.toLocalDate()
        val startTime = startDT.toLocalTime()

        if (checkDate.isBefore(startDate)) return false

        // Sort item slots chronologically based on config times
        val sortedSlots = item.whenSlots.sortedBy { slotTimes[it] ?: "00:00" }
        val activeDay0Slots = sortedSlots.filter { 
            val timeStr = slotTimes[it] ?: "00:00"
            val slotTime = try { LocalTime.parse(timeStr) } catch (_: Exception) { LocalTime.MIDNIGHT }
            slotTime >= startTime 
        }

        val occurrenceIndex = if (checkDate == startDate) {
            val idx = activeDay0Slots.indexOf(slot)
            if (idx == -1) return false
            (idx + 1).toLong()
        } else {
            val daysPassed = ChronoUnit.DAYS.between(startDate, checkDate)
            val idxInDay = sortedSlots.indexOf(slot)
            if (idxInDay == -1) return false
            activeDay0Slots.size.toLong() + (daysPassed - 1) * sortedSlots.size + (idxInDay + 1)
        }

        return occurrenceIndex in 1..intakes.toLong()
    }
}
