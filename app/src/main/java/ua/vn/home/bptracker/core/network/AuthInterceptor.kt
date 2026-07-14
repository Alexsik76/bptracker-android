package ua.vn.home.bptracker.core.network

import okhttp3.Interceptor
import okhttp3.Response
import ua.vn.home.bptracker.core.auth.TokenStore

class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.cachedAccessToken
        val request = if (token.isNullOrEmpty()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
