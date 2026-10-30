package ua.vn.home.bptracker.data.healthconnect

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import ua.vn.home.bptracker.core.config.SettingsStore
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.local.dao.HealthConnectExportDao
import ua.vn.home.bptracker.data.local.dao.MeasurementDao
import ua.vn.home.bptracker.data.local.entity.HealthConnectExportState
import ua.vn.home.bptracker.data.local.entity.SyncState
import java.util.concurrent.ConcurrentHashMap

class HealthConnectExportWorker(
    appContext: Context,
    params: WorkerParameters,
    private val healthConnectManagerOverride: HealthConnectManager? = null,
    private val exportDaoOverride: HealthConnectExportDao? = null,
    private val measurementDaoOverride: MeasurementDao? = null,
    private val settingsStoreOverride: SettingsStore? = null,
) : CoroutineWorker(appContext, params) {

    constructor(appContext: Context, params: WorkerParameters) : this(
        appContext,
        params,
        null,
        null,
        null,
        null
    )

    private fun getHealthConnectManager(): HealthConnectManager =
        healthConnectManagerOverride ?: ServiceLocator.healthConnectManager

    private fun getExportDao(): HealthConnectExportDao =
        exportDaoOverride ?: ServiceLocator.database.healthConnectExportDao()

    private fun getMeasurementDao(): MeasurementDao =
        measurementDaoOverride ?: ServiceLocator.database.measurementDao()

    private fun getSettingsStore(): SettingsStore =
        settingsStoreOverride ?: ServiceLocator.settingsStore

    override suspend fun doWork(): Result {
        val settingsStore = getSettingsStore()
        if (!settingsStore.healthConnectEnabled.first()) {
            return Result.success()
        }

        val healthConnectManager = getHealthConnectManager()
        if (!healthConnectManager.hasPermissions()) {
            return Result.success()
        }

        val exportDao = getExportDao()
        val measurementDao = getMeasurementDao()

        val pending = exportDao.getPendingList()
        var hasFailures = false

        for (item in pending) {
            try {
                when (item.exportState) {
                    HealthConnectExportState.PENDING_UPSERT -> {
                        val measurement = measurementDao.getById(item.measurementId)
                        if (measurement != null && measurement.syncState == SyncState.SYNCED) {
                            healthConnectManager.exportMeasurement(measurement)
                            exportDao.insertOrUpdate(
                                item.copy(
                                    exportState = HealthConnectExportState.EXPORTED,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                            failureCounts.remove(item.measurementId)
                        } else if (measurement == null) {
                            exportDao.deleteById(item.measurementId)
                            failureCounts.remove(item.measurementId)
                        }
                    }
                    HealthConnectExportState.PENDING_DELETE -> {
                        healthConnectManager.deleteMeasurement(item.measurementId)
                        exportDao.deleteById(item.measurementId)
                        failureCounts.remove(item.measurementId)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val failures = (failureCounts[item.measurementId] ?: 0) + 1
                if (failures >= MAX_RECORD_FAILURES) {
                    failureCounts.remove(item.measurementId)
                    exportDao.deleteById(item.measurementId)
                } else {
                    failureCounts[item.measurementId] = failures
                    hasFailures = true
                }
            }
        }

        return if (hasFailures) Result.retry() else Result.success()
    }

    companion object {
        private const val WORK_NAME = "HealthConnectExportWorker"
        private const val MAX_RECORD_FAILURES = 3
        private val failureCounts = ConcurrentHashMap<String, Int>()

        @VisibleForTesting
        fun clearFailureCounts() {
            failureCounts.clear()
        }

        fun enqueueWork(context: Context) {
            val request = OneTimeWorkRequestBuilder<HealthConnectExportWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
