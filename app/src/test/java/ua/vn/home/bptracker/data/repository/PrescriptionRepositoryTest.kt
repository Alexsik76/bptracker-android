package ua.vn.home.bptracker.data.repository

import androidx.room.withTransaction
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ua.vn.home.bptracker.data.api.MedicationItemApi
import ua.vn.home.bptracker.data.api.PrescriptionApi
import ua.vn.home.bptracker.data.dto.PrescriptionReadDto
import ua.vn.home.bptracker.data.local.BpDatabase
import ua.vn.home.bptracker.data.local.dao.MedicationItemDao
import ua.vn.home.bptracker.data.local.dao.PrescriptionDao
import ua.vn.home.bptracker.data.local.entity.toEntity

class PrescriptionRepositoryTest {

    private val db = mockk<BpDatabase>()
    private val pApi = mockk<PrescriptionApi>()
    private val iApi = mockk<MedicationItemApi>()
    private val pDao = mockk<PrescriptionDao>(relaxed = true)
    private val iDao = mockk<MedicationItemDao>(relaxed = true)

    private lateinit var repository: RealPrescriptionRepository

    @Before
    fun setup() {
        repository = RealPrescriptionRepository(db, pApi, iApi, pDao, iDao)
        
        // Mock withTransaction to just execute the block
        mockkStatic("androidx.room.RoomDatabaseKt")
        val transactionBlock = slot<suspend () -> Any>()
        coEvery { db.withTransaction(capture(transactionBlock)) } coAnswers {
            transactionBlock.captured.invoke()
        }
    }

    @Test
    fun `refresh fetches data from network and updates cache atomically`() = runTest {
        val remotePrescription = PrescriptionReadDto("p1", "Dr. Smith", "2026-07-20", true, "now")
        coEvery { pApi.getPrescriptions() } returns listOf(remotePrescription)
        coEvery { iApi.getItems("p1") } returns emptyList()

        repository.refresh()

        coVerify { pApi.getPrescriptions() }
        coVerify { iApi.getItems("p1") }
        
        coVerifyOrder {
            pDao.deleteAll()
            iDao.deleteAll()
            pDao.insertAll(any())
            iDao.insertAll(any())
        }
    }

    @Test
    fun `getPrescriptions observes DAO and maps to DTOs`() = runTest {
        val entity = PrescriptionReadDto("p1", "Dr. Smith", "2026-07-20", true, "now").toEntity()
        every { pDao.observeAll() } returns flowOf(listOf(entity))

        val result = repository.getPrescriptions().first()

        assertEquals(1, result.size)
        assertEquals("p1", result[0].id)
    }

    @Test
    fun `deletePrescription calls API and then DAO`() = runTest {
        coEvery { pApi.deletePrescription("p1") } just Runs
        
        repository.deletePrescription("p1")

        coVerify { pApi.deletePrescription("p1") }
        coVerify { pDao.deleteById("p1") }
    }
}
