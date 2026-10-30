package ua.vn.home.bptracker.core.network

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import ua.vn.home.bptracker.BuildConfig
import ua.vn.home.bptracker.core.auth.TokenStore

@OptIn(ExperimentalSerializationApi::class)
object ApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

    private fun createLoggingInterceptor(): okhttp3.Interceptor {
        if (!BuildConfig.DEBUG) {
            return HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.NONE }
        }
        val bodyLogger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
            redactHeader("Authorization")
            redactHeader("Cookie")
        }
        val headersLogger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
            redactHeader("Authorization")
            redactHeader("Cookie")
        }
        return okhttp3.Interceptor { chain ->
            val path = chain.request().url.encodedPath
            if (path.contains("/auth/") || path.endsWith("/auth")) {
                headersLogger.intercept(chain)
            } else {
                bodyLogger.intercept(chain)
            }
        }
    }

    fun plainRetrofit(): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor(createLoggingInterceptor())
            .build()
        return createRetrofit(client)
    }

    fun authedRetrofit(tokenStore: TokenStore, authenticator: Authenticator): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .authenticator(authenticator)
            .addInterceptor(createLoggingInterceptor())
            .build()
        return createRetrofit(client)
    }

    private fun createRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
