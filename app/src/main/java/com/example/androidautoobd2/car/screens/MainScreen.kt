package com.example.androidautoobd2.car.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.example.androidautoobd2.obd.OBDManager

class MainScreen(carContext: CarContext) : Screen(carContext) {

    private val obdManager = OBDManager.getInstance()

    override fun onGetTemplate(): Template {
        android.util.Log.d("MainScreen", "Building main screen template")

        val listBuilder = ItemList.Builder()

        // Connection Status Row
        val connectionStatus = when {
            obdManager.isConnected && obdManager.isDemoMode -> "🎮 Demo Mode Active"
            obdManager.isConnected -> "✅ Connected to OBD Device"
            else -> "❌ Not Connected"
        }

        listBuilder.addItem(
            Row.Builder()
                .setTitle("Connection Status")
                .addText(connectionStatus)
                .setOnClickListener {
                    android.util.Log.d("MainScreen", "Connection screen clicked")
                    screenManager.push(ConnectionScreen(carContext))
                }
                .build()
        )

        // Dashboard Row
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Vehicle Dashboard")
                .addText("View real-time OBD data")
                .setOnClickListener {
                    android.util.Log.d("MainScreen", "Dashboard screen clicked")
                    screenManager.push(DashboardScreen(carContext))
                }
                .build()
        )

        // Settings Row
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Settings")
                .addText("Configure parameters and preferences")
                .setOnClickListener {
                    android.util.Log.d("MainScreen", "Settings screen clicked")
                    screenManager.push(SettingsScreen(carContext))
                }
                .build()
        )

        // Demo Mode Toggle (if not already in demo mode)
        if (!obdManager.isDemoMode) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("🎮 Enable Demo Mode")
                    .addText("Use simulated data for testing")
                    .setOnClickListener {
                        android.util.Log.d("MainScreen", "Demo mode activated")
                        obdManager.setDemoMode(true)
                        invalidate() // Refresh the screen
                    }
                    .build()
            )
        }

        android.util.Log.d("MainScreen", "Main screen template built successfully")

        return ListTemplate.Builder()
            .setTitle("OBD-II Monitor")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(listBuilder.build())
            .build()
    }
}