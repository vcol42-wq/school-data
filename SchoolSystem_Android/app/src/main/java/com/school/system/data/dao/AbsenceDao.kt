package com.school.system.data.dao

import androidx.room.*
import com.school.system.data.model.AbsenceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AbsenceDao {
    @Query("SELECT * FROM absences")
    fun getAllAbsences(): Flow<List<AbsenceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbsence(absence: AbsenceRecord)

    @Query("DELETE FROM absences WHERE studentId = :studentId AND dateString = :dateString")
    suspend fun deleteAbsence(studentId: Long, dateString: String)

    @Query("DELETE FROM absences WHERE dateString < :cutoffDate")
    suspend fun deleteAbsencesOlderThan(cutoffDate: String): Int

    @Query("SELECT studentId, COUNT(*) as count FROM absences WHERE dateString < :cutoffDate GROUP BY studentId")
    suspend fun getExpiredAbsencesCount(cutoffDate: String): List<StudentAbsenceCount>
}

data class StudentAbsenceCount(
    val studentId: Long,
    val count: Int
)
