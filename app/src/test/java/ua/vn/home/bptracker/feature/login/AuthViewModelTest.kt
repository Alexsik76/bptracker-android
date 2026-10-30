package ua.vn.home.bptracker.feature.login

import android.net.Uri
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import ua.vn.home.bptracker.core.auth.TokenStore
import ua.vn.home.bptracker.data.api.AuthApi
import ua.vn.home.bptracker.data.api.SessionApi
import ua.vn.home.bptracker.data.api.UserApi
import ua.vn.home.bptracker.data.dto.MeResponse
import ua.vn.home.bptracker.data.dto.TokenResponse
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeTokenStore(
        access: String? = "stored_access",
        refresh: String? = "stored_refresh",
        email: String? = "cached@example.com"
    ) : TokenStore() {
        override var cachedAccessToken: String? = access
        override var cachedRefreshToken: String? = refresh
        override var cachedEmail: String? = email

        var clearCalled = false
            private set
        var savedEmail: String? = null
            private set

        override suspend fun ensureLoaded() {}
        override fun ensureLoadedBlocking() {}

        override suspend fun save(access: String, refresh: String, email: String?) {
            cachedAccessToken = access
            cachedRefreshToken = refresh
            if (email != null) {
                cachedEmail = email
                savedEmail = email
            }
        }

        override suspend fun saveEmail(email: String) {
            cachedEmail = email
            savedEmail = email
        }

        override suspend fun clear() {
            clearCalled = true
            cachedAccessToken = null
            cachedRefreshToken = null
            cachedEmail = null
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startup with no network and stored refresh token keeps LoggedIn state with cached email`() = runTest {
        val tokenStore = FakeTokenStore(
            access = "stored_access",
            refresh = "stored_refresh",
            email = "cached@example.com"
        )
        val userApi = mockk<UserApi>()
        val authApi = mockk<AuthApi>(relaxed = true)
        val sessionApi = mockk<SessionApi>(relaxed = true)

        coEvery { userApi.me() } throws IOException("Network unavailable")

        val viewModel = AuthViewModel(
            api = authApi,
            userApi = userApi,
            sessionApi = sessionApi,
            tokenStore = tokenStore
        )

        testScheduler.advanceUntilIdle()

        val currentState = viewModel.state.value
        assertTrue(currentState is AuthState.LoggedIn)
        assertEquals("cached@example.com", (currentState as AuthState.LoggedIn).email)
    }

    @Test
    fun `startup with no refresh token transitions to LoggedOut`() = runTest {
        val tokenStore = FakeTokenStore(
            access = null,
            refresh = null,
            email = null
        )
        val userApi = mockk<UserApi>()
        val authApi = mockk<AuthApi>(relaxed = true)
        val sessionApi = mockk<SessionApi>(relaxed = true)

        val viewModel = AuthViewModel(
            api = authApi,
            userApi = userApi,
            sessionApi = sessionApi,
            tokenStore = tokenStore
        )

        testScheduler.advanceUntilIdle()

        val currentState = viewModel.state.value
        assertTrue(currentState is AuthState.LoggedOut)
    }

    @Test
    fun `startup with valid session updates email from me call`() = runTest {
        val tokenStore = FakeTokenStore(
            access = "stored_access",
            refresh = "stored_refresh",
            email = "old@example.com"
        )
        val userApi = mockk<UserApi>()
        val authApi = mockk<AuthApi>(relaxed = true)
        val sessionApi = mockk<SessionApi>(relaxed = true)

        coEvery { userApi.me() } returns MeResponse(
            id = "user1",
            email = "fresh@example.com",
            createdAt = "2026-10-30T00:00:00Z"
        )

        val viewModel = AuthViewModel(
            api = authApi,
            userApi = userApi,
            sessionApi = sessionApi,
            tokenStore = tokenStore
        )

        testScheduler.advanceUntilIdle()

        val currentState = viewModel.state.value
        assertTrue(currentState is AuthState.LoggedIn)
        assertEquals("fresh@example.com", (currentState as AuthState.LoggedIn).email)
        assertEquals("fresh@example.com", tokenStore.savedEmail)
    }

    @Test
    fun `startup with 403 Forbidden clears tokens and transitions to LoggedOut`() = runTest {
        val tokenStore = FakeTokenStore(
            access = "stored_access",
            refresh = "stored_refresh",
            email = "cached@example.com"
        )
        val userApi = mockk<UserApi>()
        val authApi = mockk<AuthApi>(relaxed = true)
        val sessionApi = mockk<SessionApi>(relaxed = true)

        val errorResponse = retrofit2.Response.error<MeResponse>(
            403,
            "{\"error\":\"forbidden\"}".toResponseBody("application/json".toMediaType())
        )
        coEvery { userApi.me() } throws HttpException(errorResponse)

        val viewModel = AuthViewModel(
            api = authApi,
            userApi = userApi,
            sessionApi = sessionApi,
            tokenStore = tokenStore
        )

        testScheduler.advanceUntilIdle()

        val currentState = viewModel.state.value
        assertTrue(currentState is AuthState.LoggedOut)
        assertTrue(tokenStore.clearCalled)
    }

    @Test
    fun `confirmMagicLink token arrival cancels startup validation and signs in user`() = runTest {
        val tokenStore = FakeTokenStore(
            access = "old_access",
            refresh = "old_refresh",
            email = "old@example.com"
        )
        val userApi = mockk<UserApi>()
        val authApi = mockk<AuthApi>(relaxed = true)
        val sessionApi = mockk<SessionApi>(relaxed = true)

        coEvery { userApi.me() } returns MeResponse(
            id = "user2",
            email = "magic@example.com",
            createdAt = "2026-10-30T00:00:00Z"
        )
        coEvery { authApi.confirmMagicLink(any()) } returns TokenResponse(
            accessToken = "magic_acc",
            refreshToken = "magic_ref",
            tokenType = "Bearer",
            expiresIn = 3600
        )

        val viewModel = AuthViewModel(
            api = authApi,
            userApi = userApi,
            sessionApi = sessionApi,
            tokenStore = tokenStore
        )

        val uri = mockk<Uri>()
        every { uri.getQueryParameter("token") } returns "valid_token"

        viewModel.handleIntent(uri)

        testScheduler.advanceUntilIdle()

        val currentState = viewModel.state.value
        assertTrue(currentState is AuthState.LoggedIn)
        assertEquals("magic@example.com", (currentState as AuthState.LoggedIn).email)
        assertEquals("magic_acc", tokenStore.cachedAccessToken)
        assertEquals("magic_ref", tokenStore.cachedRefreshToken)
    }
}
