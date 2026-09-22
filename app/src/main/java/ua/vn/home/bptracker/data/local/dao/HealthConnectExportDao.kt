package ua.vn.home.bptracker.data.local.dao

import androidx.room.*
import ua.vn.home.bptracker.data.local.entity.HealthConnectExportEntity

@Dao
interface HealthConnectExportDao {
    @Query("SELECT * FROM health_connect_export WHERE exportState != 'EXPORTED'")
    suspend fun getPendingList(): List<HealthConnectExportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: HealthConnectExportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(entities: List<HealthConnectExportEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoreAll(entities: List<HealthConnectExportEntity>)

    @Query("SELECT * FROM health_connect_export WHERE measurementId = :id")
    suspend fun getById(id: String): HealthConnectExportEntity?

    @Query("DELETE FROM health_connect_export WHERE measurementId = :id")
    suspend fun deleteById(id: String)
}
