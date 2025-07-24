package com.example.androidautoobd2.utils

object VehicleDataValidator {

    fun validateSpeed(speed: Int): Int {
        return speed.coerceIn(0, 300) // Max reasonable speed in km/h
    }

    fun validateRPM(rpm: Int): Int {
        return rpm.coerceIn(0, 8000) // Max reasonable RPM
    }

    fun validateEngineTemp(temp: Int): Int {
        return temp.coerceIn(-40, 150) // Celsius range
    }

    fun validateFuelLevel(level: Int): Int {
        return level.coerceIn(0, 100) // Percentage
    }

    fun validateThrottlePosition(position: Int): Int {
        return position.coerceIn(0, 100) // Percentage
    }

    fun validateBatteryVoltage(voltage: Float): Float {
        return voltage.coerceIn(8.0f, 16.0f) // 12V system range
    }

    fun isValidOBDResponse(response: String): Boolean {
        // Check for standard OBD-II response format
        return response.matches(Regex("^[0-9A-F ]+$")) &&
                response.length >= 4 &&
                !response.contains("NO DATA") &&
                !response.contains("ERROR") &&
                !response.contains("UNABLE TO CONNECT") &&
                !response.contains("BUS INIT")
    }

    fun sanitizeResponse(response: String): String {
        return response.trim()
            .replace(Regex("[\r\n\t]"), "")
            .replace(Regex("\\s+"), " ")
            .uppercase()
    }
}