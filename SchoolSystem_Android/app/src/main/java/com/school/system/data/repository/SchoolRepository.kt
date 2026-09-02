package com.school.system.data.repository

import com.school.system.data.local.SessionManager
import com.school.system.data.models.JoinRequest
import com.school.system.data.network.SupabaseService
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchoolRepository @Inject constructor(
    private val api: SupabaseService,
    private val sessionManager: SessionManager
) {

    // 1. الربط والتحقق من كود المدرسة
    suspend fun verifySchoolCode(enteredCode: String): Result<Boolean> {
        return try {
            val response = api.getSchoolByCode(codeFilter = "eq.$enteredCode")
            if (response.isSuccessful) {
                val schools = response.body()
                if (!schools.isNullOrEmpty()) {
                    val school = schools.first()
                    // حفظ المدرسة الجديدة في المستودع المحلي
                    sessionManager.saveSchoolSession(
                        schoolId = school.id,
                        schoolCode = school.schoolCode,
                        schoolName = school.schoolName
                    )
                    Result.success(true)
                } else {
                    Result.failure(Exception("كود المدرسة غير صحيح أو المدرسة غير مسجلة"))
                }
            } else {
                Result.failure(Exception("خطأ في الخادم: ${response.code()}"))
            }
        } catch (e: UnknownHostException) {
            Result.failure(Exception("تعذر الاتصال بالشبكة، يرجى التحقق من الإنترنت"))
        } catch (e: Exception) {
            Result.failure(Exception("حدث خطأ غير متوقع: ${e.localizedMessage}"))
        }
    }

    // 2. تقديم طلب انضمام جديد
    suspend fun submitJoinRequest(request: JoinRequest): Result<Boolean> {
        return try {
            val response = api.submitJoinRequest(request)
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("فشل إرسال الطلب، رمز الخطأ: ${response.code()}"))
            }
        } catch (e: UnknownHostException) {
            Result.failure(Exception("لا يوجد اتصال بالإنترنت"))
        } catch (e: Exception) {
            Result.failure(Exception("خطأ في الاتصال: ${e.localizedMessage}"))
        }
    }

    // 3. تحديث معرف المدرسة ديناميكياً (عند الانتقال لمدرسة أخرى)
    fun updateSchoolLocally(schoolId: String, schoolCode: String, schoolName: String) {
        sessionManager.saveSchoolSession(schoolId, schoolCode, schoolName)
    }

    fun getSchoolId(): String? = sessionManager.getSchoolId()
    fun getSchoolCode(): String? = sessionManager.getSchoolCode()
    fun getSchoolName(): String? = sessionManager.getSchoolName()

    fun clearSession() {
        sessionManager.clearSchoolSession()
    }

    // 4. جلب مفتاح Gemini من السحابة
    suspend fun fetchCloudGeminiKey(): String? {
        return try {
            val response = api.getAppConfig(keyFilter = "eq.gemini_api_key")
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                response.body()!!.first().configValue
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
