package com.school.system.data.dao

import androidx.room.*
import com.school.system.data.model.Student
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE grade = :grade AND section = :section AND subject = :subject ORDER BY fullName ASC")
    fun getStudentsForClass(grade: String, section: String, subject: String): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student)

    @Update
    suspend fun updateStudent(student: Student)

    @Delete
    suspend fun deleteStudent(student: Student)

    @Query("DELETE FROM students")
    suspend fun clearAll()

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getStudentById(id: Long): Student?

    @Query("SELECT * FROM students WHERE grade = :grade AND section = :section ORDER BY fullName ASC")
    suspend fun getStudentsForGradeAndSection(grade: String, section: String): List<Student>

    @Query("SELECT * FROM students WHERE grade = :grade AND section = :section AND recordNumber = :recordNumber AND subject = :subject LIMIT 1")
    suspend fun getStudentByDetails(grade: String, section: String, recordNumber: String, subject: String): Student?

    @Query("SELECT * FROM students WHERE grade = :grade AND section = :section AND subject = :subject ORDER BY fullName ASC")
    suspend fun getStudentsListForClass(grade: String, section: String, subject: String): List<Student>

    @Query("UPDATE students SET fullName = :newName WHERE grade = :grade AND section = :section AND recordNumber = :recordNumber")
    suspend fun updateStudentNameForAllSubjects(grade: String, section: String, recordNumber: String, newName: String)

    @Query("SELECT COUNT(DISTINCT recordNumber) FROM students")
    suspend fun getUniqueStudentsCount(): Int

    @Query("SELECT * FROM students ORDER BY fullName ASC")
    suspend fun getAllStudentsList(): List<Student>
}
