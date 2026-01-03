package com.example.silentguardian // Make sure this matches your project's package name

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

class BiometricHelper(private val activity: FragmentActivity) {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    fun setupBiometric(onSuccess: () -> Unit) {
        // 1. Initialize the Executor (runs the process on the main thread)
        executor = ContextCompat.getMainExecutor(activity)

        // 2. Define the Callback (what happens when the user scans their finger)
        biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess() // This triggers the code in your Fragment to show the data
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // You could add logic here to close the app if they fail too many times
                }
            })

        // 3. Design the Popup Box
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Silent Guardian Security")
            .setSubtitle("Authenticate to access secure notes")
            .setNegativeButtonText("Cancel")
            .build()
    }

    fun authenticate() {
        biometricPrompt.authenticate(promptInfo)
    }
}