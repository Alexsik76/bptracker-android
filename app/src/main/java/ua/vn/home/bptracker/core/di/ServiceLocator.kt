package ua.vn.home.bptracker.core.di

import android.content.Context
import retrofit2.create
import ua.vn.home.bptracker.core.auth.TokenStore
import ua.vn.home.bptracker.core.config.MOCK_MODE
import ua.vn.home.bptracker.core.network.ApiClient
import ua.vn.home.bptracker.data.api.AuthApi
import ua.vn.home.bptracker.data.api.MeasurementApi
import ua.vn.home.bptracker.data.api.ReminderApi
import ua.vn.home.bptracker.data.local.BpDatabase
import ua.vn.home.bptracker.data.repository.*
import ua.vn.home.bptracker.feature.ocr.MockOcrEngine
import ua.vn.home.bptracker.feature.ocr.OcrEngine
import ua.vn.home.bptracker.feature.ocr.OnnxOcrEngine

object ServiceLocator {

    lateinit var tokenStore: TokenStore
        private set

    private val retrofit by lazy { ApiClient.retrofit(tokenStore) }

    private val database by lazy { BpDatabase.build(applicationContext) }

    val authApi: AuthApi by lazy { retrofit.create() }
    private val measurementApi: MeasurementApi by lazy { retrofit.create() }
    private val reminderApi: ReminderApi by lazy { retrofit.create() }

    val measurementRepository: MeasurementRepository by lazy {
        if (MOCK_MODE) MockMeasurementRepository()
        else RealMeasurementRepository(measurementApi, database.measurementDao())
    }

    val reminderRepository: ReminderRepository by lazy {
        if (MOCK_MODE) MockReminderRepository()
        else RealReminderRepository(reminderApi, database.medIntakeDao())
    }

    val ocrEngine: OcrEngine by lazy {
        if (MOCK_MODE) MockOcrEngine()
        else OnnxOcrEngine(applicationContext)
    }

    private lateinit var applicationContext: Context

    fun init(context: Context) {
        applicationContext = context.applicationContext
        tokenStore = TokenStore(applicationContext)
    }
}
