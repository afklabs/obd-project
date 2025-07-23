package com.example.androidautoobd2.car.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template

// Import other screen classes — adjust package paths if needed
import com.example.androidautoobd2.car.screens.DashboardScreen
import com.example.androidautoobd2.car.screens.ConnectionScreen
import com.example.androidautoobd2.car.screens.SettingsScreen

class MainScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        // Connection Status Row
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Connection Status")
                .addText("Demo Mode")
                .setOnClickListener {
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
                    screenManager.push(SettingsScreen(carContext))
                }
                .build()
        )

        return ListTemplate.Builder()
            .setTitle("OBD-II Monitor")
            .setHeaderAction(Action.APP_ICON) // Optional: add back button if needed
            .setSingleList(listBuilder.build())
            .build()
    }
}
