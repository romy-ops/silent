package com.example.silentguardian

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

class BiometricHelper(private val activity: FragmentActivity) {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    fun setupBiometric(onSuccess: () -> Unit, onFail: () -> Unit) {
        executor = ContextCompat.getMainExecutor(activity)

        biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // This triggers when the user cancels or the sensor fails
                    onFail()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // This triggers when a fingerprint is recognized but not authorized
                    onFail()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Silent Guardian Security")
            .setSubtitle("Biometric Authentication Required")
            .setNegativeButtonText("Cancel Access")
            .setConfirmationRequired(false)
            .build()
    }

    fun authenticate() {
        if (::biometricPrompt.isInitialized) {
            biometricPrompt.authenticate(promptInfo)
        }
    }
}