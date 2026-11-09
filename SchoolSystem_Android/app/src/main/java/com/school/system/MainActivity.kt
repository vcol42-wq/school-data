package com.school.system

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.school.system.data.SyncService
import com.school.system.ui.components.AppLockDialog
import com.school.system.ui.security.AppLockAuthenticationActivity
import com.school.system.ui.theme.SchoolSystemTheme
import com.school.system.utils.AppLockManager
import com.school.system.widget.WidgetScheduleHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private var isAppLocked by mutableStateOf(false)
    private var isAppLockEnabled by mutableStateOf(false)
    private var appLockError by mutableStateOf<String?>(null)
    private var isAppLockAuthenticationInFlight = false
    private val appLockHandler = Handler(Looper.getMainLooper())
    private val lockAfterBackgroundDelayMs = 5_000L
    private val lockAfterBackground = Runnable {
        if (isAppLockEnabled && !isAppLockAuthenticationInFlight && !isFinishing) {
            AppLockManager.setLocked(this, true)
            isAppLocked = true
            applyAppLockWindowSecurity()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    private val appLockLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isAppLockAuthenticationInFlight = false
        if (result.resultCode == RESULT_OK) {
            AppLockManager.setLocked(this, false)
            isAppLocked = false
            applyAppLockWindowSecurity()
            appLockError = null
        } else {
            appLockError = "لم يتم التحقق. أعد المحاولة بالبصمة أو رمز الهاتف."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        isAppLockEnabled = AppLockManager.isEnabled(this)
        isAppLocked = isAppLockEnabled || AppLockManager.isLocked(this)
        applyAppLockWindowSecurity()
        Log.d("SchoolSystem", "MainActivity onCreate")
        
        // Request Notifications permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.POST_NOTIFICATIONS
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

        // Initialize and schedule exact lesson widgets updates
        try {
            WidgetScheduleHelper.updateAllWidgets(this)
            WidgetScheduleHelper.scheduleNextLessonWidgetUpdate(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            SchoolSystemTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        SchoolSystemApp(
                            isAppLockEnabled = isAppLockEnabled,
                            onAppLockEnabledChange = ::updateAppLockEnabled
                        )
                        if (isAppLocked) {
                            AppLockDialog(
                                errorMessage = appLockError,
                                onUnlock = ::requestAppUnlock
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appLockHandler.removeCallbacks(lockAfterBackground)
        if (isAppLocked && !isAppLockAuthenticationInFlight) {
            requestAppUnlock()
        }
    }

    override fun onStop() {
        if (isAppLockEnabled && !isAppLockAuthenticationInFlight && !isChangingConfigurations) {
            appLockHandler.postDelayed(lockAfterBackground, lockAfterBackgroundDelayMs)
        }
        super.onStop()
    }

    private fun updateAppLockEnabled(enabled: Boolean) {
        if (enabled && !AppLockAuthenticationActivity.isAvailable(this)) {
            Toast.makeText(
                this,
                "فعّل بصمة أو رمز/نمط قفل الشاشة في الهاتف أولاً",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        AppLockManager.setEnabled(this, enabled)
        isAppLockEnabled = enabled
        AppLockManager.setLocked(this, false)
        isAppLocked = false
        appLockError = null
        appLockHandler.removeCallbacks(lockAfterBackground)
        applyAppLockWindowSecurity()
    }

    override fun onDestroy() {
        appLockHandler.removeCallbacks(lockAfterBackground)
        super.onDestroy()
    }

    private fun requestAppUnlock() {
        if (!isAppLocked || isAppLockAuthenticationInFlight) return
        isAppLockAuthenticationInFlight = true
        appLockLauncher.launch(AppLockAuthenticationActivity.createIntent(this))
    }

    private fun applyAppLockWindowSecurity() {
        if (isAppLocked) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
