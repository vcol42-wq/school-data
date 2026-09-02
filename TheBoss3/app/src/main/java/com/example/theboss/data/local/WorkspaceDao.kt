package com.example.theboss.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {

    // ==================== عمليات الملاحظات ====================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity): Int

    @Delete
    suspend fun deleteNote(note: NoteEntity): Int

    @Query("SELECT * FROM notes ORDER BY created_at DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    suspend fun getNoteById(noteId: Long): NoteEntity?


    // ==================== عمليات المهام ====================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity): Int

    @Delete
    suspend fun deleteTask(task: TaskEntity): Int

    @Query("SELECT * FROM tasks ORDER BY is_completed ASC, due_date ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET is_completed = :completed WHERE id = :taskId")
    suspend fun setTaskCompletion(taskId: Long, completed: Boolean): Int


    // ==================== عمليات الامتحانات والعداد التنازلي ====================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity): Long

    @Update
    suspend fun updateExam(exam: ExamEntity): Int

    @Delete
    suspend fun deleteExam(exam: ExamEntity): Int

    @Query("SELECT * FROM exams WHERE exam_timestamp >= :currentTime ORDER BY exam_timestamp ASC")
    fun getUpcomingExams(currentTime: Long): Flow<List<ExamEntity>>


    // ==================== عمليات المنبهات والمؤقتات ====================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    @Update
    suspend fun updateAlarm(alarm: AlarmEntity): Int

    @Delete
    suspend fun deleteAlarm(alarm: AlarmEntity): Int

    @Query("SELECT * FROM alarms ORDER BY trigger_time ASC")
    fun getAllAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE id = :alarmId LIMIT 1")
    suspend fun getAlarmById(alarmId: Int): AlarmEntity?
}

// دالة مساعدة لتشغيل الاستدعاء بدون معاملات (كـ Extension Function خارج الـ DAO Interface)
fun WorkspaceDao.getUpcomingExams(): Flow<List<ExamEntity>> = getUpcomingExams(System.currentTimeMillis())