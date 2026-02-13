package com.example.silentguardian.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.silentguardian.BiometricHelper
import com.example.silentguardian.ContextManager
import com.example.silentguardian.MainActivity
import com.example.silentguardian.databinding.FragmentHomeBinding
import androidx.lifecycle.lifecycleScope
import com.example.silentguardian.R
import com.example.silentguardian.data.AppDatabase
import com.example.silentguardian.data.VaultEntry
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val contextManager = ContextManager(requireContext())
        val biometricHelper = BiometricHelper(requireActivity())
        val sharedPref = requireContext().getSharedPreferences("SilentGuardianVault", Context.MODE_PRIVATE)

        // 1. Initial Load (Using the HTML parser function)
        updateUIBasedOnTrust(contextManager)
        refreshVaultDisplay()

        // 2. Unlock Logic
        binding.btnUnlock.setOnClickListener {
            if (contextManager.isTrustedEnvironment()) {
                showSecureContent("SAFE_ZONE_BYPASS")
            } else {
                biometricHelper.setupBiometric(
                    onSuccess = { showSecureContent("BIOMETRIC_AUTH") },
                    onFail = { Toast.makeText(requireContext(), "ACCESS DENIED", Toast.LENGTH_SHORT).show() }
                )
                biometricHelper.authenticate()
            }
        }

        binding.btnSaveContent.setOnClickListener {
            val newData = binding.etSecretContent.text.toString().trim()
            if (newData.isNotEmpty()) {
                // lifecycleScope allows us to run database tasks without freezing the screen
                viewLifecycleOwner.lifecycleScope.launch {
                    val db = AppDatabase.getDatabase(requireContext())
                    db.vaultDao().insertEntry(VaultEntry(content = newData))

                    binding.etSecretContent.text.clear()
                    refreshVaultDisplay() // This now pulls from the database
                    Toast.makeText(requireContext(), "ENTRY_ADDED_OFFLINE", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 4. Long Press to Copy
        binding.tvStoredDisplay.setOnLongClickListener {
            val textToCopy = binding.tvStoredDisplay.text.toString()
            if (textToCopy.isNotEmpty() && textToCopy != "[ EMPTY_RECORD ]") {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Vault Entry", textToCopy)
                clipboard.setPrimaryClip(clip)

                Toast.makeText(requireContext(), "COPIED", Toast.LENGTH_SHORT).show()
                it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            }
            true
        }

        typeWriterEffect(binding.tvVaultHeader, "[ SYSTEM_LOCKED ]", 80)
    }

    // Ensure display refreshes if you come back from another fragment after a wipe
    override fun onResume() {
        super.onResume()
        // Only refresh if the view is actually created and binding is valid
        if (_binding != null) {
            val sharedPref = requireContext().getSharedPreferences("SilentGuardianVault", Context.MODE_PRIVATE)
            refreshVaultDisplay()
        }
    }

    private fun updateUIBasedOnTrust(manager: ContextManager) {
        val ssid = manager.getCurrentWifiSSID().replace("\"", "").uppercase()
        binding.tvDebugNet.text = "NODE_ID: [$ssid]"

        if (manager.isTrustedEnvironment()) {
            binding.contextStatus.text = "SAFE_ZONE: READY"
            binding.btnUnlock.text = "OPEN_VAULT"
        } else {
            binding.contextStatus.text = "UNTRUSTED: AUTH_REQUIRED"
            binding.btnUnlock.text = "VERIFY_IDENTITY"
        }
    }

    private fun showSecureContent(method: String) {
        (activity as? MainActivity)?.addSecurityLog("ACCESS: $method")

        // 1. Switch to Vault Theme
        binding.root.setBackgroundColor(android.graphics.Color.parseColor("#0D0D0D")) // Black
        binding.tvVaultHeader.setTextColor(android.graphics.Color.parseColor("#00FF41")) // Green

        val fade = AlphaAnimation(1.0f, 0.0f).apply {
            duration = 500
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationEnd(p0: Animation?) {
                    binding.btnUnlock.visibility = View.GONE
                    binding.tvVaultHeader.text = "[ VAULT_DECRYPTED ]"
                    binding.matrixBg.alpha = 0.2f
                    binding.vaultContentLayout.visibility = View.VISIBLE

                    (activity as? MainActivity)?.resetSecurityStatus()
                }
                override fun onAnimationStart(p0: Animation?) {}
                override fun onAnimationRepeat(p0: Animation?) {}
            })
        }
        binding.btnUnlock.startAnimation(fade)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Change this: private fun refreshVaultDisplay(prefs: SharedPreferences)
    // To this:
    private fun refreshVaultDisplay() {
        if (_binding == null) return

        viewLifecycleOwner.lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val entries = db.vaultDao().getAllEntries()

            val displayBuilder = StringBuilder()
            for (entry in entries) {
                val date = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(entry.timestamp))

                displayBuilder.append("<b><font color='#888888'>[$date]</font></b><br/>")
                displayBuilder.append("> ${entry.content}<br/>---<br/>")
            }

            val finalHtml = if (displayBuilder.isEmpty()) "[ EMPTY_RECORD ]" else displayBuilder.toString()
            binding.tvStoredDisplay.text = android.text.Html.fromHtml(finalHtml, android.text.Html.FROM_HTML_MODE_LEGACY)
        }
    }

    // Add this function inside your HomeFragment class
    private fun typeWriterEffect(view: android.widget.TextView, text: String, delay: Long = 100) {
        view.text = "" // Clear existing text
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var index = 0

        val runnable = object : Runnable {
            override fun run() {
                if (index <= text.length) {
                    view.text = text.substring(0, index++)
                    handler.postDelayed(this, delay)
                }
            }
        }
        handler.post(runnable)
    }

    // In HomeFragment.kt
    fun lockVaultUI() {
        if (_binding == null) return

        activity?.runOnUiThread {
            // Use 'vaultContentLayout' and 'btnUnlock' from your code
            binding.vaultContentLayout.visibility = android.view.View.GONE
            binding.btnUnlock.visibility = android.view.View.VISIBLE

            binding.root.setBackgroundColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.black))
            binding.tvVaultHeader.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.teal_200))
            binding.tvVaultHeader.text = "SILENT GUARDIAN"

            binding.matrixBg.alpha = 0.0f
        }
    }

    // In HomeFragment.kt
    fun clearDisplayImmediately() {
        if (_binding != null) {
            // This clears the cached text on the screen instantly
            binding.tvStoredDisplay.text = "[ EMPTY_RECORD ]"
        }
    }
}