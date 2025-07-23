package com.example.androidautoobd2.data

data class VehicleData(
    var speed: Int = 0,
    var rpm: Int = 0,
    var engineTemp: Int = 0,
    var fuelLevel: Int = 0,
    var throttlePosition: Int = 0,
    var batteryVoltage: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)