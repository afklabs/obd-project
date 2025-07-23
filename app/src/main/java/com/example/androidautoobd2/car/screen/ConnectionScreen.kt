package com.example.androidautoobd2.car.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.car.app.CarToast // ✅ Make sure this import is included
import com.example.androidautoobd2.obd.OBDManager

class ConnectionScreen(carContext: CarContext) : Screen(carContext) {

    private val obdManager = OBDManager.getInstance()

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        // Demo mode button
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Use Demo Mode")
                .addText("Simulated OBD data")
                .setOnClickListener {
                    obdManager.setDemoMode(true)
                    CarToast.makeText(carContext, "Demo mode activated", CarToast.LENGTH_SHORT).show()
                    screenManager.pop()
                }
                .build()
        )

        // Scan button
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Scan for OBD-II Devices")
                .addText("Search for Bluetooth devices")
                .setOnClickListener {
                    CarToast.makeText(carContext, "Scanning...", CarToast.LENGTH_SHORT).show()
                }
                .build()
        )

        // Mock devices for demonstration
        listBuilder.addItem(
            Row.Builder()
                .setTitle("ELM327 v1.5")
                .addText("00:11:22:33:44:55")
                .setOnClickListener {
                    CarToast.makeText(carContext, "Connecting...", CarToast.LENGTH_SHORT).show()
                }
                .build()
        )

        return ListTemplate.Builder()
            .setTitle("OBD-II Connection")
            .setSingleList(listBuilder.build())
            .setHeaderAction(Action.BACK)
            .build()
    }
}
