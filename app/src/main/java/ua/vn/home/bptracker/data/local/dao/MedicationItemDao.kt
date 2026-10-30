package ua.vn.home.bptracker.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ua.vn.home.bptracker.data.local.entity.MedicationItemEntity

@Dao
interface MedicationItemDao {
    @Query("SELECT * FROM medication_items WHERE prescriptionId = :prescriptionId")
    fun observeByPrescription(prescriptionId: String): Flow<List<MedicationItemEntity>>

    @Query("SELECT * FROM medication_items WHERE prescriptionId = :prescriptionId")
    suspend fun getByPrescription(prescriptionId: String): List<MedicationItemEntity>

    @Query("SELECT * FROM medication_items WHERE id = :id")
    suspend fun getById(id: String): MedicationItemEntity?

    @Upsert
    suspend fun upsert(item: MedicationItemEntity)

    @Upsert
    suspend fun upsertAll(items: List<MedicationItemEntity>)

    @Query("DELETE FROM medication_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM medication_items WHERE prescriptionId = :prescriptionId")
    suspend fun deleteByPrescription(prescriptionId: String)

    @Query("DELETE FROM medication_items")
    suspend fun deleteAll()
}
