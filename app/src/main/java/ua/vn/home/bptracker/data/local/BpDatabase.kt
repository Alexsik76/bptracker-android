package ua.vn.home.bptracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ua.vn.home.bptracker.core.utils.TimeUtils
import ua.vn.home.bptracker.data.local.dao.*
import ua.vn.home.bptracker.data.local.entity.*

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

data class ConvertedMeasurementRow(
    val id: String,
    val recordedAtMs: Long,
    val sys: Int,
    val dia: Int,
    val pulse: Int,
    val syncState: String
)

fun convertMeasurementRowForMigration(
    id: String,
    recordedAt: String,
    sys: Int,
    dia: Int,
    pulse: Int,
    syncState: String,
    migrationTimeMs: Long
): ConvertedMeasurementRow? {
    val instant = TimeUtils.parseInstantOrNull(recordedAt)
    val recordedAtMs = if (instant != null) {
        instant.toEpochMilli()
    } else {
        if (syncState == SyncState.SYNCED) {
            return null
        }
        migrationTimeMs
    }
    return ConvertedMeasurementRow(
        id = id,
        recordedAtMs = recordedAtMs,
        sys = sys,
        dia = dia,
        pulse = pulse,
        syncState = syncState
    )
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `measurements_new` (
                `id` TEXT NOT NULL,
                `recordedAtMs` INTEGER NOT NULL,
                `sys` INTEGER NOT NULL,
                `dia` INTEGER NOT NULL,
                `pulse` INTEGER NOT NULL,
                `syncState` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        val migrationTimeMs = System.currentTimeMillis()
        val cursor = db.query("SELECT `id`, `recordedAt`, `sys`, `dia`, `pulse`, `syncState` FROM `measurements`")
        val convertedRows = mutableListOf<ConvertedMeasurementRow>()
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow("id")
            val recIdx = c.getColumnIndexOrThrow("recordedAt")
            val sysIdx = c.getColumnIndexOrThrow("sys")
            val diaIdx = c.getColumnIndexOrThrow("dia")
            val pulseIdx = c.getColumnIndexOrThrow("pulse")
            val syncIdx = c.getColumnIndexOrThrow("syncState")

            while (c.moveToNext()) {
                val converted = convertMeasurementRowForMigration(
                    id = c.getString(idIdx),
                    recordedAt = c.getString(recIdx),
                    sys = c.getInt(sysIdx),
                    dia = c.getInt(diaIdx),
                    pulse = c.getInt(pulseIdx),
                    syncState = c.getString(syncIdx),
                    migrationTimeMs = migrationTimeMs
                )
                if (converted != null) {
                    convertedRows.add(converted)
                }
            }
        }

        val stmt = db.compileStatement(
            "INSERT INTO `measurements_new` (`id`, `recordedAtMs`, `sys`, `dia`, `pulse`, `syncState`) VALUES (?, ?, ?, ?, ?, ?)"
        )
        try {
            for (row in convertedRows) {
                stmt.bindString(1, row.id)
                stmt.bindLong(2, row.recordedAtMs)
                stmt.bindLong(3, row.sys.toLong())
                stmt.bindLong(4, row.dia.toLong())
                stmt.bindLong(5, row.pulse.toLong())
                stmt.bindString(6, row.syncState)
                stmt.executeInsert()
            }
        } finally {
            stmt.close()
        }

        db.execSQL("DROP TABLE `measurements`")
        db.execSQL("ALTER TABLE `measurements_new` RENAME TO `measurements`")
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
    version = 8,
    exportSchema = true
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
                .addMigrations(MIGRATION_6_7, MIGRATION_7_8)
                .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3, 4, 5)
                .build()
        }
    }
}
