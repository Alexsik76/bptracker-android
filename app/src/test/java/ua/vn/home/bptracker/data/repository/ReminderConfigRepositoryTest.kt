package ua.vn.home.bptracker.data.repository

import io.mockk.*
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import ua.vn.home.bptracker.data.api.ReminderConfigApi
import ua.vn.home.bptracker.data.dto.ReminderConfigDto
import ua.vn.home.bptracker.data.local.dao.ReminderConfigDao
import ua.vn.home.bptracker.data.local.entity.toEntity

class ReminderConfigRepositoryTest {

    private val api = mockk<ReminderConfigApi>()
    private val dao = mockk<ReminderConfigDao>(relaxed = true)
    private lateinit var repository: RealReminderConfigRepository

    @Before
    fun setup() {
        repository = RealReminderConfigRepository(api, dao)
    }

    @Test
    fun `getConfig success saves to cache`() = runTest {
        val config = ReminderConfigDto("08:00", "14:00", "20:00", 3, 60)
        coEvery { api.getConfig() } returns config

        val result = repository.getConfig()

        assertEquals(config, result)
        coVerify { dao.upsert(config.toEntity()) }
    }

    @Test
    fun `getConfig returns null on 404 error`() = runTest {
        val response = Response.error<ReminderConfigDto>(404, "".toResponseBody())
        coEvery { api.getConfig() } throws HttpException(response)

        val result = repository.getConfig()

        assertNull(result)
        coVerify(exactly = 0) { dao.upsert(any()) }
    }

    @Test(expected = HttpException::class)
    fun `getConfig rethrows non-404 error`() = runTest {
        val response = Response.error<ReminderConfigDto>(500, "".toResponseBody())
        coEvery { api.getConfig() } throws HttpException(response)

        repository.getConfig()
    }

    @Test
    fun `saveConfig updates remote and cache`() = runTest {
        val config = ReminderConfigDto("08:00", "14:00", "20:00", 3, 60)
        coEvery { api.saveConfig(config) } returns config

        val result = repository.saveConfig(config)

        assertEquals(config, result)
        coVerify { api.saveConfig(config) }
        coVerify { dao.upsert(config.toEntity()) }
    }
}
