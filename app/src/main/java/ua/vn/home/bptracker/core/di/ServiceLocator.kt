package ua.vn.home.bptracker.core.di

import android.annotation.SuppressLint
import android.content.Context
import retrofit2.create
import ua.vn.home.bptracker.core.auth.TokenStore
import ua.vn.home.bptracker.core.config.MOCK_MODE
import ua.vn.home.bptracker.core.config.SettingsStore
import ua.vn.home.bptracker.core.network.ApiClient
import ua.vn.home.bptracker.core.network.TokenAuthenticator
import ua.vn.home.bptracker.data.api.*
import ua.vn.home.bptracker.data.local.BpDatabase
import ua.vn.home.bptracker.data.repository.*
import ua.vn.home.bptracker.feature.ocr.MockOcrEngine
import ua.vn.home.bptracker.feature.ocr.OcrEngine
import ua.vn.home.bptracker.feature.ocr.OnnxOcrEngine

@SuppressLint("StaticFieldLeak")
object ServiceLocator {

    lateinit var tokenStore: TokenStore
        private set

    @SuppressLint("StaticFieldLeak")
    lateinit var settingsStore: SettingsStore
        private set

    private val plainRetrofit by lazy { ApiClient.plainRetrofit() }
    
    private val authedRetrofit by lazy {
        ApiClient.authedRetrofit(tokenStore, tokenAuthenticator)
    }

    private val tokenAuthenticator by lazy {
        TokenAuthenticator(tokenStore, authApi)
    }

    private val database by lazy { BpDatabase.build(applicationContext) }

    val authApi: AuthApi by lazy { plainRetrofit.create() }
    val sessionApi: SessionApi by lazy { authedRetrofit.create() }
    val userApi: UserApi by lazy { authedRetrofit.create() }

    private val measurementApi: MeasurementApi by lazy { authedRetrofit.create() }
    private val reminderApi: ReminderApi by lazy { authedRetrofit.create() }
    private val prescriptionApi: PrescriptionApi by lazy { authedRetrofit.create() }
    private val medicationItemApi: MedicationItemApi by lazy { authedRetrofit.create() }
    private val reminderConfigApi: ReminderConfigApi by lazy { authedRetrofit.create() }
    val ocrApi: OcrApi by lazy { authedRetrofit.create() }

    val measurementRepository: MeasurementRepository by lazy {
        if (MOCK_MODE) MockMeasurementRepository()
        else RealMeasurementRepository(measurementApi, database.measurementDao())
    }

    val reminderRepository: ReminderRepository by lazy {
        if (MOCK_MODE) MockReminderRepository()
        else RealReminderRepository(reminderApi, database.medIntakeDao())
    }

    val prescriptionRepository: PrescriptionRepository by lazy {
        if (MOCK_MODE) MockPrescriptionRepository()
        else RealPrescriptionRepository(
            database,
            prescriptionApi,
            medicationItemApi,
            database.prescriptionDao(),
            database.medicationItemDao()
        )
    }

    val reminderConfigRepository: ReminderConfigRepository by lazy {
        if (MOCK_MODE) MockReminderConfigRepository()
        else RealReminderConfigRepository(reminderConfigApi)
    }

    val ocrEngine: OcrEngine by lazy {
        if (MOCK_MODE) MockOcrEngine()
        else OnnxOcrEngine(applicationContext)
    }

    lateinit var applicationContext: Context
        private set

    fun init(context: Context) {
        applicationContext = context.applicationContext
        tokenStore = TokenStore(applicationContext)
        settingsStore = SettingsStore(applicationContext)
    }
}
