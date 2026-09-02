package com.principal.system

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PrincipalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.principal.system.data.worker.PrincipalSyncWorker.schedule(this)
    }
}
