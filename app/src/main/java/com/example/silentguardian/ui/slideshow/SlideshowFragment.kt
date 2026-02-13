package com.example.silentguardian.ui.slideshow

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.silentguardian.databinding.FragmentSlideshowBinding
import org.json.JSONArray

class SlideshowFragment : Fragment() {

    private var _binding: FragmentSlideshowBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Enable scrolling for the log text
        binding.textSlideshow.movementMethod = ScrollingMovementMethod()

        displayLogs()

        binding.btnClearLogs.setOnClickListener {
            wipeLogs()
        }
    }

    private fun displayLogs() {
        val sharedPref = requireActivity().getSharedPreferences("SilentGuardianVault", Context.MODE_PRIVATE)
        val logsJson = sharedPref.getString("security_logs", "[]")
        val logsArray = JSONArray(logsJson)

        val logBuilder = StringBuilder()
        logBuilder.append("--- SYSTEM ACCESS LOGS ---\n\n")

        if (logsArray.length() == 0) {
            logBuilder.append("NO LOG DATA DETECTED.\nDATABASE IS CLEAN.")
        } else {
            // Newest logs at the top
            for (i in logsArray.length() - 1 downTo 0) {
                logBuilder.append("${logsArray.getString(i)}\n\n")
            }
        }

        binding.textSlideshow.apply {
            text = logBuilder.toString()
            typeface = Typeface.MONOSPACE
        }
    }

    private fun wipeLogs() {
        val sharedPref = requireActivity().getSharedPreferences("SilentGuardianVault", Context.MODE_PRIVATE)

        // Remove only the logs, keep the vault data (passwords/images)
        sharedPref.edit().remove("security_logs").apply()

        Toast.makeText(context, "Audit Trail Wiped", Toast.LENGTH_SHORT).show()
        displayLogs() // Refresh UI
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}