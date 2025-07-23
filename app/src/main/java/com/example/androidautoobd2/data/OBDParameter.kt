package com.example.androidautoobd2.data

data class OBDParameter(
    val pid: String,
    val name: String,
    val unit: String,
    val command: String
) {
    companion object {
        fun getAllParameters(): List<OBDParameter> {
            return listOf(
                OBDParameter("0C", "Engine RPM", "rpm", "010C"),
                OBDParameter("0D", "Vehicle Speed", "km/h", "010D"),
                OBDParameter("05", "Engine Coolant Temperature", "°C", "0105"),
                OBDParameter("2F", "Fuel Level", "%", "012F"),
                OBDParameter("11", "Throttle Position", "%", "0111"),
                OBDParameter("42", "Battery Voltage", "V", "0142")
            )
        }
    }
}