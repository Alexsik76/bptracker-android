package ua.vn.home.bptracker

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ua.vn.home.bptracker.core.di.ServiceLocator

class BpTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)

        // Preload OCR models on a background thread so the first scan is fast
        CoroutineScope(Dispatchers.Default).launch {
            ServiceLocator.ocrEngine.warmUp()
        }
    }
}
