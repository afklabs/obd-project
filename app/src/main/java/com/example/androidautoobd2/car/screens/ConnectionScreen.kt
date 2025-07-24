package com.example.androidautoobd2.car.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.car.app.CarToast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.androidautoobd2.obd.OBDManager
import com.example.androidautoobd2.bluetooth.BluetoothHelper
import com.example.androidautoobd2.bluetooth.OBDDevice
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ConnectionScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

    private val obdManager = OBDManager.getInstance()
    private val bluetoothHelper = BluetoothHelper(carContext)
    private var isScanning = false
    private var scanJob: Job? = null
    private var discoveredDevices = mutableListOf<OBDDevice>()
    private var isConnecting = false

    init {
        lifecycle.addObserver(this)
        android.util.Log.d("ConnectionScreen", "Connection screen initialized")
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        // Connection status
        addConnectionStatus(listBuilder)

        // Demo mode button
        addDemoModeButton(listBuilder)

        // Scan controls
        addScanControls(listBuilder)

        // Show devices
        addDeviceList(listBuilder)

        return ListTemplate.Builder()
            .setTitle("OBD-II Connection")
            .setSingleList(listBuilder.build())
            .setHeaderAction(Action.BACK)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle("Refresh")
                            .setOnClickListener {
                                invalidate()
                            }
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun addConnectionStatus(listBuilder: ItemList.Builder) {
        val statusText = when {
            isConnecting -> "⏳ Connecting..."
            obdManager.isConnected && obdManager.isDemoMode -> "🎮 Demo mode active"
            obdManager.isConnected -> "✅ Connected to OBD device"
            else -> "❌ Not connected"
        }

        listBuilder.addItem(
            Row.Builder()
                .setTitle("Connection Status")
                .addText(statusText)
                .addText("Mode: ${if (obdManager.isDemoMode) "DEMO" else "REAL"}")
                .build()
        )
    }

    private fun addDemoModeButton(listBuilder: ItemList.Builder) {
        listBuilder.addItem(
            Row.Builder()
                .setTitle(if (obdManager.isDemoMode) "🔄 Exit Demo Mode" else "🎮 Use Demo Mode")
                .addText("Simulated OBD data for testing")
                .setOnClickListener {
                    if (obdManager.isDemoMode) {
                        CarToast.makeText(carContext, "Exiting demo mode...", CarToast.LENGTH_SHORT).show()
                        obdManager.setDemoMode(false)
                    } else {
                        CarToast.makeText(carContext, "Activating demo mode...", CarToast.LENGTH_SHORT).show()
                        obdManager.setDemoMode(true)
                        screenManager.pop()
                    }
                    invalidate()
                }
                .setEnabled(!isConnecting)
                .build()
        )
    }

    private fun addScanControls(listBuilder: ItemList.Builder) {
        val scanTitle = if (isScanning) "⏳ Scanning..." else "🔍 Scan for Devices"
        val scanText = if (isScanning) "Searching for Bluetooth OBD devices..." else "Search for Bluetooth devices"

        listBuilder.addItem(
            Row.Builder()
                .setTitle(scanTitle)
                .addText(scanText)
                .setOnClickListener {
                    if (!isScanning && !isConnecting) {
                        startScan()
                    } else if (isScanning) {
                        stopScan()
                    }
                }
                .setEnabled(!isConnecting)
                .build()
        )
    }

    private fun addDeviceList(listBuilder: ItemList.Builder) {
        if (discoveredDevices.isNotEmpty()) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("📱 Available Devices (${discoveredDevices.size})")
                    .build()
            )

            discoveredDevices.forEach { device ->
                val deviceStatus = if (device.isConnectable) "✅ Ready" else "❌ Not responding"

                listBuilder.addItem(
                    Row.Builder()
                        .setTitle(device.name)
                        .addText("${device.address} | $deviceStatus")
                        .setOnClickListener {
                            if (device.isConnectable && !isConnecting) {
                                CarToast.makeText(carContext, "Connecting to ${device.name}...", CarToast.LENGTH_SHORT).show()
                                connectToDevice(device)
                            } else if (!device.isConnectable) {
                                CarToast.makeText(carContext, "Device ${device.name} is not responding", CarToast.LENGTH_SHORT).show()
                            }
                        }
                        .setEnabled(device.isConnectable && !isConnecting && !isScanning)
                        .build()
                )
            }
        } else if (!isScanning) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("❌ No Devices Found")
                    .addText("Try scanning again or use demo mode")
                    .build()
            )
        }
    }

    private fun startScan() {
        // Check permissions first
        if (!bluetoothHelper.hasBluetoothPermissions()) {
            CarToast.makeText(carContext, "Bluetooth permissions not granted - using mock devices", CarToast.LENGTH_LONG).show()
            discoveredDevices.clear()
            discoveredDevices.addAll(bluetoothHelper.getMockDevices())
            invalidate()
            return
        }

        if (!bluetoothHelper.isBluetoothEnabled()) {
            CarToast.makeText(carContext, "Bluetooth not enabled - using demo devices", CarToast.LENGTH_LONG).show()
            discoveredDevices.clear()
            discoveredDevices.addAll(bluetoothHelper.getMockDevices())
            invalidate()
            return
        }

        isScanning = true
        CarToast.makeText(carContext, "Starting scan for OBD devices...", CarToast.LENGTH_SHORT).show()
        invalidate()

        scanJob = lifecycleScope.launch {
            try {
                bluetoothHelper.startDiscovery { devices ->
                    discoveredDevices.clear()
                    discoveredDevices.addAll(devices)
                    CarToast.makeText(carContext, "Found ${devices.size} device(s)", CarToast.LENGTH_SHORT).show()
                    invalidate()
                }

                // Stop scanning after 10 seconds
                delay(10000)
                bluetoothHelper.stopDiscovery()

                if (isScanning) {
                    CarToast.makeText(carContext, "Scan completed! Found ${discoveredDevices.size} device(s)", CarToast.LENGTH_SHORT).show()
                    isScanning = false
                    invalidate()
                }
            } catch (e: Exception) {
                isScanning = false
                CarToast.makeText(carContext, "Scan failed: ${e.message}", CarToast.LENGTH_LONG).show()
                invalidate()
            }
        }
    }

    private fun stopScan() {
        scanJob?.cancel()
        bluetoothHelper.stopDiscovery()
        isScanning = false
        CarToast.makeText(carContext, "Scan stopped", CarToast.LENGTH_SHORT).show()
        invalidate()
    }

    private fun connectToDevice(device: OBDDevice) {
        isConnecting = true
        CarToast.makeText(carContext, "Connecting to ${device.name}...", CarToast.LENGTH_SHORT).show()
        invalidate()

        lifecycleScope.launch {
            try {
                // Simulate connection process
                delay(2000)

                // For demo, activate demo mode
                obdManager.setDemoMode(true)
                CarToast.makeText(carContext, "Connected successfully to ${device.name}! (Demo mode active)", CarToast.LENGTH_LONG).show()

                screenManager.pop()
            } catch (e: Exception) {
                CarToast.makeText(carContext, "Connection failed: ${e.message}", CarToast.LENGTH_LONG).show()
            } finally {
                isConnecting = false
                invalidate()
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        android.util.Log.d("ConnectionScreen", "Connection screen destroyed")
        scanJob?.cancel()
        bluetoothHelper.stopDiscovery()
        lifecycle.removeObserver(this)
    }
}