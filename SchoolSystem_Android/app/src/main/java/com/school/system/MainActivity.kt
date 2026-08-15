package com.school.system

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.school.system.data.SyncService
import com.school.system.ui.theme.SchoolSystemTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SchoolSystem", "MainActivity onCreate")
        
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
