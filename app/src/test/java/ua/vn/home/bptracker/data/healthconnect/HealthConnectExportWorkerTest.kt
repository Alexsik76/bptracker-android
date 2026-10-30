package ua.vn.home.bptracker.data.healthconnect

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ua.vn.home.bptracker.core.config.SettingsStore
import ua.vn.home.bptracker.data.local.dao.HealthConnectExportDao
import ua.vn.home.bptracker.data.local.dao.MeasurementDao
import ua.vn.home.bptracker.data.local.entity.HealthConnectExportEntity
import ua.vn.home.bptracker.data.local.entity.HealthConnectExportState
import ua.vn.home.bptracker.data.local.entity.MeasurementEntity
import ua.vn.home.bptracker.data.local.entity.SyncState

class HealthConnectExportWorkerTest {

    private val context = mockk<Context>(relaxed = true)
    private val workerParams = mockk<WorkerParameters>(relaxed = true)
    private val healthConnectManager = mockk<HealthConnectManager>(relaxed = true)
    private val exportDao = mockk<HealthConnectExportDao>(relaxed = true)
    private val measurementDao = mockk<MeasurementDao>(relaxed = true)
    private val settingsStore = mockk<SettingsStore>(relaxed = true)

    @Before
    fun setUp() {
        HealthConnectExportWorker.clearFailureCounts()
    }

    @After
    fun tearDown() {
        HealthConnectExportWorker.clearFailureCounts()
    }

    @Test
    fun `worker without permission returns success and does not retry`() = runTest {
        every { settingsStore.healthConnectEnabled } returns flowOf(true)
        coEvery { healthConnectManager.hasPermissions() } returns false

        val worker = HealthConnectExportWorker(
            appContext = context,
            params = workerParams,
            healthConnectManagerOverride = healthConnectManager,
            exportDaoOverride = exportDao,
            measurementDaoOverride = measurementDao,
            settingsStoreOverride = settingsStore
        )

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { exportDao.getPendingList() }
    }

    @Test
    fun `worker when health connect setting disabled returns success`() = runTest {
        every { settingsStore.healthConnectEnabled } returns flowOf(false)

        val worker = HealthConnectExportWorker(
            appContext = context,
            params = workerParams,
            healthConnectManagerOverride = healthConnectManager,
            exportDaoOverride = exportDao,
            measurementDaoOverride = measurementDao,
            settingsStoreOverride = settingsStore
        )

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { healthConnectManager.hasPermissions() }
    }

    @Test
    fun `worker gives up on record after 3 failures and returns success`() = runTest {
        every { settingsStore.healthConnectEnabled } returns flowOf(true)
        coEvery { healthConnectManager.hasPermissions() } returns true

        val failingItem = HealthConnectExportEntity(
            measurementId = "bad_meas_1",
            exportState = HealthConnectExportState.PENDING_UPSERT
        )
        coEvery { exportDao.getPendingList() } returns listOf(failingItem)
        coEvery { measurementDao.getById("bad_meas_1") } returns MeasurementEntity(
            id = "bad_meas_1",
            recordedAtMs = 12345678L,
            sys = 120,
            dia = 80,
            pulse = 70,
            syncState = SyncState.SYNCED
        )
        coEvery { healthConnectManager.exportMeasurement(any()) } throws RuntimeException("HC error")

        val worker = HealthConnectExportWorker(
            appContext = context,
            params = workerParams,
            healthConnectManagerOverride = healthConnectManager,
            exportDaoOverride = exportDao,
            measurementDaoOverride = measurementDao,
            settingsStoreOverride = settingsStore
        )

        // Attempt 1: retry
        val result1 = worker.doWork()
        assertEquals(ListenableWorker.Result.retry(), result1)

        // Attempt 2: retry
        val result2 = worker.doWork()
        assertEquals(ListenableWorker.Result.retry(), result2)

        // Attempt 3: 3rd failure -> record deleted from queue -> success
        val result3 = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result3)
        coVerify(exactly = 1) { exportDao.deleteById("bad_meas_1") }
    }
}
