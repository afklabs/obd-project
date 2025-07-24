package com.example.androidautoobd2

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.androidautoobd2.databinding.ActivityMainBinding
import com.example.androidautoobd2.obd.OBDManager
import com.example.androidautoobd2.data.VehicleData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val obdManager = OBDManager.getInstance()
    private var dataUpdateJob: Job? = null

    private val requestBluetoothEnable = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            setupBluetooth()
        } else {
            binding.statusText.text = "Bluetooth is required for OBD connection\nUsing demo mode"
            obdManager.setDemoMode(true)
            setupDemoMode()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkPermissions()
    }

    private fun setupUI() {
        // Add demo mode button
        val demoButton = Button(this).apply {
            text = "🎮 Start Demo Mode"
            setOnClickListener {
                obdManager.setDemoMode(true)
                setupDemoMode()
                Toast.makeText(this@MainActivity, "Demo mode activated!", Toast.LENGTH_SHORT).show()
            }
        }

        // Add Android Auto instructions button
        val autoButton = Button(this).apply {
            text = "🚗 Connect to Android Auto"
            setOnClickListener {
                showAndroidAutoInstructions()
            }
        }

        // Add vehicle data display button
        val dataButton = Button(this).apply {
            text = "📊 View Live Data"
            setOnClickListener {
                if (obdManager.isConnected || obdManager.isDemoMode) {
                    startDataDisplay()
                } else {
                    Toast.makeText(this@MainActivity, "Please activate demo mode or connect to OBD first", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Add buttons to the layout dynamically
        val layout = binding.root as androidx.constraintlayout.widget.ConstraintLayout

        // Demo button constraints
        layout.addView(demoButton)
        demoButton.id = androidx.core.view.ViewCompat.generateViewId()
        val demoParams = demoButton.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        demoParams.topToBottom = binding.instructionText.id
        demoParams.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
        demoParams.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
        demoParams.topMargin = 32
        demoButton.layoutParams = demoParams

        // Auto button constraints
        layout.addView(autoButton)
        autoButton.id = androidx.core.view.ViewCompat.generateViewId()
        val autoParams = autoButton.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        autoParams.topToBottom = demoButton.id
        autoParams.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
        autoParams.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
        autoParams.topMargin = 16
        autoButton.layoutParams = autoParams

        // Data button constraints
        layout.addView(dataButton)
        dataButton.id = androidx.core.view.ViewCompat.generateViewId()
        val dataParams = dataButton.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        dataParams.topToBottom = autoButton.id
        dataParams.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
        dataParams.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
        dataParams.topMargin = 16
        dataButton.layoutParams = dataParams
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            setupBluetooth()
        }
    }

    private fun setupBluetooth() {
        if (bluetoothAdapter == null) {
            binding.statusText.text = "⚠️ Device doesn't support Bluetooth\n✅ Demo mode available"
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetoothEnable.launch(enableBtIntent)
        } else {
            binding.statusText.text = "✅ Bluetooth ready\n🎮 Demo mode available\n🚗 Android Auto ready"
        }
    }

    private fun setupDemoMode() {
        binding.statusText.text = "🎮 Demo Mode Active\n📊 Generating vehicle data...\n🚗 Ready for Android Auto"

        // Start generating demo data
        obdManager.setDemoMode(true)

        // Update UI to show demo is running
        Toast.makeText(this, "Demo mode started! Connect to Android Auto to see dashboard", Toast.LENGTH_LONG).show()
    }

    private fun startDataDisplay() {
        // Stop any existing job
        dataUpdateJob?.cancel()

        dataUpdateJob = lifecycleScope.launch {
            while (isActive) {
                try {
                    val data = obdManager.getVehicleData()
                    updateDataDisplay(data)
                    delay(1000) // Update every second
                } catch (e: Exception) {
                    binding.statusText.text = "❌ Error reading data: ${e.message}"
                    break
                }
            }
        }
    }

    private fun updateDataDisplay(data: VehicleData) {
        val statusText = """
            🎮 ${if (obdManager.isDemoMode) "DEMO MODE" else "LIVE DATA"}
            
            🏎️ Speed: ${data.speed} km/h
            ⚡ RPM: ${data.rpm}
            🌡️ Engine Temp: ${data.engineTemp}°C
            ⛽ Fuel: ${data.fuelLevel}%
            🚗 Throttle: ${data.throttlePosition}%
            🔋 Battery: ${String.format("%.1f", data.batteryVoltage)}V
            
            🚗 Connect to Android Auto for full dashboard
        """.trimIndent()

        binding.statusText.text = statusText
    }

    private fun showAndroidAutoInstructions() {
        val instructions = """
            To use with Android Auto:
            
            1. 📱 Connect your phone to your car via USB or wireless
            
            2. 🚗 Open Android Auto on your car's display
            
            3. 📋 Look for "OBD-II Monitor" in the app list
            
            4. 🎮 The app works in demo mode for testing
            
            5. 🔗 For real OBD data, connect a Bluetooth OBD-II adapter
            
            Note: This app is designed primarily for Android Auto use in vehicles.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("🚗 Android Auto Instructions")
            .setMessage(instructions)
            .setPositiveButton("Got it!") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Open Android Auto") { dialog, _ ->
                try {
                    val intent = packageManager.getLaunchIntentForPackage("com.google.android.projection.gearhead")
                    if (intent != null) {
                        startActivity(intent)
                    } else {
                        // Try to open Android Auto on Play Store
                        val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.projection.gearhead"))
                        startActivity(playStoreIntent)
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Android Auto not found", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                setupBluetooth()
            } else {
                binding.statusText.text = "⚠️ Some permissions denied\n✅ Demo mode still available\n🚗 Android Auto ready"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dataUpdateJob?.cancel()
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}