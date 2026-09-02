package com.example.theboss.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SubjectEntity::class,
        GradeEntity::class,
        AssignmentEntity::class,
        AttendanceEntity::class,
        DirectMessageEntity::class
    ],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
}
