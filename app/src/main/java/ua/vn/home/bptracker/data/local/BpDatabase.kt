package ua.vn.home.bptracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ua.vn.home.bptracker.data.local.dao.*
import ua.vn.home.bptracker.data.local.entity.*

@Database(
    entities = [
        MeasurementEntity::class,
        MedIntakeEntity::class,
        PrescriptionEntity::class,
        MedicationItemEntity::class,
        IntakeReportEntity::class,
        ReminderConfigEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class BpDatabase : RoomDatabase() {
    abstract fun measurementDao(): MeasurementDao
    abstract fun medIntakeDao(): MedIntakeDao
    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun medicationItemDao(): MedicationItemDao
    abstract fun intakeReportDao(): IntakeReportDao
    abstract fun reminderConfigDao(): ReminderConfigDao

    companion object {
        private const val DB_NAME = "bp_tracker.db"

        fun build(context: Context): BpDatabase {
            return Room.databaseBuilder(context, BpDatabase::class.java, DB_NAME)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
    }
}
