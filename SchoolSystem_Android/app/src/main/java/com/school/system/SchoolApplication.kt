package com.school.system

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SchoolApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("SchoolSystem", "SchoolApplication onCreate")
    }
}
