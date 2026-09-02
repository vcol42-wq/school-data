package com.example.theboss.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        NoteEntity::class,
        TaskEntity::class,
        ExamEntity::class,
        AlarmEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppWorkspaceDatabase : RoomDatabase() {

    abstract fun workspaceDao(): WorkspaceDao

    companion object {
        @Volatile
        private var INSTANCE: AppWorkspaceDatabase? = null

        fun getDatabase(context: Context): AppWorkspaceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppWorkspaceDatabase::class.java,
                    "student_workspace.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
