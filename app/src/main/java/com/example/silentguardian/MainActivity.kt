package com.example.silentguardian

import android.Manifest
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.silentguardian.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.silentguardian.data.AppDatabase
import com.example.silentguardian.ui.home.HomeFragment
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.edit
import android.net.wifi.WifiManager
import kotlinx.coroutines.delay
import android.content.BroadcastReceiver
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var sensorManager: SensorManager
    private var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        /*
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )
         */
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow, R.id.nav_settings),
            drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        startStatusPulse()

        // In MainActivity.kt
        binding.appBarMain.fab.setOnClickListener {
            quickLockVault()
            triggerVibration(isPermanentWipe = false) // Short buzz for lock
            Toast.makeText(this, "VAULT_LOCKED", Toast.LENGTH_SHORT).show()
        }

        binding.appBarMain.fab.setOnLongClickListener {
            lifecycleScope.launch { // lifecycleScope makes 'delay' work
                var countdown = 5
                while (countdown > 0) {
                    binding.appBarMain.toolbarStatus.text = "● SELF_DESTRUCT_IN: $countdown"
                    binding.appBarMain.toolbarStatus.setTextColor(Color.YELLOW)
                    delay(1000)
                    countdown--
                }
                executeEmergencyWipe()
            }
            true
        }

        checkAndRequestPermissions()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            lastAcceleration = currentAcceleration
            currentAcceleration = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta = currentAcceleration - lastAcceleration
            acceleration = acceleration * 0.9f + delta

            if (acceleration > 12) {
                quickLockVault()
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun addSecurityLog(event: String) {
        val sharedPref = getSharedPreferences("SilentGuardianVault", Context.MODE_PRIVATE)
        val logsJson = sharedPref.getString("security_logs", "[]") ?: "[]"
        val logsArray = JSONArray(logsJson)
        val timeStamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logsArray.put("[$timeStamp] $event")

        // Use the KTX extension (it handles apply() for you)
        sharedPref.edit {
            putString("security_logs", logsArray.toString())
        }
    }

    private fun executeEmergencyLockdown() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            db.vaultDao().deleteAll()

            launch(Dispatchers.Main) {
                // Update the Toolbar Status we fixed earlier
                binding.appBarMain.toolbarStatus.text = "● SYSTEM_WIPED"
                binding.appBarMain.toolbarStatus.setTextColor(android.graphics.Color.RED)

                // Force the fragment to refresh (it will now show [ EMPTY_RECORD ])
                quickLockVault()

                Toast.makeText(this@MainActivity, "DATA_DELETED_PERMANENTLY", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun resetSecurityStatus() {
        runOnUiThread {
            val statusText = findViewById<TextView>(R.id.toolbar_status)
            statusText?.text = "● SYSTEM ENCRYPTED"
            statusText?.setTextColor(Color.parseColor("#03DAC5"))
            invalidateOptionsMenu()
        }
    }

    private fun startStatusPulse() {
        val statusText = findViewById<TextView>(R.id.toolbar_status)
        val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse)
        statusText?.startAnimation(pulseAnim)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return when (item.itemId) {
            R.id.action_settings -> {
                navController.navigate(R.id.nav_settings)
                true
            }
            R.id.action_wipe -> {
                val sharedPref = getSharedPreferences("SilentGuardianVault", Context.MODE_PRIVATE)
                sharedPref.edit()
                    .remove("security_logs")
                    .remove("secret_note") // Wipes the vault history too
                    .apply()

                addSecurityLog("GLOBAL_WIPE_EXECUTED")
                Toast.makeText(this, "Audit Trail & Vault Cleared", Toast.LENGTH_SHORT).show()

                // Refresh the app UI to reflect the empty state
                recreate()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.USE_BIOMETRIC,
            // --- ADDED VIBRATE PERMISSION TO REQUEST ---
            Manifest.permission.VIBRATE
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 101)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun showPanicDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("ACTIVATE PANIC PROTOCOL?")
            .setMessage("This will permanently delete ALL encrypted data.")
            .setPositiveButton("WIPE EVERYTHING") { _, _ ->
                executeEmergencyLockdown() // The function we fixed earlier
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun showPanicDeleteDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("TERMINATE DATA?")
            .setMessage("This will permanently wipe all offline records.")
            .setPositiveButton("DELETE") { _, _ ->
                executeEmergencyWipe()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun executeEmergencyWipe() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            db.vaultDao().deleteAll()

            launch(Dispatchers.Main) {
                // Trigger the complex feedback you requested
                triggerVibration(isPermanentWipe = true)

                // Clean the UI instantly
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
                val currentFragment = navHostFragment?.childFragmentManager?.fragments?.get(0) as? HomeFragment

                currentFragment?.let {
                    it.clearDisplayImmediately()
                    it.lockVaultUI()
                }

                // Log the event in the Access Config history
                addSecurityLog("PANIC_WIPE_EXECUTED_BY_USER")
                binding.appBarMain.toolbarStatus.text = "● SYSTEM_PURGED"
                binding.appBarMain.toolbarStatus.setTextColor(Color.RED)
            }
        }
    }

    private fun quickLockVault() {
        // 1. Find the Navigation Host
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)

        // 2. Loop through active fragments to find HomeFragment
        val fragments = navHostFragment?.childFragmentManager?.fragments
        fragments?.forEach { fragment ->
            if (fragment is HomeFragment) {
                fragment.lockVaultUI()
            }
        }

        // 3. Force the status bar update
        runOnUiThread {
            binding.appBarMain.toolbarStatus.text = "● SYSTEM_ENCRYPTED"
            binding.appBarMain.toolbarStatus.setTextColor(android.graphics.Color.parseColor("#03DAC5"))
        }
    }
    private fun triggerVibration(isPermanentWipe: Boolean) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // Only proceed if the OS version is high enough and permission is declared
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = if (isPermanentWipe) {
                VibrationEffect.createWaveform(longArrayOf(0, 150, 50, 150, 50, 150), -1)
            } else {
                VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(if (isPermanentWipe) 500 else 50)
        }
    }

    private fun checkSafeZone() {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val isWifiEnabled = wifiManager.isWifiEnabled
        val info = wifiManager.connectionInfo
        val currentSsid = info.ssid?.replace("\"", "") ?: ""

        val sharedPref = getSharedPreferences("SilentGuardianVault", Context.MODE_PRIVATE)
        val safeSsid = sharedPref.getString("safe_wifi_name", "OFFLINE")

        if (safeSsid != "OFFLINE") {
            if (!isWifiEnabled || currentSsid.isEmpty() || currentSsid == "<unknown ssid>" || currentSsid != safeSsid) {
                quickLockVault()
                triggerVibration(false)
                addSecurityLog("SAFE_ZONE_BREACHED: VAULT_SECURED")
            }
        }
    }

    // Separate function to handle the lock and the pulse
    private fun forceLockSequence(reason: String) {
        quickLockVault() // This resets your Fragment
        triggerVibration(false)
        addSecurityLog("AUTO_LOCK: $reason")

        // Re-trigger the Pulse here to make sure it shows on the red/teal status
        startStatusPulse()
    }

    fun captureIntruderPhoto() {
        // This is a high-level ADV requirement: Mobile Vision/Recognition
        // Logic: Access front camera silently and save to private storage
        addSecurityLog("INTRUDER_CAPTURE_INITIATED")

        // In a real implementation, you would use CameraX or Camera2
        // to bind to the front camera and take a picture.
        Toast.makeText(this, "SECURE_SCAN_ACTIVE", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        // Register the shake listener
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accel?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private var inactivityJob: kotlinx.coroutines.Job? = null

    fun startInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = lifecycleScope.launch {
            delay(30000) // 30 seconds of inactivity
            quickLockVault()
            addSecurityLog("AUTO_LOCK: INACTIVITY_TIMEOUT")
        }
    }

    // You would call startInactivityTimer() in onUserInteraction()
    override fun onUserInteraction() {
        super.onUserInteraction()
        startInactivityTimer()
    }

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // This Toast helps you confirm the code is actually running
            Toast.makeText(context, "SYSTEM: NETWORK_CHANGE_DETECTED", Toast.LENGTH_SHORT).show()
            checkSafeZone()
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = android.content.IntentFilter().apply {
            addAction(android.net.wifi.WifiManager.WIFI_STATE_CHANGED_ACTION) // Hardware Toggle
            addAction(android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION) // Connection Change
        }
        registerReceiver(wifiReceiver, filter)
    }

    var isVaultLocked: Boolean = true // Track state globally in MainActivity

    fun setVaultUnlocked() {
        isVaultLocked = false
        binding.appBarMain.toolbarStatus.text = "● SYSTEM_DECRYPTED"
        binding.appBarMain.toolbarStatus.setTextColor(Color.GREEN)
    }

    override fun onPause() {
        super.onPause()
        // Unregister to prevent crashes and save battery
        sensorManager.unregisterListener(sensorListener)
    }
}