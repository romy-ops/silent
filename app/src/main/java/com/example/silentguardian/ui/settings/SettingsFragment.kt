package com.example.silentguardian.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.silentguardian.MainActivity
import com.example.silentguardian.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences("SilentGuardianVault", Context.MODE_PRIVATE)

        // Load the currently saved SSID
        val savedSSID = sharedPref.getString("trusted_ssid", "Home_WiFi")
        binding.etTrustedWifi.setText(savedSSID)

        binding.btnSaveConfig.setOnClickListener {
            val newSSID = binding.etTrustedWifi.text.toString()
            if (newSSID.isNotEmpty()) {
                sharedPref.edit().putString("trusted_ssid", newSSID).apply()
                (activity as? MainActivity)?.addSecurityLog("TRUSTED_SSID_UPDATED")
                Toast.makeText(context, "Protocol Updated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}