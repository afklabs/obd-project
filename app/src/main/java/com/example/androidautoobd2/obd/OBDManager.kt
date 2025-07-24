package com.example.androidautoobd2.obd

import android.content.Context
import com.example.androidautoobd2.data.VehicleData
import com.example.androidautoobd2.data.DataLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

class OBDManager private constructor() {

    private val dataLogger = DataLogger()
    private val obdCommunicator = OBDCommunicator()

    // Demo mode variables
    private var demoSpeed = 0
    private var demoRpm = 800
    private var demoDirection = 1 // 1 for acceleration, -1 for deceleration

    // DEBUG/TRACKING VARIABLES
    private var dataFetchCount = 0
    private var errorCount = 0
    private var lastFetchTime = 0L

    var isDemoMode = true
        private set

    var isConnected = false
        private set

    var isLogging = false
        private set

    fun setDemoMode(enabled: Boolean) {
        isDemoMode = enabled
        isConnected = enabled
        if (!enabled) {
            // If disabling demo mode, ensure we disconnect from real OBD
            // This will be implemented when real connection is added
        }
    }

    suspend fun getVehicleData(): VehicleData {
        return withContext(Dispatchers.IO) {
            try {
                dataFetchCount++
                lastFetchTime = System.currentTimeMillis()

                val data = if (isDemoMode) {
                    generateDemoData()
                } else {
                    // Real OBD data fetching
                    fetchRealOBDData()
                }

                // Reset error count on successful fetch
                if (errorCount > 0) {
                    errorCount = 0
                }

                data
            } catch (e: Exception) {
                errorCount++
                throw Exception("OBD data fetch failed (attempt $dataFetchCount, error $errorCount): ${e.message}")
            }
        }
    }

    // Real OBD data fetching method
    private suspend fun fetchRealOBDData(): VehicleData {
        return try {
            if (!obdCommunicator.isConnected()) {
                throw Exception("OBD device not connected")
            }

            // Query all parameters
            val speed = obdCommunicator.queryParameter("0D")?.toInt() ?: 0
            val rpm = obdCommunicator.queryParameter("0C")?.toInt() ?: 0
            val engineTemp = obdCommunicator.queryParameter("05")?.toInt() ?: 0
            val fuelLevel = obdCommunicator.queryParameter("2F")?.toInt() ?: 0
            val throttlePosition = obdCommunicator.queryParameter("11")?.toInt() ?: 0
            val batteryVoltage = obdCommunicator.queryParameter("42") ?: 0.0f

            val data = VehicleData(
                speed = speed.coerceIn(0, 300),
                rpm = rpm.coerceIn(0, 8000),
                engineTemp = engineTemp.coerceIn(-40, 150),
                fuelLevel = fuelLevel.coerceIn(0, 100),
                throttlePosition = throttlePosition.coerceIn(0, 100),
                batteryVoltage = batteryVoltage.coerceIn(8.0f, 16.0f),
                timestamp = System.currentTimeMillis()
            )

            if (isLogging) {
                dataLogger.logData(data)
            }

            data
        } catch (e: Exception) {
            throw Exception("Failed to read OBD data: ${e.message}")
        }
    }

    private fun generateDemoData(): VehicleData {
        // Simulate more realistic driving patterns
        when {
            demoSpeed < 20 && demoDirection == 1 -> {
                // Accelerating from stop
                demoSpeed += Random.nextInt(2, 8)
                demoRpm += Random.nextInt(100, 400)
            }
            demoSpeed > 60 && demoDirection == 1 -> {
                // High speed, chance to decelerate
                if (Random.nextFloat() < 0.3f) {
                    demoDirection = -1
                }
                demoSpeed += Random.nextInt(-2, 5)
                demoRpm += Random.nextInt(-100, 200)
            }
            demoDirection == -1 -> {
                // Decelerating
                demoSpeed += Random.nextInt(-8, 2)
                demoRpm += Random.nextInt(-300, 100)
                if (demoSpeed < 10) {
                    demoDirection = 1
                }
            }
            else -> {
                // Normal driving
                demoSpeed += Random.nextInt(-3, 6)
                demoRpm += Random.nextInt(-150, 250)
                if (Random.nextFloat() < 0.1f) {
                    demoDirection = if (demoDirection == 1) -1 else 1
                }
            }
        }

        // Keep values in realistic ranges
        demoSpeed = demoSpeed.coerceIn(0, 120)
        demoRpm = demoRpm.coerceIn(700, 6000)

        val data = VehicleData(
            speed = demoSpeed,
            rpm = demoRpm,
            engineTemp = Random.nextInt(85, 105),
            fuelLevel = Random.nextInt(15, 85),
            throttlePosition = if (demoSpeed > 0) {
                when {
                    demoDirection == 1 -> Random.nextInt(20, 70)
                    demoSpeed > 30 -> Random.nextInt(5, 25)
                    else -> Random.nextInt(0, 15)
                }
            } else 0,
            batteryVoltage = 12.0f + Random.nextFloat() * 2.8f,
            timestamp = System.currentTimeMillis()
        )

        if (isLogging) {
            dataLogger.logData(data)
        }

        return data
    }

    fun startLogging() {
        isLogging = true
        dataLogger.startNewSession()
    }

    fun stopLogging() {
        isLogging = false
        dataLogger.endSession()
    }

    fun exportLogs(format: DataLogger.ExportFormat, context: Context): String? {
        return dataLogger.exportData(format, context)
    }

    fun getLogSessionCount(): Int {
        return dataLogger.getSessionCount()
    }

    fun clearLogs() {
        dataLogger.clearLogs()
    }

    // Additional methods for connection management
    suspend fun disconnect() {
        isConnected = false
        obdCommunicator.disconnect()
        if (!isDemoMode) {
            isDemoMode = false
        }
    }

    fun getConnectionStatus(): String {
        return when {
            isConnected && isDemoMode -> "Demo Mode (Fetches: $dataFetchCount, Errors: $errorCount)"
            isConnected -> "Connected (Fetches: $dataFetchCount, Errors: $errorCount)"
            else -> "Disconnected"
        }
    }

    // Real OBD connection method
    suspend fun connectToDevice(device: android.bluetooth.BluetoothDevice): Boolean {
        return try {
            val success = obdCommunicator.connect(device)
            if (success) {
                isConnected = true
                isDemoMode = false
                // Reset counters on new connection
                dataFetchCount = 0
                errorCount = 0
            }
            success
        } catch (e: Exception) {
            errorCount++
            false
        }
    }

    // DEBUG METHODS for toast notifications
    fun getDebugInfo(): String {
        val timeSinceLastFetch = if (lastFetchTime > 0) {
            (System.currentTimeMillis() - lastFetchTime) / 1000
        } else 0

        return "Fetches: $dataFetchCount | Errors: $errorCount | Last: ${timeSinceLastFetch}s ago | Mode: ${if (isDemoMode) "DEMO" else "REAL"}"
    }

    fun getDataFetchCount(): Int = dataFetchCount
    fun getErrorCount(): Int = errorCount
    fun getLastFetchTime(): Long = lastFetchTime

    companion object {
        @Volatile
        private var instance: OBDManager? = null

        fun getInstance(): OBDManager {
            return instance ?: synchronized(this) {
                instance ?: OBDManager().also { instance = it }
            }
        }
    }
}