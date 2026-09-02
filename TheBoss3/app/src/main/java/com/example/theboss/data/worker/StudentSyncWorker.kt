package com.example.theboss.data.worker

import android.content.Context
import androidx.work.*
import com.example.theboss.data.repository.StudentRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

class StudentSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface StudentSyncWorkerEntryPoint {
        fun studentRepository(): StudentRepository
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                StudentSyncWorkerEntryPoint::class.java
            )
            val repository = entryPoint.studentRepository()
            val schoolId = repository.getSchoolId()
            if (!schoolId.isNullOrBlank()) {
                repository.syncDailyAssignments(schoolId)
                repository.syncTimetableAndInstructions(schoolId)
                repository.syncDirectMessages()
                val deviceId = repository.getDeviceId()
                repository.syncAttendance(schoolId, deviceId)
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "StudentPeriodicSyncWorker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<StudentSyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
