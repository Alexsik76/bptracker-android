package ua.vn.home.bptracker.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ua.vn.home.bptracker.data.local.entity.MeasurementEntity

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements ORDER BY recordedAt DESC")
    fun getAllFlow(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements ORDER BY recordedAt DESC")
    suspend fun getAll(): List<MeasurementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: MeasurementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(measurements: List<MeasurementEntity>)

    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM measurements WHERE isSynced = 0")
    suspend fun getUnsynced(): List<MeasurementEntity>

    @Query("DELETE FROM measurements")
    suspend fun deleteAll()
}
