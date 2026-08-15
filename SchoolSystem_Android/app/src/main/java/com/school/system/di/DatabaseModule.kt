package com.school.system.di

import android.content.Context
import androidx.room.Room
import com.school.system.data.SchoolDatabase
import com.school.system.data.dao.*
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
    fun provideDatabase(@ApplicationContext context: Context): SchoolDatabase {
        return Room.databaseBuilder(
            context,
            SchoolDatabase::class.java,
            "school_db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideStudentDao(db: SchoolDatabase): StudentDao = db.studentDao()

    @Provides
    fun provideConfigDao(db: SchoolDatabase): ConfigDao = db.configDao()

    @Provides
    fun provideColumnSettingDao(db: SchoolDatabase): ColumnSettingDao = db.columnSettingDao()

    @Provides
    fun provideDailyColumnDao(db: SchoolDatabase): DailyColumnDao = db.dailyColumnDao()

    @Provides
    fun provideClassPackageDao(db: SchoolDatabase): ClassPackageDao = db.classPackageDao()

    @Provides
    fun provideAbsenceDao(db: SchoolDatabase): AbsenceDao = db.absenceDao()
}
