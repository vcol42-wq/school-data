package com.school.system

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.school.system.data.SyncService
import com.school.system.ui.theme.SchoolSystemTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SchoolSystem", "MainActivity onCreate")
        
        // Request Notifications & Alarm permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.POST_NOTIFICATIONS,
                    android.Manifest.permission.SCHEDULE_EXACT_ALARM
                )
            )
        }
        
        // Start periodic sync background service
        try {
            val syncServiceIntent = Intent(this, SyncService::class.java)
            startService(syncServiceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            SchoolSystemTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SchoolSystemApp()
                }
            }
        }
    }
}
