package com.example.androidautoobd2.car

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class OBDCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        // FIXED: Use ALLOW_ALL_HOSTS_VALIDATOR for simplicity
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return OBDSession()
    }

    // FIXED: Override to handle service lifecycle properly
    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("OBDCarAppService", "Car App Service Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("OBDCarAppService", "Car App Service Started")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        android.util.Log.d("OBDCarAppService", "Car App Service Destroyed")
        super.onDestroy()
    }
}