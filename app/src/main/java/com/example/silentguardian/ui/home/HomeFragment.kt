package com.example.silentguardian.ui.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.silentguardian.BiometricHelper
import com.example.silentguardian.ContextManager
import com.example.silentguardian.databinding.FragmentHomeBinding
import java.util.Calendar

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

        // Initial Stealth State: Hide the secure card and keep text neutral
        binding.secureCard.visibility = View.GONE
        binding.secureCard.alpha = 0f

        // 1. Context-Aware Feature: Check Network Environment
        if (contextManager.isTrustedEnvironment()) {
            // SUCCESS: Trusted Wi-Fi detected
            showSecureContent("Trusted Network")
            Toast.makeText(requireContext(), "Safe Zone: Access Granted", Toast.LENGTH_SHORT).show()
        } else {
            // CHALLENGE: Unsecured environment (Mobile Data or Unknown Wi-Fi)
            binding.contextStatus.text = "Environment Unsecured: Scan Required"

            // 2. Mobile Security Feature: Biometric Authentication
            biometricHelper.setupBiometric(
                onSuccess = {
                    showSecureContent("Biometric Verification")
                }
            )
            biometricHelper.authenticate()
        }
    }

    private fun showSecureContent(authType: String) {
        // 3. Creative Context: Dynamic Greeting based on Time of Day
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 0..11 -> "Good Morning, Agent"
            in 12..17 -> "Good Afternoon, Agent"
            else -> "Good Evening, Agent"
        }

        // Update UI Text
        binding.textHome.text = greeting
        binding.contextStatus.text = "Identity Secured via $authType"
        binding.contextStatus.setTextColor(Color.parseColor("#03DAC5")) // Stealth Green

        // 4. Creative Execution: Animation Flow
        binding.secureCard.visibility = View.VISIBLE
        binding.secureCard.animate()
            .alpha(1f)
            .setDuration(1000)
            .start()

        // Update Icon to "Unlocked" with a scale animation
        binding.lockIcon.setImageResource(android.R.drawable.ic_partial_secure)
        binding.lockIcon.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(500)
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}