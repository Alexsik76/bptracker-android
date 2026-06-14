package ua.vn.home.bptracker.core.di

import android.content.Context
import retrofit2.create
import ua.vn.home.bptracker.core.auth.TokenStore
import ua.vn.home.bptracker.core.config.MOCK_MODE
import ua.vn.home.bptracker.core.network.ApiClient
import ua.vn.home.bptracker.data.api.AuthApi
import ua.vn.home.bptracker.data.api.MeasurementApi
import ua.vn.home.bptracker.data.api.ReminderApi
import ua.vn.home.bptracker.data.repository.*

object ServiceLocator {

    lateinit var tokenStore: TokenStore
        private set

    private val retrofit by lazy { ApiClient.retrofit(tokenStore) }

    val authApi: AuthApi by lazy { retrofit.create() }
    private val measurementApi: MeasurementApi by lazy { retrofit.create() }
    private val reminderApi: ReminderApi by lazy { retrofit.create() }

    val measurementRepository: MeasurementRepository by lazy {
        if (MOCK_MODE) MockMeasurementRepository()
        else RealMeasurementRepository(measurementApi)
    }

    val reminderRepository: ReminderRepository by lazy {
        if (MOCK_MODE) MockReminderRepository()
        else RealReminderRepository(reminderApi)
    }

    fun init(context: Context) {
        tokenStore = TokenStore(context.applicationContext)
    }
}
