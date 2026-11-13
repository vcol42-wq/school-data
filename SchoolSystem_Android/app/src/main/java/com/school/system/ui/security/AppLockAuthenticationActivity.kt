package com.school.system.ui.security

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.school.system.utils.AppLockManager

class AppLockAuthenticationActivity : FragmentActivity() {
    private val deviceCredentialLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        finishAuthentication(result.resultCode == RESULT_OK)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            startAuthentication()
        }
    }

    private fun startAuthentication() {
        val biometricManager = BiometricManager.from(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) ==
                BiometricManager.BIOMETRIC_SUCCESS
            ) {
                showBiometricPrompt(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            } else {
                finishAuthentication(false)
            }
            return
        }

        if (biometricManager.canAuthenticate(BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
        ) {
            showBiometricPrompt(BIOMETRIC_STRONG)
        } else {
            showDeviceCredential()
        }
    }

    private fun showBiometricPrompt(authenticators: Int) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                finishAuthentication(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    showDeviceCredential()
                } else if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_CANCELED
                ) {
                    val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && keyguardManager.isDeviceSecure) {
                        showDeviceCredential()
                    } else {
                        finishAuthentication(false)
                    }
                } else {
                    finishAuthentication(false)
                }
            }
        }
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), callback)
        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("فتح التطبيق")
            .setSubtitle("استخدم بصمة الإصبع أو رمز قفل الهاتف")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptInfoBuilder.setAllowedAuthenticators(authenticators)
        } else {
            promptInfoBuilder
                .setAllowedAuthenticators(authenticators)
                .setNegativeButtonText("استخدام رمز الهاتف")
        }

        prompt.authenticate(promptInfoBuilder.build())
    }

    private fun showDeviceCredential() {
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (!keyguardManager.isDeviceSecure) {
            finishAuthentication(false)
            return
        }

        val credentialIntent = keyguardManager.createConfirmDeviceCredentialIntent(
            "فتح التطبيق",
            "أدخل رمز أو نمط قفل الهاتف للمتابعة"
        )
        if (credentialIntent == null) {
            finishAuthentication(false)
        } else {
            deviceCredentialLauncher.launch(credentialIntent)
        }
    }

    private fun finishAuthentication(authenticated: Boolean) {
        setResult(if (authenticated) RESULT_OK else RESULT_CANCELED)
        finish()
    }

    companion object {
        fun createIntent(context: android.content.Context): Intent =
            Intent(context, AppLockAuthenticationActivity::class.java)

        fun isAvailable(context: android.content.Context): Boolean =
            AppLockManager.canAuthenticate(context)
    }
}
