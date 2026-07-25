package ua.vn.home.bptracker.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ua.vn.home.bptracker.data.local.entity.PrescriptionEntity

@Dao
interface PrescriptionDao {
    @Query("SELECT * FROM prescriptions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions")
    suspend fun getAll(): List<PrescriptionEntity>

    @Query("SELECT * FROM prescriptions WHERE id = :id")
    suspend fun getById(id: String): PrescriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prescription: PrescriptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(prescriptions: List<PrescriptionEntity>)

    @Query("DELETE FROM prescriptions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM prescriptions")
    suspend fun deleteAll()
}
