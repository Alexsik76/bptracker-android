package ua.vn.home.bptracker.infra

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class InfraSanityTest {

    interface TestRepository {
        suspend fun getData(): String
    }

    @Test
    fun `coroutines and turbine work`() = runTest {
        flowOf(1, 2, 3).test {
            assertEquals(1, awaitItem())
            assertEquals(2, awaitItem())
            assertEquals(3, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `mockk works`() = runTest {
        val repository = mockk<TestRepository>()
        coEvery { repository.getData() } returns "mocked_data"

        assertEquals("mocked_data", repository.getData())
    }
}
