package com.example.androidautoobd2.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

data class OBDDevice(
    val name: String,
    val address: String,
    val isConnectable: Boolean = true
)

class BluetoothHelper(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter = bluetoothManager?.adapter
    private val discoveredDevices = mutableSetOf<BluetoothDevice>()
    private var isDiscovering = false
    private var discoveryCallback: ((List<OBDDevice>) -> Unit)? = null
    private var isReceiverRegistered = false

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    device?.let { discoveredDevice ->
                        if (isObdDevice(discoveredDevice)) {
                            discoveredDevices.add(discoveredDevice)
                            notifyDevicesFound()
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    isDiscovering = false
                    notifyDevicesFound()
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    isDiscovering = true
                    discoveredDevices.clear()
                }
            }
        }
    }

    fun startDiscovery(callback: (List<OBDDevice>) -> Unit) {
        if (!checkBluetoothPermissions()) {
            callback(getMockDevices())
            return
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            callback(getMockDevices())
            return
        }

        discoveryCallback = callback
        discoveredDevices.clear()

        // Register receiver if not already registered
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }

            try {
                context.registerReceiver(discoveryReceiver, filter)
                isReceiverRegistered = true
            } catch (e: Exception) {
                callback(getMockDevices())
                return
            }
        }

        // Add paired devices that might be OBD devices
        try {
            bluetoothAdapter.bondedDevices?.forEach { device ->
                if (isObdDevice(device)) {
                    discoveredDevices.add(device)
                }
            }
            notifyDevicesFound()
        } catch (e: SecurityException) {
            // Handle permission error
        }

        // Start discovery
        try {
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            bluetoothAdapter.startDiscovery()
        } catch (e: SecurityException) {
            // Handle permission error - fall back to mock devices
            callback(getMockDevices())
        }
    }

    fun stopDiscovery() {
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
            if (isReceiverRegistered) {
                context.unregisterReceiver(discoveryReceiver)
                isReceiverRegistered = false
            }
        } catch (e: Exception) {
            // Handle unregister errors
        }
        isDiscovering = false
        discoveryCallback = null
    }

    private fun notifyDevicesFound() {
        val obdDevices = discoveredDevices.mapNotNull { device ->
            try {
                OBDDevice(
                    name = device.name ?: "Unknown Device",
                    address = device.address,
                    isConnectable = true
                )
            } catch (e: SecurityException) {
                null
            }
        }

        // If no real devices found, add mock devices for testing
        val devicesToReturn = if (obdDevices.isEmpty()) {
            getMockDevices()
        } else {
            obdDevices
        }

        discoveryCallback?.invoke(devicesToReturn)
    }

    private fun getMockDevices(): List<OBDDevice> {
        return listOf(
            OBDDevice("ELM327 v1.5", "00:11:22:33:44:55", true),
            OBDDevice("OBDLink LX", "AA:BB:CC:DD:EE:FF", true),
            OBDDevice("BAFX Products 34t5", "12:34:56:78:90:AB", false),
            OBDDevice("Veepeak OBDCheck BLE", "FE:DC:BA:98:76:54", true)
        )
    }

    private fun isObdDevice(device: BluetoothDevice): Boolean {
        val deviceName = try {
            device.name?.lowercase()
        } catch (e: SecurityException) {
            null
        }

        return deviceName?.let { name ->
            // Common OBD-II device names and patterns
            name.contains("elm") ||
                    name.contains("obd") ||
                    name.contains("obdlink") ||
                    name.contains("scantool") ||
                    name.contains("veepeak") ||
                    name.contains("bafx") ||
                    name.contains("foseal") ||
                    name.contains("kobra") ||
                    name.contains("carista") ||
                    name.contains("torque") ||
                    name.startsWith("v") && name.length < 10 // Many cheap adapters use "v1.5" etc
        } ?: false
    }

    private fun checkBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_ADMIN) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
}