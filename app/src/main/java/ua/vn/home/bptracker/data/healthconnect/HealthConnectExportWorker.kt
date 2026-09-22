package ua.vn.home.bptracker.data.healthconnect

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ua.vn.home.bptracker.core.di.ServiceLocator
import ua.vn.home.bptracker.data.local.entity.HealthConnectExportState
import ua.vn.home.bptracker.data.local.entity.SyncState

class HealthConnectExportWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val healthConnectManager = ServiceLocator.healthConnectManager
        val exportDao = ServiceLocator.database.healthConnectExportDao()
        val measurementDao = ServiceLocator.database.measurementDao()

        if (!healthConnectManager.hasPermissions()) {
            return Result.retry()
        }

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
                        } else if (measurement == null) {
                            exportDao.deleteById(item.measurementId)
                        }
                    }
                    HealthConnectExportState.PENDING_DELETE -> {
                        healthConnectManager.deleteMeasurement(item.measurementId)
                        exportDao.deleteById(item.measurementId)
                    }
                }
            } catch (_: Exception) {
                hasFailures = true
            }
        }

        return if (hasFailures) Result.retry() else Result.success()
    }

    companion object {
        private const val WORK_NAME = "HealthConnectExportWorker"

        fun enqueueWork(context: Context) {
            val request = OneTimeWorkRequestBuilder<HealthConnectExportWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
        }
    }
}
