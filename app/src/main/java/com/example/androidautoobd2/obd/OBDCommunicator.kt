package com.example.androidautoobd2.obd

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class OBDCommunicator {

    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var isConnected = false

    companion object {
        private val OBD_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val COMMAND_TIMEOUT = 3000L // 3 seconds
    }

    suspend fun connect(device: BluetoothDevice): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                disconnect() // Ensure clean state

                bluetoothSocket = device.createRfcommSocketToServiceRecord(OBD_UUID)
                bluetoothSocket?.connect()

                inputStream = bluetoothSocket?.inputStream
                outputStream = bluetoothSocket?.outputStream

                // Initialize OBD connection
                if (initializeOBD()) {
                    isConnected = true
                    true
                } else {
                    disconnect()
                    false
                }
            } catch (e: Exception) {
                disconnect()
                false
            }
        }
    }

    suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                isConnected = false
                inputStream?.close()
                outputStream?.close()
                bluetoothSocket?.close()
            } catch (e: Exception) {
                // Ignore errors during cleanup
            } finally {
                inputStream = null
                outputStream = null
                bluetoothSocket = null
            }
        }
    }

    private suspend fun initializeOBD(): Boolean {
        return try {
            // Reset adapter
            sendCommand("ATZ") != null &&
                    // Turn off echo
                    sendCommand("ATE0") != null &&
                    // Set protocol to auto
                    sendCommand("ATSP0") != null
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendCommand(command: String): String? {
        return withContext(Dispatchers.IO) {
            if (!isConnected || outputStream == null || inputStream == null) {
                return@withContext null
            }

            try {
                withTimeoutOrNull(COMMAND_TIMEOUT) {
                    // Send command
                    outputStream?.write("$command\r".toByteArray())
                    outputStream?.flush()

                    // Read response
                    readResponse()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun readResponse(): String {
        return withContext(Dispatchers.IO) {
            val buffer = StringBuilder()
            val byteBuffer = ByteArray(1)

            try {
                while (true) {
                    val bytesRead = inputStream?.read(byteBuffer) ?: break
                    if (bytesRead > 0) {
                        val char = byteBuffer[0].toInt().toChar()
                        if (char == '>') {
                            break // End of response
                        } else if (char != '\r' && char != '\n') {
                            buffer.append(char)
                        }
                    }
                }
            } catch (e: Exception) {
                // Return partial response on error
            }

            buffer.toString().trim()
        }
    }

    suspend fun queryParameter(pid: String): Float? {
        val response = sendCommand("01$pid") ?: return null
        return parseResponse(response, pid)
    }

    private fun parseResponse(response: String, pid: String): Float? {
        if (response.contains("NO DATA") || response.contains("ERROR") || response.length < 4) {
            return null
        }

        return try {
            when (pid) {
                "0C" -> parseRPM(response)
                "0D" -> parseSpeed(response)
                "05" -> parseEngineTemp(response)
                "2F" -> parseFuelLevel(response)
                "11" -> parseThrottlePosition(response)
                "42" -> parseBatteryVoltage(response)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseRPM(response: String): Float? {
        // Response format: "41 0C XX XX" where RPM = ((A*256)+B)/4
        val hex = response.replace(" ", "")
        if (hex.length < 8) return null

        val a = hex.substring(4, 6).toInt(16)
        val b = hex.substring(6, 8).toInt(16)
        return ((a * 256) + b) / 4.0f
    }

    private fun parseSpeed(response: String): Float? {
        // Response format: "41 0D XX" where Speed = A
        val hex = response.replace(" ", "")
        if (hex.length < 6) return null

        return hex.substring(4, 6).toInt(16).toFloat()
    }

    private fun parseEngineTemp(response: String): Float? {
        // Response format: "41 05 XX" where Temp = A - 40
        val hex = response.replace(" ", "")
        if (hex.length < 6) return null

        return hex.substring(4, 6).toInt(16) - 40.0f
    }

    private fun parseFuelLevel(response: String): Float? {
        // Response format: "41 2F XX" where Fuel = A * 100/255
        val hex = response.replace(" ", "")
        if (hex.length < 6) return null

        return hex.substring(4, 6).toInt(16) * 100.0f / 255.0f
    }

    private fun parseThrottlePosition(response: String): Float? {
        // Response format: "41 11 XX" where Throttle = A * 100/255
        val hex = response.replace(" ", "")
        if (hex.length < 6) return null

        return hex.substring(4, 6).toInt(16) * 100.0f / 255.0f
    }

    private fun parseBatteryVoltage(response: String): Float? {
        // Response format: "41 42 XX XX" where Voltage = ((A*256)+B)/1000
        val hex = response.replace(" ", "")
        if (hex.length < 8) return null

        val a = hex.substring(4, 6).toInt(16)
        val b = hex.substring(6, 8).toInt(16)
        return ((a * 256) + b) / 1000.0f
    }

    fun isConnected(): Boolean = isConnected
}