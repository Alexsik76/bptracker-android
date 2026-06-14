package ua.vn.home.bptracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ua.vn.home.bptracker.data.local.dao.MeasurementDao
import ua.vn.home.bptracker.data.local.dao.MedIntakeDao
import ua.vn.home.bptracker.data.local.entity.MeasurementEntity
import ua.vn.home.bptracker.data.local.entity.MedIntakeEntity

@Database(entities = [MeasurementEntity::class, MedIntakeEntity::class], version = 1, exportSchema = false)
abstract class BpDatabase : RoomDatabase() {
    abstract fun measurementDao(): MeasurementDao
    abstract fun medIntakeDao(): MedIntakeDao

    companion object {
        private const val DB_NAME = "bp_tracker.db"

        fun build(context: Context): BpDatabase {
            return Room.databaseBuilder(context, BpDatabase::class.java, DB_NAME)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
