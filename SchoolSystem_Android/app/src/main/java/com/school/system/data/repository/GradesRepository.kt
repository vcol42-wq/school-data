package com.school.system.data.repository

import android.util.Log
import com.school.system.data.GradeUploadResult
import com.school.system.data.GradeRestoreResult
import com.school.system.data.SyncRepository
import com.school.system.data.dao.ConfigDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class SecureUploadResult {
    data class Success(val message: String) : SecureUploadResult()
    data class InvalidPin(val reason: String) : SecureUploadResult()
    data class ClassLocked(val reason: String) : SecureUploadResult()
    data class Failure(val errorMessage: String) : SecureUploadResult()
}

@Singleton
class GradesRepository @Inject constructor(
    private val configDao: ConfigDao,
    private val syncRepository: SyncRepository
) {
    private val tag = "GradesRepository"

    suspend fun uploadGradesSecurely(
        grade: String,
        section: String,
        subject: String,
        secretPin: String
    ): SecureUploadResult = withContext(Dispatchers.IO) {
        fun normalizeArabicDigits(input: String): String = buildString {
            input.forEach { ch ->
                append(
                    when (ch) {
                        '٠' -> '0'
                        '١' -> '1'
                        '٢' -> '2'
                        '٣' -> '3'
                        '٤' -> '4'
                        '٥' -> '5'
                        '٦' -> '6'
                        '٧' -> '7'
                        '٨' -> '8'
                        '٩' -> '9'
                        else -> ch
                    }
                )
            }
        }.trim()

        val cleanPin = normalizeArabicDigits(secretPin)
        if (grade.isBlank() || section.isBlank() || subject.isBlank() || cleanPin.isBlank()) {
            return@withContext SecureUploadResult.InvalidPin("رمز الأستاذ مطلوب.")
        }

        try {
            val currentConfig = configDao.getConfig().first()
            val schoolId = currentConfig?.schoolId
                ?.trim()
                ?.takeIf { it.isNotEmpty() && it != "school_01" }
                ?: currentConfig?.schoolId?.trim()?.takeIf(String::isNotEmpty)
                ?: "school_01"

            val resolvedTeacher = syncRepository.getResolvedTeacherName().takeIf { it.isNotBlank() }
                ?: currentConfig?.userEmail?.takeIf { it.isNotBlank() }
                ?: (if (currentConfig?.role == "supervisor") currentConfig?.managerName else null)

            when (
                val result = syncRepository.syncGradesAndAttendance(
                    schoolId = schoolId,
                    teacherId = resolvedTeacher,
                    targetGrade = grade.trim(),
                    targetSection = section.trim(),
                    targetSubject = subject.trim(),
                    secretCode = cleanPin
                )
            ) {
                GradeUploadResult.Success ->
                    SecureUploadResult.Success("تم اعتماد درجات ($subject) ورفعها للسحابة بنجاح ☁️✓")
                GradeUploadResult.InvalidPin ->
                    SecureUploadResult.InvalidPin(
                        "لم يُقبل الرمز: تأكد أنه رمز الأستاذ المكوّن من 4 أرقام لهذا الصف والشعبة والمادة، وأن الإدارة حفظت الإسناد والرمز في Supabase ولم تقفله."
                    )
                is GradeUploadResult.Failure ->
                    SecureUploadResult.Failure(result.message)
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception in uploadGradesSecurely: ${e.message}", e)
            SecureUploadResult.Failure("خطأ في الاتصال بالسحابة: ${e.localizedMessage ?: e.message}")
        }
    }

    suspend fun restoreGradesSecurely(
        grade: String,
        section: String,
        subject: String,
        secretPin: String
    ): GradeRestoreResult = withContext(Dispatchers.IO) {
        fun normalizeArabicDigits(input: String): String = buildString {
            input.forEach { ch ->
                append(
                    when (ch) {
                        '٠' -> '0'
                        '١' -> '1'
                        '٢' -> '2'
                        '٣' -> '3'
                        '٤' -> '4'
                        '٥' -> '5'
                        '٦' -> '6'
                        '٧' -> '7'
                        '٨' -> '8'
                        '٩' -> '9'
                        else -> ch
                    }
                )
            }
        }.trim()

        val cleanPin = normalizeArabicDigits(secretPin)
        if (grade.isBlank() || section.isBlank() || subject.isBlank() || cleanPin.isBlank()) {
            return@withContext GradeRestoreResult.InvalidPin("رمز الأستاذ مطلوب.")
        }

        try {
            val currentConfig = configDao.getConfig().first()
            val schoolId = currentConfig?.schoolId
                ?.trim()
                ?.takeIf { it.isNotEmpty() && it != "school_01" }
                ?: currentConfig?.schoolId?.trim()?.takeIf(String::isNotEmpty)
                ?: "school_01"

            syncRepository.restoreTeacherGradesFromCloud(
                schoolId = schoolId,
                targetGrade = grade.trim(),
                targetSection = section.trim(),
                targetSubject = subject.trim(),
                secretCode = cleanPin
            )
        } catch (e: Exception) {
            Log.e(tag, "Exception in restoreGradesSecurely: ${e.message}", e)
            GradeRestoreResult.Failure("خطأ في الاتصال بالسحابة: ${e.localizedMessage ?: e.message}")
        }
    }
}
