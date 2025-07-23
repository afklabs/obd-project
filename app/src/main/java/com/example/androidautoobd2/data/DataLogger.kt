package com.example.androidautoobd2.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class DataLogger {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val dataPoints = mutableListOf<VehicleData>()
    private var sessionStartTime: Long = 0
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())

    enum class ExportFormat {
        JSON, CSV
    }

    fun startNewSession() {
        dataPoints.clear()
        sessionStartTime = System.currentTimeMillis()
    }

    fun logData(data: VehicleData) {
        dataPoints.add(data)
    }

    fun endSession() {
        // Session ended, data is ready for export
    }

    fun exportData(format: ExportFormat, context: Context): String? {
        if (dataPoints.isEmpty()) return null

        val fileName = "OBD_Log_${dateFormat.format(Date(sessionStartTime))}"
        val extension = when (format) {
            ExportFormat.JSON -> ".json"
            ExportFormat.CSV -> ".csv"
        }

        val file = File(context.filesDir, "$fileName$extension")

        return try {
            when (format) {
                ExportFormat.JSON -> exportAsJson(file)
                ExportFormat.CSV -> exportAsCsv(file)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun exportAsJson(file: File) {
        FileWriter(file).use { writer ->
            val logData = mapOf(
                "sessionStart" to dateFormat.format(Date(sessionStartTime)),
                "sessionEnd" to dateFormat.format(Date()),
                "dataPoints" to dataPoints
            )
            gson.toJson(logData, writer)
        }
    }

    private fun exportAsCsv(file: File) {
        FileWriter(file).use { writer ->
            writer.write("Timestamp,Speed (km/h),RPM,Engine Temp (°C),Fuel Level (%),Throttle (%),Battery (V)\n")

            dataPoints.forEach { data ->
                writer.write(
                    "${dateFormat.format(Date(data.timestamp))},${data.speed},${data.rpm}," +
                            "${data.engineTemp},${data.fuelLevel},${data.throttlePosition},${data.batteryVoltage}\n"
                )
            }
        }
    }
}