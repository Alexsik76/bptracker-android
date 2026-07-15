package ua.vn.home.bptracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ua.vn.home.bptracker.data.local.dao.MeasurementDao
import ua.vn.home.bptracker.data.local.dao.MedIntakeDao
import ua.vn.home.bptracker.data.local.dao.MedicationItemDao
import ua.vn.home.bptracker.data.local.dao.PrescriptionDao
import ua.vn.home.bptracker.data.local.entity.MeasurementEntity
import ua.vn.home.bptracker.data.local.entity.MedIntakeEntity
import ua.vn.home.bptracker.data.local.entity.MedicationItemEntity
import ua.vn.home.bptracker.data.local.entity.PrescriptionEntity

@Database(
    entities = [
        MeasurementEntity::class,
        MedIntakeEntity::class,
        PrescriptionEntity::class,
        MedicationItemEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class BpDatabase : RoomDatabase() {
    abstract fun measurementDao(): MeasurementDao
    abstract fun medIntakeDao(): MedIntakeDao
    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun medicationItemDao(): MedicationItemDao

    companion object {
        private const val DB_NAME = "bp_tracker.db"

        fun build(context: Context): BpDatabase {
            return Room.databaseBuilder(context, BpDatabase::class.java, DB_NAME)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
    }
}
