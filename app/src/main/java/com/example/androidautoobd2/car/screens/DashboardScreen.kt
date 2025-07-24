package com.example.androidautoobd2.car.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.car.app.CarToast
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.androidautoobd2.R
import com.example.androidautoobd2.data.VehicleData
import com.example.androidautoobd2.obd.OBDManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DashboardScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

    private val obdManager = OBDManager.getInstance()
    private var updateJob: Job? = null
    private var vehicleData = VehicleData()

    // Error state management
    private var isLoading = false
    private var errorMessage: String? = null

    // DEBUG/TOAST TRACKING VARIABLES
    private var updateCount = 0
    private var errorCount = 0
    private var lastSuccessTime = 0L
    private var debugInfo = ""

    init {
        lifecycle.addObserver(this)
        CarToast.makeText(carContext, "Dashboard initialized", CarToast.LENGTH_SHORT).show()
    }

    override fun onGetTemplate(): Template {
        // Loading state
        if (isLoading) {
            return MessageTemplate.Builder("Loading vehicle data...")
                .setTitle("OBD Dashboard")
                .setHeaderAction(Action.BACK)
                .setLoading(true)
                .build()
        }

        // Error state with recovery options
        errorMessage?.let { error ->
            return MessageTemplate.Builder(error)
                .setTitle("Connection Error")
                .setHeaderAction(Action.BACK)
                .addAction(
                    Action.Builder()
                        .setTitle("Retry")
                        .setOnClickListener {
                            CarToast.makeText(carContext, "Retrying connection...", CarToast.LENGTH_SHORT).show()
                            errorMessage = null
                            errorCount = 0
                            startDataUpdate()
                        }
                        .build()
                )
                .addAction(
                    Action.Builder()
                        .setTitle("Demo Mode")
                        .setOnClickListener {
                            CarToast.makeText(carContext, "Switching to demo mode...", CarToast.LENGTH_SHORT).show()
                            obdManager.setDemoMode(true)
                            errorMessage = null
                            errorCount = 0
                            startDataUpdate()
                        }
                        .build()
                )
                .build()
        }

        return buildNormalTemplate()
    }

    private fun buildNormalTemplate(): Template {
        // Update debug info
        val timeSinceLastUpdate = if (lastSuccessTime > 0) {
            (System.currentTimeMillis() - lastSuccessTime) / 1000
        } else 0

        debugInfo = "Updates: $updateCount | Errors: $errorCount | Last: ${timeSinceLastUpdate}s ago"

        val gridItems = mutableListOf<GridItem>()

        // DEBUG INFO as first item - FIXED: Use setText() instead of addText()
        val debugText = "Mode: ${if (obdManager.isDemoMode) "DEMO" else "REAL"} | ${obdManager.getConnectionStatus()}"
        gridItems.add(
            GridItem.Builder()
                .setTitle("Debug Status")
                .setText(debugText)
                .setOnClickListener {
                    CarToast.makeText(carContext, debugInfo, CarToast.LENGTH_LONG).show()
                }
                .build()
        )

        // Speed gauge with status indicator - FIXED: Use setText() for single text
        val speedStatus = if (vehicleData.speed > 0) "MOVING" else "STOPPED"
        gridItems.add(
            GridItem.Builder()
                .setTitle("Speed ($speedStatus)")
                .setText("${vehicleData.speed} km/h")
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_speed)
                    ).build()
                )
                .setOnClickListener {
                    CarToast.makeText(carContext, "Speed: ${vehicleData.speed} km/h - Status: $speedStatus", CarToast.LENGTH_SHORT).show()
                }
                .build()
        )

        // RPM gauge with engine status - FIXED: Use setText() for single text
        val engineStatus = when {
            vehicleData.rpm < 500 -> "OFF"
            vehicleData.rpm < 1000 -> "IDLE"
            vehicleData.rpm < 3000 -> "NORMAL"
            else -> "HIGH"
        }
        gridItems.add(
            GridItem.Builder()
                .setTitle("RPM ($engineStatus)")
                .setText("${vehicleData.rpm}")
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_rpm)
                    ).build()
                )
                .setOnClickListener {
                    CarToast.makeText(carContext, "Engine RPM: ${vehicleData.rpm} - $engineStatus", CarToast.LENGTH_SHORT).show()
                }
                .build()
        )

        // Engine Temperature with warning levels - FIXED: Use setText() for single text
        val tempStatus = when {
            vehicleData.engineTemp < 60 -> "COLD"
            vehicleData.engineTemp < 90 -> "WARMING"
            vehicleData.engineTemp < 110 -> "NORMAL"
            else -> "HOT"
        }
        gridItems.add(
            GridItem.Builder()
                .setTitle("Engine Temp ($tempStatus)")
                .setText("${vehicleData.engineTemp}°C")
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_temp)
                    ).build()
                )
                .setOnClickListener {
                    CarToast.makeText(carContext, "Engine temp: ${vehicleData.engineTemp}°C - $tempStatus", CarToast.LENGTH_SHORT).show()
                }
                .build()
        )

        // Fuel Level with range estimation - FIXED: Use setText() for single text
        val fuelStatus = when {
            vehicleData.fuelLevel < 15 -> "LOW"
            vehicleData.fuelLevel < 30 -> "QUARTER"
            vehicleData.fuelLevel < 70 -> "HALF"
            else -> "FULL"
        }
        gridItems.add(
            GridItem.Builder()
                .setTitle("Fuel ($fuelStatus)")
                .setText("${vehicleData.fuelLevel}%")
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_fuel)
                    ).build()
                )
                .setOnClickListener {
                    CarToast.makeText(carContext, "Fuel level: ${vehicleData.fuelLevel}% - $fuelStatus", CarToast.LENGTH_SHORT).show()
                }
                .build()
        )

        // Throttle Position with driving mode - FIXED: Use setText() for single text
        val throttleStatus = when {
            vehicleData.throttlePosition < 10 -> "COAST"
            vehicleData.throttlePosition < 30 -> "CRUISE"
            vehicleData.throttlePosition < 70 -> "ACCEL"
            else -> "FULL"
        }
        gridItems.add(
            GridItem.Builder()
                .setTitle("Throttle ($throttleStatus)")
                .setText("${vehicleData.throttlePosition}%")
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_throttle)
                    ).build()
                )
                .setOnClickListener {
                    CarToast.makeText(carContext, "Throttle: ${vehicleData.throttlePosition}% - $throttleStatus", CarToast.LENGTH_SHORT).show()
                }
                .build()
        )

        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle(if (updateJob?.isActive == true) "Stop" else "Start")
                    .setOnClickListener {
                        toggleDataUpdate()
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle(if (obdManager.isLogging) "Stop Log" else "Log")
                    .setOnClickListener {
                        toggleDataLogging()
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Test")
                    .setOnClickListener {
                        CarToast.makeText(carContext, "Test button pressed! Updates: $updateCount, Errors: $errorCount", CarToast.LENGTH_LONG).show()
                    }
                    .build()
            )
            .build()

        return GridTemplate.Builder()
            .setTitle("Vehicle Dashboard (Debug)")
            .setSingleList(ItemList.Builder().apply {
                gridItems.forEach { addItem(it) }
            }.build())
            .setHeaderAction(Action.BACK)
            .setActionStrip(actionStrip)
            .build()
    }

    private fun startDataUpdate() {
        CarToast.makeText(carContext, "🚀 Starting data updates... (Demo: ${obdManager.isDemoMode})", CarToast.LENGTH_SHORT).show()
        updateJob?.cancel()
        updateJob = lifecycleScope.launch {
            while (isActive) {
                try {
                    updateVehicleData()
                    updateCount++
                    lastSuccessTime = System.currentTimeMillis()

                    // Show progress every 10 updates
                    if (updateCount % 10 == 0) {
                        CarToast.makeText(carContext, "📊 Updates: $updateCount | Speed: ${vehicleData.speed} km/h", CarToast.LENGTH_SHORT).show()
                    }

                    // Show milestone updates
                    when (updateCount) {
                        1 -> CarToast.makeText(carContext, "✅ First data received successfully!", CarToast.LENGTH_SHORT).show()
                        5 -> CarToast.makeText(carContext, "🔄 Data stream stable (5 updates)", CarToast.LENGTH_SHORT).show()
                        25 -> CarToast.makeText(carContext, "⚡ 25 updates completed!", CarToast.LENGTH_SHORT).show()
                    }

                    invalidate()
                    delay(1000)
                } catch (e: Exception) {
                    errorCount++
                    val errorMsg = e.message?.take(30) ?: "Unknown error"
                    CarToast.makeText(carContext, "❌ Update failed ($errorCount): $errorMsg", CarToast.LENGTH_LONG).show()
                    errorMessage = "Update failed: ${e.message}"
                    invalidate()
                    break
                }
            }
        }
    }

    private fun stopDataUpdate() {
        CarToast.makeText(carContext, "⏹️ Stopping data updates... (Total: $updateCount)", CarToast.LENGTH_SHORT).show()
        updateJob?.cancel()
        updateJob = null
    }

    private fun toggleDataUpdate() {
        if (updateJob?.isActive == true) {
            stopDataUpdate()
        } else {
            startDataUpdate()
        }
        invalidate()
    }

    private suspend fun updateVehicleData() {
        try {
            isLoading = true

            val rawData = obdManager.getVehicleData()

            // Validate and coerce data
            vehicleData = VehicleData(
                speed = rawData.speed.coerceIn(0, 300),
                rpm = rawData.rpm.coerceIn(0, 8000),
                engineTemp = rawData.engineTemp.coerceIn(-40, 150),
                fuelLevel = rawData.fuelLevel.coerceIn(0, 100),
                throttlePosition = rawData.throttlePosition.coerceIn(0, 100),
                batteryVoltage = rawData.batteryVoltage.coerceIn(8.0f, 16.0f),
                timestamp = rawData.timestamp
            )

            errorMessage = null

        } catch (e: Exception) {
            errorMessage = "Failed to read vehicle data: ${e.message}"
            throw e
        } finally {
            isLoading = false
        }
    }

    private fun toggleDataLogging() {
        if (obdManager.isLogging) {
            obdManager.stopLogging()
            CarToast.makeText(carContext, "📝 Logging stopped. Session saved.", CarToast.LENGTH_SHORT).show()
        } else {
            obdManager.startLogging()
            CarToast.makeText(carContext, "📝 Logging started. Recording data...", CarToast.LENGTH_SHORT).show()
        }
        invalidate()
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        CarToast.makeText(carContext, "🏁 Dashboard screen started", CarToast.LENGTH_SHORT).show()
        startDataUpdate()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        CarToast.makeText(carContext, "⏸️ Dashboard screen stopped", CarToast.LENGTH_SHORT).show()
        stopDataUpdate()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        CarToast.makeText(carContext, "💀 Dashboard destroyed. Cleanup complete.", CarToast.LENGTH_SHORT).show()
        updateJob?.cancel()
        updateJob = null
        lifecycle.removeObserver(this)
    }
}