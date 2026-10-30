package ua.vn.home.bptracker.core.network

import io.mockk.coEvery
import io.mockk.mockk
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import ua.vn.home.bptracker.core.auth.TokenStore
import ua.vn.home.bptracker.data.api.AuthApi
import ua.vn.home.bptracker.data.dto.RefreshRequest
import ua.vn.home.bptracker.data.dto.TokenResponse
import java.io.IOException

class TokenAuthenticatorTest {

    private class FakeTokenStore(
        initialAccess: String? = "old_access",
        initialRefresh: String? = "old_refresh"
    ) : TokenStore() {
        override var cachedAccessToken: String? = initialAccess
        override var cachedRefreshToken: String? = initialRefresh

        var clearCalled = false
            private set
        var notifySessionExpiredCalled = false
            private set
        var savedAccess: String? = null
            private set
        var savedRefresh: String? = null
            private set

        override suspend fun ensureLoaded() {}
        override fun ensureLoadedBlocking() {}

        override suspend fun save(access: String, refresh: String, email: String?) {
            cachedAccessToken = access
            cachedRefreshToken = refresh
            savedAccess = access
            savedRefresh = refresh
        }

        override suspend fun clear() {
            clearCalled = true
            cachedAccessToken = null
            cachedRefreshToken = null
        }

        override fun notifySessionExpired() {
            notifySessionExpiredCalled = true
        }
    }

    private fun create401Response(authorizationHeader: String = "Bearer old_access"): Response {
        val request = Request.Builder()
            .url("https://api.example.com/user/me")
            .header("Authorization", authorizationHeader)
            .build()
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .build()
    }

    private fun createHttpException(code: Int): HttpException {
        val response = retrofit2.Response.error<TokenResponse>(
            code,
            "{\"error\":\"rejected\"}".toResponseBody("application/json".toMediaType())
        )
        return HttpException(response)
    }

    @Test
    fun `401 from refresh clears tokens and notifies session expired`() {
        val tokenStore = FakeTokenStore()
        val authApi = mockk<AuthApi>()
        coEvery { authApi.refresh(RefreshRequest("old_refresh")) } throws createHttpException(401)

        val authenticator = TokenAuthenticator(tokenStore, authApi)
        val result = authenticator.authenticate(null, create401Response())

        assertNull(result)
        assertTrue(tokenStore.clearCalled)
        assertTrue(tokenStore.notifySessionExpiredCalled)
        assertNull(tokenStore.cachedAccessToken)
        assertNull(tokenStore.cachedRefreshToken)
    }

    @Test
    fun `400 from refresh clears tokens and notifies session expired`() {
        val tokenStore = FakeTokenStore()
        val authApi = mockk<AuthApi>()
        coEvery { authApi.refresh(RefreshRequest("old_refresh")) } throws createHttpException(400)

        val authenticator = TokenAuthenticator(tokenStore, authApi)
        val result = authenticator.authenticate(null, create401Response())

        assertNull(result)
        assertTrue(tokenStore.clearCalled)
        assertTrue(tokenStore.notifySessionExpiredCalled)
        assertNull(tokenStore.cachedAccessToken)
        assertNull(tokenStore.cachedRefreshToken)
    }

    @Test
    fun `IOException does not clear tokens`() {
        val tokenStore = FakeTokenStore()
        val authApi = mockk<AuthApi>()
        coEvery { authApi.refresh(RefreshRequest("old_refresh")) } throws IOException("Connection timed out")

        val authenticator = TokenAuthenticator(tokenStore, authApi)
        val result = authenticator.authenticate(null, create401Response())

        assertNull(result)
        assertFalse(tokenStore.clearCalled)
        assertFalse(tokenStore.notifySessionExpiredCalled)
        assertEquals("old_access", tokenStore.cachedAccessToken)
        assertEquals("old_refresh", tokenStore.cachedRefreshToken)
    }

    @Test
    fun `5xx server error does not clear tokens`() {
        val tokenStore = FakeTokenStore()
        val authApi = mockk<AuthApi>()
        coEvery { authApi.refresh(RefreshRequest("old_refresh")) } throws createHttpException(500)

        val authenticator = TokenAuthenticator(tokenStore, authApi)
        val result = authenticator.authenticate(null, create401Response())

        assertNull(result)
        assertFalse(tokenStore.clearCalled)
        assertFalse(tokenStore.notifySessionExpiredCalled)
        assertEquals("old_access", tokenStore.cachedAccessToken)
        assertEquals("old_refresh", tokenStore.cachedRefreshToken)
    }

    @Test
    fun `success saves new tokens and returns retried request with new Authorization header`() {
        val tokenStore = FakeTokenStore()
        val authApi = mockk<AuthApi>()
        coEvery { authApi.refresh(RefreshRequest("old_refresh")) } returns TokenResponse(
            accessToken = "new_access_token",
            refreshToken = "new_refresh_token",
            tokenType = "Bearer",
            expiresIn = 3600
        )

        val authenticator = TokenAuthenticator(tokenStore, authApi)
        val result = authenticator.authenticate(null, create401Response())

        assertNotNull(result)
        assertEquals("Bearer new_access_token", result?.header("Authorization"))
        assertEquals("new_access_token", tokenStore.savedAccess)
        assertEquals("new_refresh_token", tokenStore.savedRefresh)
        assertEquals("new_access_token", tokenStore.cachedAccessToken)
        assertEquals("new_refresh_token", tokenStore.cachedRefreshToken)
        assertFalse(tokenStore.clearCalled)
        assertFalse(tokenStore.notifySessionExpiredCalled)
    }
}
