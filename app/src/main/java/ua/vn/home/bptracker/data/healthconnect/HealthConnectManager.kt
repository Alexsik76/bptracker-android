package ua.vn.home.bptracker.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Pressure
import ua.vn.home.bptracker.data.local.entity.MeasurementEntity
import java.time.OffsetDateTime
import java.time.ZoneId

// Increment whenever the record mapping or transformation logic changes to overwrite previously exported entries.
private const val EXPORT_REVISION = 1L

class HealthConnectManager(private val context: Context) {

    val requiredPermissions = setOf(
        HealthPermission.getWritePermission(BloodPressureRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class)
    )

    fun isAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    private fun getClient(): HealthConnectClient? {
        if (!isAvailable()) return null
        return HealthConnectClient.getOrCreate(context)
    }

    suspend fun hasPermissions(): Boolean {
        val client = getClient() ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(requiredPermissions)
    }

    suspend fun exportMeasurement(measurement: MeasurementEntity) {
        val client = getClient() ?: throw IllegalStateException("HealthConnect client unavailable")
        if (!hasPermissions()) throw IllegalStateException("HealthConnect permissions not granted")

        val instant = OffsetDateTime.parse(measurement.recordedAt).toInstant()
        val zoneOffset = ZoneId.systemDefault().rules.getOffset(instant)
        val version = instant.toEpochMilli() + EXPORT_REVISION

        val bpMetadata = Metadata.manualEntry(
            clientRecordId = "bp-${measurement.id}",
            clientRecordVersion = version
        )
        val bpRecord = BloodPressureRecord(
            time = instant,
            zoneOffset = zoneOffset,
            systolic = Pressure.millimetersOfMercury(measurement.sys.toDouble()),
            diastolic = Pressure.millimetersOfMercury(measurement.dia.toDouble()),
            metadata = bpMetadata
        )

        val records = mutableListOf<Record>(bpRecord)

        if (measurement.pulse > 0) {
            val hrMetadata = Metadata.manualEntry(
                clientRecordId = "hr-${measurement.id}",
                clientRecordVersion = version
            )
            val hrRecord = HeartRateRecord(
                startTime = instant,
                startZoneOffset = zoneOffset,
                endTime = instant.plusSeconds(1),
                endZoneOffset = zoneOffset,
                samples = listOf(
                    HeartRateRecord.Sample(
                        time = instant,
                        beatsPerMinute = measurement.pulse.toLong()
                    )
                ),
                metadata = hrMetadata
            )
            records.add(hrRecord)
        }

        client.insertRecords(records)
    }

    suspend fun deleteMeasurement(measurementId: String) {
        val client = getClient() ?: throw IllegalStateException("HealthConnect client unavailable")
        if (!hasPermissions()) throw IllegalStateException("HealthConnect permissions not granted")

        client.deleteRecords(
            recordType = BloodPressureRecord::class,
            recordIdsList = emptyList(),
            clientRecordIdsList = listOf("bp-$measurementId")
        )
        client.deleteRecords(
            recordType = HeartRateRecord::class,
            recordIdsList = emptyList(),
            clientRecordIdsList = listOf("hr-$measurementId")
        )
    }
}
