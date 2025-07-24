package com.example.androidautoobd2.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import com.example.androidautoobd2.car.screens.MainScreen

class OBDSession : Session() {

    override fun onCreateScreen(intent: Intent): Screen {
        android.util.Log.d("OBDSession", "Creating main screen")
        return MainScreen(carContext)
    }

    // FIXED: Override lifecycle methods for better debugging
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        android.util.Log.d("OBDSession", "New intent received: ${intent.action}")
    }
}