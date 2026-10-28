package ua.vn.home.bptracker.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ua.vn.home.bptracker.data.local.entity.MeasurementEntity
import ua.vn.home.bptracker.data.local.entity.SyncState

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements WHERE syncState != '${SyncState.PENDING_DELETE}' ORDER BY recordedAtMs DESC")
    fun getAllFlow(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE syncState != '${SyncState.PENDING_DELETE}' ORDER BY recordedAtMs DESC")
    suspend fun getAll(): List<MeasurementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: MeasurementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(measurements: List<MeasurementEntity>)

    @Transaction
    suspend fun upsertSynced(measurements: List<MeasurementEntity>) {
        if (measurements.isEmpty()) return
        val nonSyncedIds = getPending().map { it.id }.toSet()
        val toInsert = measurements.filter { it.id !in nonSyncedIds }
        if (toInsert.isNotEmpty()) {
            insertAll(toInsert)
        }
    }

    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM measurements WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT * FROM measurements WHERE syncState != '${SyncState.SYNCED}'")
    suspend fun getPending(): List<MeasurementEntity>

    @Query("SELECT * FROM measurements WHERE id = :id")
    suspend fun getById(id: String): MeasurementEntity?

    @Query("SELECT * FROM measurements WHERE syncState = '${SyncState.SYNCED}' ORDER BY recordedAtMs DESC")
    suspend fun getAllSynced(): List<MeasurementEntity>

    @Query("SELECT id FROM measurements WHERE syncState = '${SyncState.SYNCED}' AND recordedAtMs >= :windowStart")
    suspend fun getSyncedIds(windowStart: Long): List<String>

    @Query("SELECT id FROM measurements WHERE syncState = '${SyncState.SYNCED}' AND (:dateFrom IS NULL OR recordedAtMs >= :dateFrom) AND (:dateTo IS NULL OR recordedAtMs < :dateTo)")
    suspend fun getSyncedIdsInRange(dateFrom: Long?, dateTo: Long?): List<String>

    @Query("UPDATE measurements SET syncState = '${SyncState.PENDING_DELETE}' WHERE id = :id")
    suspend fun markPendingDelete(id: String)
}
