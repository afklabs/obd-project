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

    init {
        lifecycle.addObserver(this)
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
                            .setOnClickListener { invalidate() }
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun addConnectionStatus(listBuilder: ItemList.Builder) {
        val statusText = when {
            obdManager.isConnected && obdManager.isDemoMode -> "Demo mode active"
            obdManager.isConnected -> "Connected to OBD device"
            else -> "Not connected"
        }

        listBuilder.addItem(
            Row.Builder()
                .setTitle("Connection Status")
                .addText(statusText)
                .build()
        )
    }

    private fun addDemoModeButton(listBuilder: ItemList.Builder) {
        listBuilder.addItem(
            Row.Builder()
                .setTitle(if (obdManager.isDemoMode) "Exit Demo Mode" else "Use Demo Mode")
                .addText("Simulated OBD data for testing")
                .setOnClickListener {
                    if (obdManager.isDemoMode) {
                        obdManager.setDemoMode(false)
                        CarToast.makeText(carContext, "Demo mode deactivated", CarToast.LENGTH_SHORT).show()
                    } else {
                        obdManager.setDemoMode(true)
                        CarToast.makeText(carContext, "Demo mode activated", CarToast.LENGTH_SHORT).show()
                        screenManager.pop()
                    }
                    invalidate()
                }
                .build()
        )
    }

    private fun addScanControls(listBuilder: ItemList.Builder) {
        listBuilder.addItem(
            Row.Builder()
                .setTitle(if (isScanning) "Scanning..." else "Scan for Devices")
                .addText(if (isScanning) "Searching for Bluetooth OBD devices" else "Search for Bluetooth devices")
                .setOnClickListener {
                    if (!isScanning) {
                        startScan()
                    } else {
                        stopScan()
                    }
                }
                .build()
        )
    }

    private fun addDeviceList(listBuilder: ItemList.Builder) {
        if (discoveredDevices.isNotEmpty()) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle(if (isScanning) "Found Devices" else "Available Devices")
                    .build()
            )

            discoveredDevices.forEach { device ->
                listBuilder.addItem(
                    Row.Builder()
                        .setTitle(device.name)
                        .addText("${device.address} ${if (device.isConnectable) "" else "(Not responding)"}")
                        .setOnClickListener {
                            if (device.isConnectable) {
                                connectToDevice(device)
                            } else {
                                CarToast.makeText(carContext, "Device not responding", CarToast.LENGTH_SHORT).show()
                            }
                        }
                        .setEnabled(device.isConnectable)
                        .build()
                )
            }
        }
    }

    private fun startScan() {
        isScanning = true
        CarToast.makeText(carContext, "Scanning for devices...", CarToast.LENGTH_SHORT).show()

        invalidate()

        scanJob = lifecycleScope.launch {
            try {
                bluetoothHelper.startDiscovery { devices ->
                    discoveredDevices.clear()
                    discoveredDevices.addAll(devices)
                    invalidate()
                }

                // Stop scanning after 10 seconds
                delay(10000)
                bluetoothHelper.stopDiscovery()

                if (isScanning) {
                    CarToast.makeText(carContext, "Scan completed", CarToast.LENGTH_SHORT).show()
                    isScanning = false
                    invalidate()
                }
            } catch (e: Exception) {
                isScanning = false
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
        CarToast.makeText(carContext, "Connecting to ${device.name}...", CarToast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                // Simulate connection delay
                delay(2000)

                // For demo, just activate demo mode
                obdManager.setDemoMode(true)
                CarToast.makeText(carContext, "Connected successfully!", CarToast.LENGTH_SHORT).show()
                screenManager.pop()
            } catch (e: Exception) {
                CarToast.makeText(carContext, "Connection failed", CarToast.LENGTH_LONG).show()
                invalidate()
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        scanJob?.cancel()
        bluetoothHelper.stopDiscovery()
    }
}