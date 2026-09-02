package com.principal.system.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TeacherOverviewEntity::class,
        AttendanceSummaryEntity::class,
        CriticalStudentAbsenceEntity::class,
        BroadcastEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PrincipalDatabase : RoomDatabase() {
    abstract fun dao(): PrincipalDao
}
