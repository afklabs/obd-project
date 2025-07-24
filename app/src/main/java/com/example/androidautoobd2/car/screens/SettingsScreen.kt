package com.example.androidautoobd2.car.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import com.example.androidautoobd2.data.OBDParameter
import com.example.androidautoobd2.utils.PreferencesManager

class SettingsScreen(carContext: CarContext) : Screen(carContext) {

    private val preferencesManager = PreferencesManager(carContext)
    private val availableParameters = OBDParameter.getAllParameters()

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        // Update frequency setting
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Update Frequency")
                .addText("${preferencesManager.getUpdateFrequency()} ms")
                .setOnClickListener {
                    showFrequencyDialog()
                }
                .build()
        )

        // Parameter selection header
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Select Parameters to Display")
                .setBrowsable(false)
                .build()
        )

        // Parameter toggles - limiting to 6 main parameters for Android Auto
        availableParameters.take(6).forEach { parameter ->
            val isEnabled = preferencesManager.isParameterEnabled(parameter.pid)

            listBuilder.addItem(
                Row.Builder()
                    .setTitle(parameter.name)
                    .addText("${parameter.unit} | ${if (isEnabled) "Enabled" else "Disabled"}")
                    .setToggle(
                        Toggle.Builder { enabled ->
                            preferencesManager.setParameterEnabled(parameter.pid, enabled)
                            invalidate()
                        }
                            .setChecked(isEnabled)
                            .build()
                    )
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setTitle("Settings")
            .setSingleList(listBuilder.build())
            .setHeaderAction(Action.BACK)
            .build()
    }

    private fun showFrequencyDialog() {
        val frequencies = listOf(500, 1000, 2000, 5000)
        val listBuilder = ItemList.Builder()

        frequencies.forEach { freq ->
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("$freq ms")
                    .addText(when (freq) {
                        500 -> "Fast updates (more CPU usage)"
                        1000 -> "Normal updates (recommended)"
                        2000 -> "Slow updates"
                        5000 -> "Very slow updates"
                        else -> ""
                    })
                    .setOnClickListener {
                        preferencesManager.setUpdateFrequency(freq)
                        screenManager.pop()
                    }
                    .build()
            )
        }

        val frequencyScreen = object : Screen(carContext) {
            override fun onGetTemplate(): Template {
                return ListTemplate.Builder()
                    .setTitle("Select Update Frequency")
                    .setSingleList(listBuilder.build())
                    .setHeaderAction(Action.BACK)
                    .build()
            }
        }

        screenManager.push(frequencyScreen)
    }
}