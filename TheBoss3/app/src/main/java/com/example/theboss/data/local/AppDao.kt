package com.example.theboss.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM subjects")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectEntity>): List<Long>

    @Query("DELETE FROM subjects")
    suspend fun clearSubjects(): Int

    @Query("SELECT * FROM grades WHERE subjectId = :subjectId")
    fun getGradesForSubject(subjectId: String): Flow<List<GradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrades(grades: List<GradeEntity>): List<Long>

    @Query("SELECT * FROM assignments WHERE subjectId = :subjectId")
    fun getAssignmentsForSubject(subjectId: String): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments ORDER BY isCompleted ASC, isHot DESC")
    fun getAllAssignments(): Flow<List<AssignmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignments(assignments: List<AssignmentEntity>): List<Long>

    @Query("UPDATE assignments SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateAssignmentStatus(id: String, isCompleted: Boolean): Int

    @Query("DELETE FROM assignments")
    suspend fun clearAssignments(): Int

    @Query("SELECT * FROM attendance")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE dateString = :dateString")
    fun getAttendanceForDate(dateString: String): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: List<AttendanceEntity>): List<Long>

    @Query("DELETE FROM attendance")
    suspend fun clearAttendance(): Int

    @Query("SELECT * FROM direct_messages ORDER BY createdAt ASC")
    fun getAllDirectMessages(): Flow<List<DirectMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirectMessages(messages: List<DirectMessageEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirectMessage(message: DirectMessageEntity): Long
}
