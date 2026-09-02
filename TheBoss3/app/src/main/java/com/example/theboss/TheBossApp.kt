package com.example.theboss

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TheBossApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.example.theboss.data.worker.StudentSyncWorker.schedule(this)
    }
}
