package com.example.theboss.di

import android.content.Context
import com.example.theboss.data.local.AppWorkspaceDatabase
import com.example.theboss.data.local.WorkspaceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideWorkspaceDatabase(@ApplicationContext context: Context): AppWorkspaceDatabase {
        return AppWorkspaceDatabase.getDatabase(context)
    }

    @Provides
    fun provideWorkspaceDao(database: AppWorkspaceDatabase): WorkspaceDao {
        return database.workspaceDao()
    }
}
