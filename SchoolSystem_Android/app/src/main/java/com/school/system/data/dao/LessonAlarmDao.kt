package com.school.system.data.dao

import androidx.room.*
import com.school.system.data.model.LessonAlarm
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonAlarmDao {
    @Query("SELECT * FROM lesson_alarms ORDER BY dayOfWeek ASC, lessonIndex ASC")
    fun getAllAlarms(): Flow<List<LessonAlarm>>

    @Query("SELECT * FROM lesson_alarms WHERE dayOfWeek = :day ORDER BY lessonIndex ASC")
    fun getAlarmsForDay(day: Int): Flow<List<LessonAlarm>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarms(alarms: List<LessonAlarm>)

    @Update
    suspend fun updateAlarm(alarm: LessonAlarm)

    @Query("DELETE FROM lesson_alarms")
    suspend fun deleteAll()

    @Query("SELECT * FROM lesson_alarms WHERE isEnabled = 1")
    suspend fun getEnabledAlarmsList(): List<LessonAlarm>
}
