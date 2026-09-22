package ua.vn.home.bptracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ua.vn.home.bptracker.data.local.dao.*
import ua.vn.home.bptracker.data.local.entity.*

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `health_connect_export` (
                `measurementId` TEXT NOT NULL,
                `exportState` TEXT NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`measurementId`)
            )
            """.trimIndent()
        )
    }
}

@Database(
    entities = [
        MeasurementEntity::class,
        PrescriptionEntity::class,
        MedicationItemEntity::class,
        IntakeReportEntity::class,
        ReminderConfigEntity::class,
        HealthConnectExportEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class BpDatabase : RoomDatabase() {
    abstract fun measurementDao(): MeasurementDao
    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun medicationItemDao(): MedicationItemDao
    abstract fun intakeReportDao(): IntakeReportDao
    abstract fun reminderConfigDao(): ReminderConfigDao
    abstract fun healthConnectExportDao(): HealthConnectExportDao

    companion object {
        private const val DB_NAME = "bp_tracker.db"

        fun build(context: Context): BpDatabase {
            return Room.databaseBuilder(context, BpDatabase::class.java, DB_NAME)
                .addMigrations(MIGRATION_6_7)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
    }
}
