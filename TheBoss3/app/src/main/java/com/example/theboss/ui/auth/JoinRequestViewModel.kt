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
            
            // 1. Verify School Code first
            val verifyResult = repository.verifySchoolCode(schoolCode)
            if (verifyResult.isFailure) {
                _error.value = verifyResult.exceptionOrNull()?.message ?: "كود المدرسة غير صحيح"
                _isLoading.value = false
                return@launch
            }

            val schoolId = repository.getSchoolId() ?: ""

            // 2. Submit Join Request
            val request = JoinRequest(
                schoolId = schoolId,
                fullName = name,
                deviceId = deviceId,
                className = grade,
                sectionName = section
            )
            
            val result = repository.submitJoinRequest(request)
            if (result.isSuccess) {
                val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)
                val targetGrade = grade.trim().ifEmpty { stage.trim() }.ifEmpty { "الأول المتوسط" }
                val targetSec = section.trim().ifEmpty { "أ" }
                prefs.edit()
                    .putString("student_name", name.trim())
                    .putString("student_email", email.trim())
                    .putString("student_grade", targetGrade)
                    .putString("student_section", targetSec)
                    .apply()

                try {
                    repository.syncTimetableAndInstructions(schoolId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                _isSuccess.value = true
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "فشل إرسال طلب الانضمام"
            }
            _isLoading.value = false
        }
    }
}
