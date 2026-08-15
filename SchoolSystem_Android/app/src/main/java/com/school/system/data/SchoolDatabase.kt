package com.school.system.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.SkipQueryVerification
import com.school.system.data.dao.*
import com.school.system.data.model.*
import com.school.system.data.utils.Converters

@Database(entities = [Student::class, SchoolConfig::class, ColumnSetting::class, DailyColumnSetting::class, ClassPackage::class, AbsenceRecord::class], version = 12, exportSchema = false)
@TypeConverters(Converters::class)
@SkipQueryVerification
abstract class SchoolDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun configDao(): ConfigDao
    abstract fun columnSettingDao(): ColumnSettingDao
    abstract fun dailyColumnDao(): DailyColumnDao
    abstract fun classPackageDao(): ClassPackageDao
    abstract fun absenceDao(): AbsenceDao
}
