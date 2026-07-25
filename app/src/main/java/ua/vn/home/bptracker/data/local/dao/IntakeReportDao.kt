package ua.vn.home.bptracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ua.vn.home.bptracker.data.local.entity.IntakeReportEntity
import ua.vn.home.bptracker.data.local.entity.SyncState

@Dao
interface IntakeReportDao {
    @Query("SELECT * FROM intake_reports WHERE date = :date")
    fun observeForDate(date: String): Flow<List<IntakeReportEntity>>

    @Query("SELECT * FROM intake_reports WHERE date = :date AND period = :period")
    suspend fun get(date: String, period: String): IntakeReportEntity?

    @Query("SELECT * FROM intake_reports WHERE syncState != '${SyncState.SYNCED}'")
    suspend fun getPending(): List<IntakeReportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: IntakeReportEntity)

    @Query("UPDATE intake_reports SET syncState = '${SyncState.PENDING_DELETE}' WHERE date = :date AND period = :period")
    suspend fun markPendingDelete(date: String, period: String)

    @Query("DELETE FROM intake_reports WHERE date = :date AND period = :period")
    suspend fun delete(date: String, period: String)

    @Query("DELETE FROM intake_reports WHERE date < :before")
    suspend fun deleteOld(before: String)

    @Query("DELETE FROM intake_reports")
    suspend fun deleteAll()
}
