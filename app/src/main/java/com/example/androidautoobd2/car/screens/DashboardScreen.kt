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
    private var isLoading = false
    private var errorMessage: String? = null
    private var updateCount = 0

    init {
        lifecycle.addObserver(this)
        android.util.Log.d("DashboardScreen", "Dashboard initialized")
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

        // Error state
        errorMessage?.let { error ->
            return MessageTemplate.Builder(error)
                .setTitle("Connection Error")
                .setHeaderAction(Action.BACK)
                .addAction(
                    Action.Builder()
                        .setTitle("Retry")
                        .setOnClickListener {
                            errorMessage = null
                            startDataUpdate()
                        }
                        .build()
                )
                .addAction(
                    Action.Builder()
                        .setTitle("Demo Mode")
                        .setOnClickListener {
                            obdManager.setDemoMode(true)
                            errorMessage = null
                            startDataUpdate()
                        }
                        .build()
                )
                .build()
        }

        return buildDashboardTemplate()
    }

    private fun buildDashboardTemplate(): Template {
        val gridItems = mutableListOf<GridItem>()

        // Speed
        gridItems.add(
            GridItem.Builder()
                .setTitle("Speed")
                .setText("${vehicleData.speed} km/h")
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_speed)
                    ).build()
                )
                .build()
        )

        // RPM
        gridItems.add(
            GridItem.Builder()
                .setTitle("RPM")
                .setText("${vehicleData.rpm}")
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_rpm)
                    ).build()
                )
                .build()
        )

        // Engine Temperature
        gridItems.add(
            GridItem.Builder()
                .setTitle("Engine Temp")
                .setText("${vehicleData.engineTemp}°C")
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_temp)
                    ).build()
                )
                .build()
        )

        // Fuel Level
        gridItems.add(
            GridItem.Builder()
                .setTitle("Fuel Level")
                .setText("${vehicleData.fuelLevel}%")
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_fuel)
                    ).build()
                )
                .build()
        )

        // Throttle Position
        gridItems.add(
            GridItem.Builder()
                .setTitle("Throttle")
                .setText("${vehicleData.throttlePosition}%")
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_throttle)
                    ).build()
                )
                .build()
        )

        // Battery Voltage
        gridItems.add(
            GridItem.Builder()
                .setTitle("Battery")
                .setText("${String.format("%.1f", vehicleData.batteryVoltage)}V")
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_battery)
                    ).build()
                )
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
            .build()

        return GridTemplate.Builder()
            .setTitle("Vehicle Dashboard")
            .setSingleList(ItemList.Builder().apply {
                gridItems.forEach { addItem(it) }
            }.build())
            .setHeaderAction(Action.BACK)
            .setActionStrip(actionStrip)
            .build()
    }

    private fun startDataUpdate() {
        updateJob?.cancel()
        updateJob = lifecycleScope.launch {
            while (isActive) {
                try {
                    updateVehicleData()
                    updateCount++
                    invalidate()
                    delay(1000) // Update every second
                } catch (e: Exception) {
                    android.util.Log.e("DashboardScreen", "Error updating data", e)
                    errorMessage = "Update failed: ${e.message}"
                    invalidate()
                    break
                }
            }
        }
    }

    private fun stopDataUpdate() {
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
            CarToast.makeText(carContext, "Logging stopped", CarToast.LENGTH_SHORT).show()
        } else {
            obdManager.startLogging()
            CarToast.makeText(carContext, "Logging started", CarToast.LENGTH_SHORT).show()
        }
        invalidate()
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        android.util.Log.d("DashboardScreen", "Dashboard started")
        startDataUpdate()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        android.util.Log.d("DashboardScreen", "Dashboard stopped")
        stopDataUpdate()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        android.util.Log.d("DashboardScreen", "Dashboard destroyed")
        updateJob?.cancel()
        lifecycle.removeObserver(this)
    }
}