package ua.vn.home.bptracker.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ua.vn.home.bptracker.data.api.MedicationItemApi
import ua.vn.home.bptracker.data.api.PrescriptionApi
import ua.vn.home.bptracker.data.dto.*
import ua.vn.home.bptracker.data.local.dao.MedicationItemDao
import ua.vn.home.bptracker.data.local.dao.PrescriptionDao
import ua.vn.home.bptracker.data.local.entity.toDomain
import ua.vn.home.bptracker.data.local.entity.toEntity
import ua.vn.home.bptracker.domain.model.MedicationItem
import ua.vn.home.bptracker.domain.model.Prescription

interface PrescriptionRepository {
    fun getPrescriptions(): Flow<List<Prescription>>
    suspend fun refresh()
    suspend fun createPrescription(doctor: String, prescribedOn: String): Prescription
    suspend fun updatePrescription(id: String, doctor: String?, prescribedOn: String?, isActive: Boolean?): Prescription
    suspend fun deletePrescription(id: String)

    fun getItems(prescriptionId: String): Flow<List<MedicationItem>>
    suspend fun createItem(prescriptionId: String, item: MedicationItemCreateDto): MedicationItem
    suspend fun updateItem(prescriptionId: String, itemId: String, item: MedicationItemPatchDto): MedicationItem
    suspend fun deleteItem(prescriptionId: String, itemId: String)
}

class RealPrescriptionRepository(
    private val pApi: PrescriptionApi,
    private val iApi: MedicationItemApi,
    private val pDao: PrescriptionDao,
    private val iDao: MedicationItemDao
) : PrescriptionRepository {

    override fun getPrescriptions(): Flow<List<Prescription>> {
        return pDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refresh() {
        val remotePrescriptions = pApi.getPrescriptions()
        pDao.deleteAll()
        iDao.deleteAll() // Clear all since we refresh the whole set
        
        pDao.insertAll(remotePrescriptions.map { it.toEntity() })
        
        // Sync items for each prescription
        remotePrescriptions.forEach { p ->
            val items = iApi.getItems(p.id)
            iDao.insertAll(items.map { it.toEntity() })
        }
    }

    override suspend fun createPrescription(doctor: String, prescribedOn: String): Prescription {
        val createdDto = pApi.createPrescription(PrescriptionCreateDto(doctor, prescribedOn))
        pDao.insert(createdDto.toEntity())
        return createdDto.toEntity().toDomain()
    }

    override suspend fun updatePrescription(
        id: String,
        doctor: String?,
        prescribedOn: String?,
        isActive: Boolean?
    ): Prescription {
        val updatedDto = pApi.updatePrescription(id, PrescriptionPatchDto(doctor, prescribedOn, isActive))
        pDao.insert(updatedDto.toEntity())
        return updatedDto.toEntity().toDomain()
    }

    override suspend fun deletePrescription(id: String) {
        pApi.deletePrescription(id)
        pDao.deleteById(id)
        // items are deleted via Room cascade, but we can be explicit if needed.
        // Room cascade only works if we enable foreign keys and the delete is triggered.
    }

    override fun getItems(prescriptionId: String): Flow<List<MedicationItem>> {
        return iDao.observeByPrescription(prescriptionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun createItem(prescriptionId: String, item: MedicationItemCreateDto): MedicationItem {
        val createdDto = iApi.createItem(prescriptionId, item)
        iDao.insert(createdDto.toEntity())
        return createdDto.toEntity().toDomain()
    }

    override suspend fun updateItem(
        prescriptionId: String,
        itemId: String,
        item: MedicationItemPatchDto
    ): MedicationItem {
        val updatedDto = iApi.updateItem(prescriptionId, itemId, item)
        iDao.insert(updatedDto.toEntity())
        return updatedDto.toEntity().toDomain()
    }

    override suspend fun deleteItem(prescriptionId: String, itemId: String) {
        iApi.deleteItem(prescriptionId, itemId)
        iDao.deleteById(itemId)
    }
}

class MockPrescriptionRepository : PrescriptionRepository {
    override fun getPrescriptions(): Flow<List<Prescription>> = kotlinx.coroutines.flow.flowOf(emptyList())
    override suspend fun refresh() {}
    override suspend fun createPrescription(doctor: String, prescribedOn: String): Prescription = TODO()
    override suspend fun updatePrescription(id: String, doctor: String?, prescribedOn: String?, isActive: Boolean?): Prescription = TODO()
    override suspend fun deletePrescription(id: String) {}
    override fun getItems(prescriptionId: String): Flow<List<MedicationItem>> = kotlinx.coroutines.flow.flowOf(emptyList())
    override suspend fun createItem(prescriptionId: String, item: MedicationItemCreateDto): MedicationItem = TODO()
    override suspend fun updateItem(prescriptionId: String, itemId: String, item: MedicationItemPatchDto): MedicationItem = TODO()
    override suspend fun deleteItem(prescriptionId: String, itemId: String) {}
}
