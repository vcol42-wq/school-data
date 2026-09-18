package com.school.system

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SchoolApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            com.school.system.data.worker.TeacherSyncWorker.schedule(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            com.example.theboss.data.worker.StudentSyncWorker.schedule(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
