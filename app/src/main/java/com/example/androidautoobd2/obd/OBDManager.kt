package com.example.androidautoobd2.obd

import com.example.androidautoobd2.data.VehicleData
import com.example.androidautoobd2.data.DataLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

class OBDManager private constructor() {

    private val dataLogger = DataLogger()
    private var isDemoMode = true
    private var demoSpeed = 0
    private var demoRpm = 800

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
        // Simulate realistic driving data
        demoSpeed = (demoSpeed + Random.nextInt(-5, 10)).coerceIn(0, 180)
        demoRpm = (demoRpm + Random.nextInt(-200, 300)).coerceIn(800, 6000)

        val data = VehicleData(
            speed = demoSpeed,
            rpm = demoRpm,
            engineTemp = Random.nextInt(75, 95),
            fuelLevel = Random.nextInt(20, 80),
            throttlePosition = if (demoSpeed > 0) Random.nextInt(10, 60) else 0,
            batteryVoltage = 12.0f + Random.nextFloat() * 2.5f
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

    fun exportLogs(format: DataLogger.ExportFormat): String? {
        return dataLogger.exportData(format)
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