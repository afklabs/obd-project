package com.example.androidautoobd2.car.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template

// Import other screen classes
import com.example.androidautoobd2.car.screens.DashboardScreen
import com.example.androidautoobd2.car.screens.ConnectionScreen
import com.example.androidautoobd2.car.screens.SettingsScreen

class MainScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        android.util.Log.d("MainScreen", "Building main screen template")

        val listBuilder = ItemList.Builder()

        try {
            // Connection Status Row
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("Connection Status")
                    .addText("Demo Mode")
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
                    .addText("View real-time data")
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
                    .addText("Configure parameters")
                    .setOnClickListener {
                        android.util.Log.d("MainScreen", "Settings screen clicked")
                        screenManager.push(SettingsScreen(carContext))
                    }
                    .build()
            )

            android.util.Log.d("MainScreen", "Main screen template built successfully")

            return ListTemplate.Builder()
                .setTitle("OBD-II Monitor")
                .setHeaderAction(Action.APP_ICON)
                .setSingleList(listBuilder.build())
                .build()

        } catch (e: Exception) {
            android.util.Log.e("MainScreen", "Error building template", e)
            throw e
        }
    }
}