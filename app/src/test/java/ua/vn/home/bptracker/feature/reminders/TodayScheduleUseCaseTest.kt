package ua.vn.home.bptracker.feature.reminders

import org.junit.Assert.*
import org.junit.Test
import ua.vn.home.bptracker.data.dto.*
import ua.vn.home.bptracker.data.repository.LocalIntake
import ua.vn.home.bptracker.data.repository.MockIntakeReportRepository
import ua.vn.home.bptracker.data.repository.MockPrescriptionRepository
import ua.vn.home.bptracker.data.repository.MockReminderConfigRepository

class TodayScheduleUseCaseTest {

    private val useCase = TodayScheduleUseCase(
        prescriptionRepository = MockPrescriptionRepository(),
        reminderConfigRepository = MockReminderConfigRepository(),
        intakeReportRepository = MockIntakeReportRepository()
    )

    private val config = ReminderConfigDto(
        morningTime = "08:00:00",
        dayTime = "14:00:00",
        eveningTime = "20:00:00",
        maxReminders = 1,
        durationMinutes = 15
    )

    @Test
    fun `null config returns unconfigured schedule`() {
        val result = useCase.buildTodaySchedule("2026-07-17", null, emptyList(), emptyList())
        assertFalse(result.configured)
        assertTrue(result.slots.isEmpty())
    }

    @Test
    fun `ongoing Morning item creates one Morning slot`() {
        val item = createItem(id = "1", medicine = "Bisoprolol", slots = listOf(WhenSlot.Morning))
        val result = useCase.buildTodaySchedule("2026-07-17", config, listOf(item), emptyList())

        assertTrue(result.configured)
        assertEquals(1, result.slots.size)
        val slot = result.slots[0]
        assertEquals(WhenSlot.Morning, slot.slot)
        assertEquals("08:00", slot.time)
        assertEquals(1, slot.meds.size)
        assertEquals("Bisoprolol", slot.meds[0].medicine)
        assertFalse(slot.taken)
    }

    @Test
    fun `items from two prescriptions in same slot are merged`() {
        val item1 = createItem(id = "1", medicine = "Med 1", slots = listOf(WhenSlot.Evening))
        val item2 = createItem(id = "2", medicine = "Med 2", slots = listOf(WhenSlot.Evening))

        val result = useCase.buildTodaySchedule("2026-07-17", config, listOf(item1, item2), emptyList())

        assertEquals(1, result.slots.size)
        assertEquals(2, result.slots[0].meds.size)
        assertEquals(WhenSlot.Evening, result.slots[0].slot)
    }

    @Test
    fun `empty slots are omitted and remaining are ordered`() {
        val item1 = createItem(id = "1", medicine = "Med 1", slots = listOf(WhenSlot.Evening))
        val item2 = createItem(id = "2", medicine = "Med 2", slots = listOf(WhenSlot.Morning))

        val result = useCase.buildTodaySchedule("2026-07-17", config, listOf(item1, item2), emptyList())

        assertEquals(2, result.slots.size)
        assertEquals(WhenSlot.Morning, result.slots[0].slot)
        assertEquals(WhenSlot.Evening, result.slots[1].slot)
    }

    @Test
    fun `intake state is correctly detected`() {
        val item = createItem(id = "1", medicine = "Med", slots = listOf(WhenSlot.Day))
        val date = "2026-07-17"
        
        // Taken
        val intakes1 = listOf(LocalIntake(WhenSlot.Day, date, "2026-07-17T14:05:00", false))
        val res1 = useCase.buildTodaySchedule(date, config, listOf(item), intakes1)
        assertTrue(res1.slots[0].taken)
        assertEquals("2026-07-17T14:05:00", res1.slots[0].takenAt)

        // Pending delete
        val intakes2 = listOf(LocalIntake(WhenSlot.Day, date, "2026-07-17T14:05:00", true))
        val res2 = useCase.buildTodaySchedule(date, config, listOf(item), intakes2)
        assertFalse(res2.slots[0].taken)
        assertNull(res2.slots[0].takenAt)
    }

    @Test
    fun `course items are filtered by start date`() {
        val date = "2026-07-17"
        val futureItem = createItem(
            id = "1", 
            medicine = "Future", 
            slots = listOf(WhenSlot.Morning),
            courseType = CourseType.Course,
            courseStart = "2026-07-18"
        )
        val pastItem = createItem(
            id = "2", 
            medicine = "Past", 
            slots = listOf(WhenSlot.Morning),
            courseType = CourseType.Course,
            courseStart = "2026-07-16"
        )
        val ongoingItem = createItem(
            id = "3", 
            medicine = "Ongoing", 
            slots = listOf(WhenSlot.Morning),
            courseType = CourseType.Ongoing
        )

        val result = useCase.buildTodaySchedule(date, config, listOf(futureItem, pastItem, ongoingItem), emptyList())

        assertEquals(1, result.slots.size)
        assertEquals(2, result.slots[0].meds.size)
        assertTrue(result.slots[0].meds.any { it.medicine == "Past" })
        assertTrue(result.slots[0].meds.any { it.medicine == "Ongoing" })
        assertFalse(result.slots[0].meds.any { it.medicine == "Future" })
    }

    @Test
    fun `dose fields map correctly including nulls`() {
        val item = createItem(
            id = "1",
            medicine = "Med",
            slots = listOf(WhenSlot.Morning),
            doseAmount = "2",
            doseUnit = DoseUnit.Tablet,
            condition = "After meal"
        )
        val item2 = createItem(
            id = "2",
            medicine = "Med 2",
            slots = listOf(WhenSlot.Morning),
            doseAmount = "1",
            doseUnit = null,
            condition = null
        )

        val result = useCase.buildTodaySchedule("2026-07-17", config, listOf(item, item2), emptyList())
        val meds = result.slots[0].meds
        
        val med1 = meds.find { it.medicine == "Med" }!!
        assertEquals("2", med1.doseAmount)
        assertEquals(DoseUnit.Tablet, med1.doseUnit)
        assertEquals("After meal", med1.condition)

        val med2 = meds.find { it.medicine == "Med 2" }!!
        assertEquals("1", med2.doseAmount)
        assertNull(med2.doseUnit)
        assertNull(med2.condition)
    }

    private fun createItem(
        id: String,
        medicine: String,
        slots: List<WhenSlot>,
        courseType: CourseType = CourseType.Ongoing,
        courseStart: String? = null,
        doseAmount: String = "1",
        doseUnit: DoseUnit? = null,
        condition: String? = null
    ) = MedicationItemReadDto(
        id = id,
        prescriptionId = "p1",
        medicine = medicine,
        condition = condition,
        whenSlots = slots,
        doseAmount = doseAmount,
        doseUnit = doseUnit,
        freqCount = 1,
        freqPeriod = 1,
        freqPeriodUnit = FreqPeriodUnit.Day,
        courseType = courseType,
        courseStart = courseStart,
        courseIntakes = null
    )
}
