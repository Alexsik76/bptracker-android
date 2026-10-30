package ua.vn.home.bptracker.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ua.vn.home.bptracker.data.local.dao.MedicationItemDao
import ua.vn.home.bptracker.data.local.dao.PrescriptionDao
import ua.vn.home.bptracker.data.local.entity.MedicationItemEntity
import ua.vn.home.bptracker.data.local.entity.PrescriptionEntity

@RunWith(RobolectricTestRunner::class)
class PrescriptionRoomTest {

    private lateinit var db: BpDatabase
    private lateinit var prescriptionDao: PrescriptionDao
    private lateinit var medicationItemDao: MedicationItemDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, BpDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prescriptionDao = db.prescriptionDao()
        medicationItemDao = db.medicationItemDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `updating a prescription keeps its medication items`() = runTest {
        val prescription = PrescriptionEntity(
            id = "rx-1",
            doctor = "Dr. House",
            prescribedOn = "2026-05-01",
            isActive = true,
            createdAt = "2026-05-01T10:00:00Z"
        )
        prescriptionDao.upsert(prescription)

        val medItem1 = MedicationItemEntity(
            id = "med-1",
            prescriptionId = "rx-1",
            medicine = "Amlodipine",
            condition = "Hypertension",
            whenSlotsJson = "[\"Morning\"]",
            doseAmount = "5",
            doseUnitJson = "\"Mg\"",
            freqCount = 1,
            freqPeriod = 1,
            freqPeriodUnitJson = "\"Day\"",
            courseTypeJson = "\"Ongoing\"",
            courseStart = null,
            courseIntakes = null
        )
        val medItem2 = MedicationItemEntity(
            id = "med-2",
            prescriptionId = "rx-1",
            medicine = "Bisoprolol",
            condition = "Tachycardia",
            whenSlotsJson = "[\"Morning\"]",
            doseAmount = "2.5",
            doseUnitJson = "\"Mg\"",
            freqCount = 1,
            freqPeriod = 1,
            freqPeriodUnitJson = "\"Day\"",
            courseTypeJson = "\"Ongoing\"",
            courseStart = null,
            courseIntakes = null
        )
        medicationItemDao.upsertAll(listOf(medItem1, medItem2))

        val itemsBefore = medicationItemDao.getByPrescription("rx-1")
        assertEquals(2, itemsBefore.size)

        // Update the prescription entity (e.g. change doctor and active state)
        val updatedPrescription = prescription.copy(
            doctor = "Dr. Cuddy",
            isActive = false
        )
        prescriptionDao.upsert(updatedPrescription)

        // Verify that medication items are still present and were not deleted by ON DELETE CASCADE
        val itemsAfter = medicationItemDao.getByPrescription("rx-1")
        assertEquals(2, itemsAfter.size)
        assertEquals(listOf("med-1", "med-2"), itemsAfter.map { it.id }.sorted())

        val savedRx = prescriptionDao.getById("rx-1")
        assertNotNull(savedRx)
        assertEquals("Dr. Cuddy", savedRx?.doctor)
        assertEquals(false, savedRx?.isActive)
    }
}
