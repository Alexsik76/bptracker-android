package ua.vn.home.bptracker.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ua.vn.home.bptracker.data.local.entity.MedIntakeEntity

@Dao
interface MedIntakeDao {
    @Query("SELECT * FROM med_intakes WHERE date = :date ORDER BY time ASC")
    fun getByDateFlow(date: String): Flow<List<MedIntakeEntity>>

    @Query("SELECT * FROM med_intakes WHERE date = :date ORDER BY time ASC")
    suspend fun getByDate(date: String): List<MedIntakeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(intakes: List<MedIntakeEntity>)

    @Query("UPDATE med_intakes SET status = :status, timeTaken = :timeTaken WHERE date = :date AND period = :period")
    suspend fun updateStatus(date: String, period: String, status: String, timeTaken: String?)

    @Query("DELETE FROM med_intakes WHERE date < :beforeDate")
    suspend fun deleteOld(beforeDate: String)

    @Query("DELETE FROM med_intakes")
    suspend fun deleteAll()

    @Query("DELETE FROM med_intakes WHERE date = :date AND period = :period")
    suspend fun deleteByDateAndPeriod(date: String, period: String)
}
