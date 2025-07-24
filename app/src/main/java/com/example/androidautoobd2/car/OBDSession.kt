package com.example.androidautoobd2.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import com.example.androidautoobd2.car.screens.MainScreen

class OBDSession : Session() {

    override fun onCreateScreen(intent: Intent): Screen {
        android.util.Log.d("OBDSession", "Creating main screen for Android Auto")
        return MainScreen(carContext)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        android.util.Log.d("OBDSession", "New intent received: ${intent.action}")
    }
}