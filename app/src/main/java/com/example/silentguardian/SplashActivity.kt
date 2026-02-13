package com.example.silentguardian

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.silentguardian.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val terminalLines = arrayOf(
        "> LOADING KERNEL...",
        "> VERIFYING ENCRYPTION KEYS...",
        "> SCANNING ENVIRONMENT...",
        "> SAFE_ZONE DETECTED: [ONLINE]",
        "> ACCESSING SILENT GUARDIAN PROTOCOL...",
        "> DECRYPTING DATA..."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var lineIndex = 0
        val handler = Handler(Looper.getMainLooper())

        // Print each line with a delay
        val runnable = object : Runnable {
            override fun run() {
                if (lineIndex < terminalLines.size) {
                    val currentText = binding.tvTerminal.text.toString()
                    binding.tvTerminal.text = "$currentText\n${terminalLines[lineIndex]}"
                    lineIndex++

                    // Random delay to make it look "real"
                    handler.postDelayed(this, (400..800).random().toLong())
                } else {
                    // Once done, go to MainActivity
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }
            }
        }

        handler.postDelayed(runnable, 500)
    }
}