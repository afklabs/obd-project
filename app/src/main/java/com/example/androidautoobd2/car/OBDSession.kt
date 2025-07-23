package com.example.androidautoobd2.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import com.example.androidautoobd2.car.screens.MainScreen

class OBDSession : Session() {

    override fun onCreateScreen(intent: Intent): Screen {
        return MainScreen(carContext)
    }
}