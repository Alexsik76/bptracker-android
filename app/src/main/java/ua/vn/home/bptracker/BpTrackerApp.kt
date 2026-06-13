package ua.vn.home.bptracker

import android.app.Application
import ua.vn.home.bptracker.core.di.ServiceLocator

class BpTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
