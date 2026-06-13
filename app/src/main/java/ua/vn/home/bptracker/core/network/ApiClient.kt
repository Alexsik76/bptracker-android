package ua.vn.home.bptracker.core.network

import ua.vn.home.bptracker.BuildConfig

// TODO: build a Retrofit instance (OkHttp + kotlinx-serialization converter) in a later step.
object ApiClient {
    const val BASE_URL: String = BuildConfig.API_BASE_URL
}
