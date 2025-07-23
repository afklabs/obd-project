package com.example.androidautoobd2.obd

import android.content.Context
import com.example.androidautoobd2.data.VehicleData
import com.example.androidautoobd2.data.DataLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

class OBDManager private constructor() {

    private val dataLogger = DataLogger()
    private var demoSpeed = 0
    private var demoRpm = 800
    private var demoDirection = 1 // 1 for acceleration, -1 for deceleration

    var isDemoMode = true
        private set

    var isConnected = false
        private set

    var isLogging = false
        private set

    fun setDemoMode(enabled: Boolean) {
        isDemoMode = enabled
        isConnected = enabled
    }

    suspend fun getVehicleData(): VehicleData {
        return withContext(Dispatchers.IO) {
            if (isDemoMode) {
                generateDemoData()
            } else {
                // Real OBD data would be fetched here
                VehicleData()
            }
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
    fun disconnect() {
        isConnected = false
        if (!isDemoMode) {
            isDemoMode = false
        }
    }

    fun getConnectionStatus(): String {
        return when {
            isConnected && isDemoMode -> "Demo Mode"
            isConnected -> "Connected"
            else -> "Disconnected"
        }
    }

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