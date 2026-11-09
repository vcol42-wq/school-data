package com.school.system.data

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.school.system.data.dao.ConfigDao
import com.school.system.data.dao.ClassPackageDao
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SyncService : Service() {

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var configDao: ConfigDao

    @Inject
    lateinit var packageDao: ClassPackageDao

    @Inject
    lateinit var syncRepository: SyncRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("SyncService", "SyncService onCreate starting periodic sync background job")
        startPeriodicSync()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (syncJob?.isActive != true) {
            startPeriodicSync()
        }
        return START_NOT_STICKY
    }

    private fun startPeriodicSync() {
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            while (isActive) {
                try {
                    val config = configDao.getConfig().first()
                    if (config != null && (config.isActivated || config.isVerified) && config.schoolId.isNotBlank() && config.schoolId != "school_01") {
                        // 1. Validate session integrity first
                        val isSessionValid = syncManager.validateSessionIntegrity()
                        if (!isSessionValid) {
                            Log.w("SyncService", "Session integrity validation failed: locked! Aborting class sync.")
                            delay(60000)
                            continue
                        }

                        // التحقق من تفعيل التحديث والمزامنة السحابية من قبل المستخدم
                        if (!config.isCloudSyncEnabled) {
                            Log.d("SyncService", "SyncService background execution paused: isCloudSyncEnabled is false")
                            delay(30000)
                            continue
                        }

                        Log.d("SyncService", "SyncService background execution - syncing all classes")
                        val packages = packageDao.getAllPackagesList()
                        for (pkg in packages) {
                            val success = syncManager.syncGrades(pkg.grade, pkg.section, pkg.subject)
                            Log.d("SyncService", "Synced class: ${pkg.grade}-${pkg.section} [${pkg.subject}] - success = $success")

                            // 2. تحديث أسماء الطلاب والقوائم آلياً من السحابة
                            try {
                                syncManager.downloadSimpleRosterForClass(pkg.grade, pkg.section, pkg.subject)
                            } catch (e: Exception) {
                                Log.e("SyncService", "Error auto-updating roster for ${pkg.grade}-${pkg.section}: ${e.message}")
                            }
                        }

                        // 3. تحديث جدول الحصص الأسبوعي والودجت آلياً من السحابة
                        try {
                            val schedOk = syncManager.downloadSchedule(applicationContext)
                            if (schedOk) {
                                com.school.system.widget.WidgetScheduleHelper.updateAllWidgets(applicationContext)
                                com.school.system.widget.WidgetScheduleHelper.scheduleNextLessonWidgetUpdate(applicationContext)
                            }
                        } catch (e: Exception) {
                            Log.e("SyncService", "Error auto-downloading schedule: ${e.message}")
                        }

                        // Check for new directives and broadcast notification
                        try {
                            syncRepository.fetchAndNotifyDirectives(config.schoolId)
                        } catch (e: Exception) {
                            Log.e("SyncService", "Directives background fetch error: ${e.message}")
                        }
                    } else {
                        Log.d("SyncService", "SyncService bypassed - school config not activated or verified")
                    }
                } catch (c: CancellationException) {
                    throw c
                } catch (e: Exception) {
                    Log.e("SyncService", "Error during periodic sync cycle: ${e.localizedMessage}")
                }
                delay(60000) // Sync periodically every 60 seconds
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SyncService", "SyncService onDestroy stopping periodic sync job")
        syncJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
