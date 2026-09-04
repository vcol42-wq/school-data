package com.school.system.utils

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * مشغل المصادقة البيومترية القوية (Hardware-Backed Biometric Auth Bridge)
 * يضمن عدم رفع أي درجات إلا بعد اجتياز فحص البصمة الرسمي (BiometricPrompt)
 * مع دعم بديل لكلمة مرور/نمط قفل الجهاز عند عدم توفر مستشعر بيومتري مفعل.
 */
object BiometricHelper {

    private const val TAG = "BiometricHelper"

    sealed class BiometricStatus {
        object Available : BiometricStatus()
        data class Unavailable(val reason: String) : BiometricStatus()
    }

    /**
     * فحص إمكانية التوثيق البيومتري في الجهاز.
     */
    fun checkBiometricAvailability(context: Context): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        } else {
            BIOMETRIC_STRONG
        }

        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Available
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.Unavailable("الجهاز لا يحتوي على مستشعر بصمة.")
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.Unavailable("مستشعر البصمة غير متاح حالياً.")
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.Unavailable("لم يتم تسجيل أي بصمة في إعدادات الهاتف. يرجى تفعيل قفل الشاشة أو البصمة.")
            else -> BiometricStatus.Unavailable("المصادقة الحيوية غير متوفرة في هذا الجهاز.")
        }
    }

    /**
     * إظهار نافذة مصادقة البصمة الرسمية قبل رفع الدرجات.
     *
     * @param activity النشاط الحالي (يجب أن يكون FragmentActivity)
     * @param title عنوان النافذة
     * @param subtitle العنوان الفرعي
     * @param onSuccess تُستدعى عند نجاح البصمة
     * @param onError تُستدعى عند الفشل أو الإلغاء
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "تأكيد رفع الدرجات - المصادقة الحيوية",
        subtitle: String = "يرجى المصادقة ببصمة الإصبع لاعتماد درجات الشعبة ورفعها للسحابة",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.d(TAG, "Biometric authentication succeeded.")
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.w(TAG, "Biometric authentication error ($errorCode): $errString")
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onError("تم إلغاء عملية المصادقة بواسطة المستخدم.")
                } else {
                    onError("خطأ في المصادقة: $errString")
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.w(TAG, "Biometric authentication failed (fingerprint not recognized).")
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        } else {
            promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG)
            promptInfoBuilder.setNegativeButtonText("إلغاء")
        }

        try {
            biometricPrompt.authenticate(promptInfoBuilder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch BiometricPrompt: ${e.message}", e)
            onError("تعذر إطلاق المصادقة البيومترية: ${e.message}")
        }
    }
}
