import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

class BiometricHelper(private val activity: FragmentActivity) {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    fun setupBiometric(onSuccess: () -> Unit) {
        executor = ContextCompat.getMainExecutor(activity)

        biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess() // This runs our "Unlock" logic
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Silent Guardian Security")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Cancel")
            .build()
    }

    fun authenticate() {
        biometricPrompt.authenticate(promptInfo)
    }
}