package ua.vn.home.bptracker.core.network

import okhttp3.Interceptor
import okhttp3.Response

// Attaches "Authorization: Bearer <token>" once a device token is stored.
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // TODO: read token from TokenStore and add the header when present.
        return chain.proceed(chain.request())
    }
}
