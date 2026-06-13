package ua.vn.home.bptracker.core.di

import android.content.Context
import retrofit2.create
import ua.vn.home.bptracker.core.auth.TokenStore
import ua.vn.home.bptracker.core.network.ApiClient
import ua.vn.home.bptracker.data.api.AuthApi

object ServiceLocator {

    lateinit var tokenStore: TokenStore
        private set

    val authApi: AuthApi by lazy { ApiClient.retrofit(tokenStore).create() }

    fun init(context: Context) {
        tokenStore = TokenStore(context.applicationContext)
    }
}
