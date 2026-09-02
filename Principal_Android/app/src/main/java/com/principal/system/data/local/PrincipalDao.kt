package com.principal.system.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PrincipalDao {

    // ==================== المعلمين والنشاط ====================
    @Query("SELECT * FROM teachers_overview ORDER BY gradeCompletionRate ASC, fullName ASC")
    fun getAllTeachersOverview(): Flow<List<TeacherOverviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeachers(teachers: List<TeacherOverviewEntity>): List<Long>

    @Query("DELETE FROM teachers_overview")
    suspend fun clearTeachers(): Int


    // ==================== ملخص الحضور اليومي ====================
    @Query("SELECT * FROM attendance_summary WHERE dateString = :dateString LIMIT 1")
    fun getAttendanceSummaryForDate(dateString: String): Flow<AttendanceSummaryEntity?>

    @Query("SELECT * FROM attendance_summary ORDER BY dateString DESC LIMIT 7")
    fun getRecentAttendanceSummaries(): Flow<List<AttendanceSummaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceSummary(summary: AttendanceSummaryEntity): Long


    // ==================== المنطقة الحمراء والغيابات الحرجة ====================
    @Query("SELECT * FROM critical_absences ORDER BY totalAbsenceDays DESC")
    fun getCriticalAbsences(): Flow<List<CriticalStudentAbsenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCriticalAbsences(absences: List<CriticalStudentAbsenceEntity>): List<Long>

    @Query("DELETE FROM critical_absences")
    suspend fun clearCriticalAbsences(): Int


    // ==================== التعميمات والإعلانات ====================
    @Query("SELECT * FROM broadcasts ORDER BY createdAt DESC")
    fun getAllBroadcasts(): Flow<List<BroadcastEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBroadcast(broadcast: BroadcastEntity): Long

    @Query("DELETE FROM broadcasts WHERE id = :id")
    suspend fun deleteBroadcast(id: String): Int
}
