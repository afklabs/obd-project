package com.example.androidautoobd2.car

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class OBDCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        // Allow all hosts for compatibility with different Android Auto versions
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        android.util.Log.d("OBDCarAppService", "Creating new OBD session")
        return OBDSession()
    }

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("OBDCarAppService", "Car App Service Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("OBDCarAppService", "Car App Service Started with intent: ${intent?.action}")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        android.util.Log.d("OBDCarAppService", "Car App Service Destroyed")
        super.onDestroy()
    }
}