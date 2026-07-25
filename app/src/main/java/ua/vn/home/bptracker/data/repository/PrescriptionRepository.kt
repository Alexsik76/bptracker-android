package ua.vn.home.bptracker.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ua.vn.home.bptracker.data.api.MedicationItemApi
import ua.vn.home.bptracker.data.api.PrescriptionApi
import ua.vn.home.bptracker.data.dto.*
import ua.vn.home.bptracker.data.local.BpDatabase
import ua.vn.home.bptracker.data.local.dao.MedicationItemDao
import ua.vn.home.bptracker.data.local.dao.PrescriptionDao
import ua.vn.home.bptracker.data.local.entity.toDto
import ua.vn.home.bptracker.data.local.entity.toEntity

interface PrescriptionRepository {
    fun getPrescriptions(): Flow<List<PrescriptionReadDto>>
    suspend fun refresh()
    suspend fun createPrescription(doctor: String, prescribedOn: String): PrescriptionReadDto
    suspend fun updatePrescription(id: String, doctor: String?, prescribedOn: String?, isActive: Boolean?): PrescriptionReadDto
    suspend fun deletePrescription(id: String)

    fun getItems(prescriptionId: String): Flow<List<MedicationItemReadDto>>
    suspend fun createItem(prescriptionId: String, item: MedicationItemCreateDto): MedicationItemReadDto
    suspend fun updateItem(prescriptionId: String, itemId: String, item: MedicationItemPatchDto): MedicationItemReadDto
    suspend fun deleteItem(prescriptionId: String, itemId: String)
}

class RealPrescriptionRepository(
    private val db: BpDatabase,
    private val pApi: PrescriptionApi,
    private val iApi: MedicationItemApi,
    private val pDao: PrescriptionDao,
    private val iDao: MedicationItemDao
) : PrescriptionRepository {

    override fun getPrescriptions(): Flow<List<PrescriptionReadDto>> {
        return pDao.observeAll().map { entities ->
            entities.map { it.toDto() }
        }
    }

    override suspend fun refresh() {
        // Fetch all data from network first
        val remotePrescriptions = pApi.getPrescriptions()
        val allItems = remotePrescriptions.flatMap { p ->
            iApi.getItems(p.id)
        }

        // Apply to cache atomically
        db.withTransaction {
            pDao.deleteAll()
            iDao.deleteAll()
            
            pDao.insertAll(remotePrescriptions.map { it.toEntity() })
            iDao.insertAll(allItems.map { it.toEntity() })
        }
    }

    override suspend fun createPrescription(doctor: String, prescribedOn: String): PrescriptionReadDto {
        val createdDto = pApi.createPrescription(PrescriptionCreateDto(doctor, prescribedOn))
        pDao.insert(createdDto.toEntity())
        return createdDto
    }

    override suspend fun updatePrescription(
        id: String,
        doctor: String?,
        prescribedOn: String?,
        isActive: Boolean?
    ): PrescriptionReadDto {
        val updatedDto = pApi.updatePrescription(id, PrescriptionPatchDto(doctor, prescribedOn, isActive))
        pDao.insert(updatedDto.toEntity())
        return updatedDto
    }

    override suspend fun deletePrescription(id: String) {
        pApi.deletePrescription(id)
        pDao.deleteById(id)
    }

    override fun getItems(prescriptionId: String): Flow<List<MedicationItemReadDto>> {
        return iDao.observeByPrescription(prescriptionId).map { entities ->
            entities.map { it.toDto() }
        }
    }

    override suspend fun createItem(prescriptionId: String, item: MedicationItemCreateDto): MedicationItemReadDto {
        val createdDto = iApi.createItem(prescriptionId, item)
        iDao.insert(createdDto.toEntity())
        return createdDto
    }

    override suspend fun updateItem(
        prescriptionId: String,
        itemId: String,
        item: MedicationItemPatchDto
    ): MedicationItemReadDto {
        val updatedDto = iApi.updateItem(prescriptionId, itemId, item)
        iDao.insert(updatedDto.toEntity())
        return updatedDto
    }

    override suspend fun deleteItem(prescriptionId: String, itemId: String) {
        iApi.deleteItem(prescriptionId, itemId)
        iDao.deleteById(itemId)
    }
}

class MockPrescriptionRepository : PrescriptionRepository {
    override fun getPrescriptions(): Flow<List<PrescriptionReadDto>> = kotlinx.coroutines.flow.flowOf(emptyList())
    override suspend fun refresh() {}
    override suspend fun createPrescription(doctor: String, prescribedOn: String): PrescriptionReadDto {
        return PrescriptionReadDto("mock", doctor, prescribedOn, true, "2026-07-15T00:00:00Z")
    }
    override suspend fun updatePrescription(id: String, doctor: String?, prescribedOn: String?, isActive: Boolean?): PrescriptionReadDto {
        return PrescriptionReadDto(id, doctor ?: "Dr. Mock", prescribedOn ?: "2026-07-15", isActive ?: true, "2026-07-15T00:00:00Z")
    }
    override suspend fun deletePrescription(id: String) {}
    override fun getItems(prescriptionId: String): Flow<List<MedicationItemReadDto>> = kotlinx.coroutines.flow.flowOf(emptyList())
    override suspend fun createItem(prescriptionId: String, item: MedicationItemCreateDto): MedicationItemReadDto {
        return MedicationItemReadDto("mock-item", prescriptionId, item.medicine, item.condition, item.whenSlots, item.doseAmount, item.doseUnit, item.freqCount, item.freqPeriod, item.freqPeriodUnit, item.courseType, item.courseStart, item.courseIntakes)
    }
    override suspend fun updateItem(prescriptionId: String, itemId: String, item: MedicationItemPatchDto): MedicationItemReadDto {
        return MedicationItemReadDto(itemId, prescriptionId, item.medicine ?: "Medicine", item.condition, item.whenSlots ?: emptyList(), item.doseAmount ?: "1", item.doseUnit, item.freqCount ?: 1, item.freqPeriod ?: 1, item.freqPeriodUnit ?: FreqPeriodUnit.Day, item.courseType ?: CourseType.Ongoing, item.courseStart, item.courseIntakes)
    }
    override suspend fun deleteItem(prescriptionId: String, itemId: String) {}
}
