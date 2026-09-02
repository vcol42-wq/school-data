package com.school.system

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SchoolApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.school.system.data.worker.TeacherSyncWorker.schedule(this)
    }
}
