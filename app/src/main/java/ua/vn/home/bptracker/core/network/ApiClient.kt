package ua.vn.home.bptracker.core.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import ua.vn.home.bptracker.BuildConfig
import ua.vn.home.bptracker.core.auth.TokenStore

object ApiClient {

    private val json = Json {
        ignoreUnknownKeys = true   // tolerate extra fields from Fido2 / backend
        explicitNulls = false
    }

    fun retrofit(tokenStore: TokenStore): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .addInterceptor(logging)
            .build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
