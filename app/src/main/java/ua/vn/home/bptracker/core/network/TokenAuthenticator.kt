package ua.vn.home.bptracker.core.network

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import ua.vn.home.bptracker.core.auth.TokenStore
import ua.vn.home.bptracker.data.api.AuthApi
import ua.vn.home.bptracker.data.dto.RefreshRequest

class TokenAuthenticator(
    private val tokenStore: TokenStore,
    private val authApi: AuthApi,   // from the PLAIN retrofit instance
) : Authenticator {

    // Application-wide: serializes all refresh attempts.
    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        val staleHeader = response.request.header("Authorization")

        return runBlocking {
            mutex.withLock {
                // Someone else refreshed while we waited on the mutex.
                // Retry with the fresh token instead of refreshing again.
                val current = tokenStore.cachedAccessToken
                if (current != null && "Bearer $current" != staleHeader) {
                    return@withLock response.request.withToken(current)
                }

                val refresh = tokenStore.cachedRefreshToken ?: return@withLock null

                try {
                    val pair = authApi.refresh(RefreshRequest(refresh))
                    tokenStore.save(pair.accessToken, pair.refreshToken)
                    response.request.withToken(pair.accessToken)
                } catch (e: Exception) {
                    tokenStore.clear()   // refresh rejected: the session is dead
                    null                 // OkHttp surfaces the original 401
                }
            }
        }
    }
}

private fun Request.withToken(token: String): Request =
    newBuilder()
        .removeHeader("Authorization")
        .addHeader("Authorization", "Bearer $token")
        .build()
