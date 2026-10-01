package com.example.theboss.ui.auth

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theboss.data.remote.JoinRequest
import com.example.theboss.data.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JoinRequestViewModel @Inject constructor(
    private val repository: StudentRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val _isSuccess = mutableStateOf(false)
    val isSuccess: State<Boolean> = _isSuccess

    fun pairDirectlyFromQr(
        schoolId: String,
        schoolCode: String,
        schoolName: String,
        url: String,
        apiKey: String
    ) {
        viewModelScope.launch {
            repository.pairSchoolDirectly(
                schoolId = schoolId,
                schoolCode = schoolCode,
                schoolName = schoolName,
                url = url.ifBlank { null },
                apiKey = apiKey.ifBlank { null }
            )
            if (schoolId.isNotBlank() && schoolId != "independent_student" && schoolId != "school_01") {
                com.school.system.utils.RoleManager.lockRole(context, com.school.system.utils.AppRole.STUDENT)
            }
        }
    }

    @SuppressLint("HardwareIds")
    fun sendRequest(
        name: String,
        email: String,
        schoolCode: String,
        stage: String,
        grade: String,
        section: String
    ) {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN"
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            // 1. التحقق من كود المدرسة أو الاعتماد على الجلسة المقترنة بالباركود
            var schoolId = repository.getSchoolId()
            if (schoolId.isNullOrBlank() || schoolId == "school_01" || schoolCode.isNotBlank()) {
                val verifyResult = repository.verifySchoolCode(schoolCode)
                if (verifyResult.isFailure && (schoolId.isNullOrBlank() || schoolId == "school_01")) {
                    _error.value = verifyResult.exceptionOrNull()?.message ?: "كود المدرسة غير صحيح"
                    _isLoading.value = false
                    return@launch
                }
                schoolId = repository.getSchoolId() ?: ""
            }

            if (schoolId.isBlank() || schoolId == "school_01") {
                _error.value = "يرجى مسح باركود المدرسة أو إدخال كود مدرسة صالح"
                _isLoading.value = false
                return@launch
            }

            // 2. Submit Join Request
            val request = JoinRequest(
                schoolId = schoolId,
                fullName = name,
                deviceId = deviceId,
                className = grade,
                sectionName = section
            )
            
            try {
                repository.submitJoinRequest(request)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)
            val targetGrade = grade.trim().ifEmpty { stage.trim() }.ifEmpty { "الأول المتوسط" }
            val targetSec = section.trim().ifEmpty { "أ" }
            prefs.edit()
                .putString("student_name", name.trim())
                .putString("student_email", email.trim())
                .putString("student_grade", targetGrade)
                .putString("student_section", targetSec)
                .putBoolean("independent_mode", false)
                .apply()

            try {
                // مطابقة ذكية بالاسم الثلاثي وربط القيد والدرجات والغياب فورياً
                repository.matchAndBindStudentByName(
                    schoolId = schoolId,
                    enteredName = name.trim(),
                    grade = targetGrade,
                    section = targetSec
                )
                val boundRecord = repository.getStudentRecordNumber()
                repository.syncStudentGrades(schoolId, boundRecord)
                repository.syncAttendance(schoolId, boundRecord)
                repository.syncTimetableAndInstructions(schoolId)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (schoolId.isNotBlank() && schoolId != "independent_student" && schoolId != "school_01") {
                com.school.system.utils.RoleManager.lockRole(context, com.school.system.utils.AppRole.STUDENT)
            }

            _isSuccess.value = true
            _isLoading.value = false
        }
    }

    fun continueAsIndependentStudent(
        name: String = "",
        grade: String = "",
        section: String = ""
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.verifySchoolCode("independent")
            val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)
            val finalName = name.trim().ifEmpty { "طالب مستقل" }
            val targetGrade = grade.trim().ifEmpty { "الأول المتوسط" }
            val targetSec = section.trim().ifEmpty { "أ" }
            prefs.edit()
                .putString("student_name", finalName)
                .putString("student_grade", targetGrade)
                .putString("student_section", targetSec)
                .putBoolean("independent_mode", true)
                .apply()
            _isSuccess.value = true
            _isLoading.value = false
        }
    }
}
