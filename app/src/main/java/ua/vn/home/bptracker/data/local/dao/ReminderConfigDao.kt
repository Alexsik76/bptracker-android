package ua.vn.home.bptracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ua.vn.home.bptracker.data.local.entity.ReminderConfigEntity

@Dao
interface ReminderConfigDao {
    @Query("SELECT * FROM reminder_config WHERE id = 0")
    fun observeConfig(): Flow<ReminderConfigEntity?>

    @Query("SELECT * FROM reminder_config WHERE id = 0")
    suspend fun getConfig(): ReminderConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ReminderConfigEntity)
}
