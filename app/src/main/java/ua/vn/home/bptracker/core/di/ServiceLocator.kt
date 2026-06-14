package ua.vn.home.bptracker.core.di

import android.content.Context
import retrofit2.create
import ua.vn.home.bptracker.core.auth.TokenStore
import ua.vn.home.bptracker.core.network.ApiClient
import ua.vn.home.bptracker.data.api.AuthApi
import ua.vn.home.bptracker.data.api.MeasurementApi

object ServiceLocator {

    lateinit var tokenStore: TokenStore
        private set

    private val retrofit by lazy { ApiClient.retrofit(tokenStore) }

    val authApi: AuthApi by lazy { retrofit.create() }
    val measurementApi: MeasurementApi by lazy { retrofit.create() }

    fun init(context: Context) {
        tokenStore = TokenStore(context.applicationContext)
    }
}
