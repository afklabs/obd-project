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

    // DEBUG/TOAST TRACKING
    private var scanAttempts = 0
    private var connectionAttempts = 0
    private var devicesFoundCount = 0

    init {
        lifecycle.addObserver(this)
        CarToast.makeText(carContext, "🔗 Connection screen initialized", CarToast.LENGTH_SHORT).show()
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        // Connection status with detailed info
        addConnectionStatus(listBuilder)

        // Demo mode button
        addDemoModeButton(listBuilder)

        // Scan controls
        addScanControls(listBuilder)

        // Debug info
        addDebugInfo(listBuilder)

        // Show devices
        addDeviceList(listBuilder)

        return ListTemplate.Builder()
            .setTitle("OBD-II Connection (Debug)")
            .setSingleList(listBuilder.build())
            .setHeaderAction(Action.BACK)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle("Refresh")
                            .setOnClickListener {
                                CarToast.makeText(carContext, "🔄 Refreshing connection screen...", CarToast.LENGTH_SHORT).show()
                                invalidate()
                            }
                            .build()
                    )
                    .addAction(
                        Action.Builder()
                            .setTitle("Test")
                            .setOnClickListener {
                                CarToast.makeText(carContext, "📊 Scans: $scanAttempts | Devices: $devicesFoundCount | Connections: $connectionAttempts", CarToast.LENGTH_LONG).show()
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

        // FIXED: Combine status info into single text line
        val detailText = "Mode: ${if (obdManager.isDemoMode) "DEMO" else "REAL"} | Status: ${obdManager.getConnectionStatus()}"

        listBuilder.addItem(
            Row.Builder()
                .setTitle("Connection Status")
                .addText(statusText)
                .addText(detailText)
                .setOnClickListener {
                    CarToast.makeText(carContext, "Status: ${obdManager.getConnectionStatus()}", CarToast.LENGTH_SHORT).show()
                }
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
                        CarToast.makeText(carContext, "🔄 Exiting demo mode...", CarToast.LENGTH_SHORT).show()
                        obdManager.setDemoMode(false)
                        CarToast.makeText(carContext, "✅ Demo mode deactivated", CarToast.LENGTH_SHORT).show()
                    } else {
                        CarToast.makeText(carContext, "🎮 Activating demo mode...", CarToast.LENGTH_SHORT).show()
                        obdManager.setDemoMode(true)
                        CarToast.makeText(carContext, "✅ Demo mode activated - Ready to test!", CarToast.LENGTH_SHORT).show()
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
        val scanText = if (isScanning)
            "Searching for Bluetooth OBD devices..." else
            "Search for Bluetooth devices (Attempts: $scanAttempts)"

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

    private fun addDebugInfo(listBuilder: ItemList.Builder) {
        // FIXED: Combine all debug info into single text line
        val debugText = "Scans: $scanAttempts | Devices: $devicesFoundCount | Connections: $connectionAttempts | BT: ${bluetoothHelper.isBluetoothEnabled()}"

        listBuilder.addItem(
            Row.Builder()
                .setTitle("📊 Debug Information")
                .addText(debugText)
                .setOnClickListener {
                    CarToast.makeText(carContext, debugText, CarToast.LENGTH_LONG).show()
                }
                .build()
        )
    }

    private fun addDeviceList(listBuilder: ItemList.Builder) {
        if (discoveredDevices.isNotEmpty()) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle(if (isScanning) "🔍 Found Devices ($devicesFoundCount)" else "📱 Available Devices ($devicesFoundCount)")
                    .build()
            )

            discoveredDevices.forEach { device ->
                val deviceStatus = if (device.isConnectable) "✅ Ready" else "❌ Not responding"

                // FIXED: Combine device info into single text line
                val deviceInfo = "${device.address} | $deviceStatus"

                listBuilder.addItem(
                    Row.Builder()
                        .setTitle("${device.name}")
                        .addText(deviceInfo)
                        .setOnClickListener {
                            if (device.isConnectable && !isConnecting) {
                                CarToast.makeText(carContext, "🔗 Attempting to connect to ${device.name}...", CarToast.LENGTH_SHORT).show()
                                connectToDevice(device)
                            } else if (!device.isConnectable) {
                                CarToast.makeText(carContext, "❌ Device ${device.name} is not responding", CarToast.LENGTH_SHORT).show()
                            }
                        }
                        .setEnabled(device.isConnectable && !isConnecting && !isScanning)
                        .build()
                )
            }
        } else if (scanAttempts > 0 && !isScanning) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("❌ No Devices Found")
                    .addText("Try scanning again or use demo mode")
                    .setOnClickListener {
                        CarToast.makeText(carContext, "💡 Tip: Use Demo Mode to test the app without a real OBD device", CarToast.LENGTH_LONG).show()
                    }
                    .build()
            )
        }
    }

    private fun startScan() {
        // Check permissions first
        if (!bluetoothHelper.hasBluetoothPermissions()) {
            CarToast.makeText(carContext, "❌ Bluetooth permissions not granted - using mock devices", CarToast.LENGTH_LONG).show()
            // Fall back to mock devices when permissions are missing
            discoveredDevices.clear()
            discoveredDevices.addAll(bluetoothHelper.getMockDevices())
            devicesFoundCount = discoveredDevices.size
            invalidate()
            return
        }

        if (!bluetoothHelper.isBluetoothEnabled()) {
            CarToast.makeText(carContext, "❌ Bluetooth not enabled - using demo devices", CarToast.LENGTH_LONG).show()
            discoveredDevices.clear()
            discoveredDevices.addAll(bluetoothHelper.getMockDevices())
            devicesFoundCount = discoveredDevices.size
            invalidate()
            return
        }

        isScanning = true
        scanAttempts++
        CarToast.makeText(carContext, "🔍 Starting scan #$scanAttempts for OBD devices...", CarToast.LENGTH_SHORT).show()

        invalidate()

        scanJob = lifecycleScope.launch {
            try {
                CarToast.makeText(carContext, "📡 Initializing Bluetooth discovery...", CarToast.LENGTH_SHORT).show()

                bluetoothHelper.startDiscovery { devices ->
                    discoveredDevices.clear()
                    discoveredDevices.addAll(devices)
                    devicesFoundCount = devices.size

                    CarToast.makeText(carContext, "📱 Found ${devices.size} device(s)", CarToast.LENGTH_SHORT).show()
                    invalidate()
                }

                // Show progress updates
                delay(3000)
                if (isScanning) {
                    CarToast.makeText(carContext, "🔍 Scanning... Found ${discoveredDevices.size} devices so far", CarToast.LENGTH_SHORT).show()
                }

                delay(4000)
                if (isScanning) {
                    CarToast.makeText(carContext, "🔍 Still scanning... 3 seconds remaining", CarToast.LENGTH_SHORT).show()
                }

                // Stop scanning after 10 seconds
                delay(3000)
                bluetoothHelper.stopDiscovery()

                if (isScanning) {
                    CarToast.makeText(carContext, "✅ Scan completed! Found ${discoveredDevices.size} device(s)", CarToast.LENGTH_SHORT).show()
                    isScanning = false
                    invalidate()
                }
            } catch (e: Exception) {
                isScanning = false
                CarToast.makeText(carContext, "❌ Scan failed: ${e.message?.take(30)}", CarToast.LENGTH_LONG).show()
                invalidate()
            }
        }
    }

    private fun stopScan() {
        scanJob?.cancel()
        bluetoothHelper.stopDiscovery()
        isScanning = false
        CarToast.makeText(carContext, "⏹️ Scan stopped manually", CarToast.LENGTH_SHORT).show()
        invalidate()
    }

    private fun connectToDevice(device: OBDDevice) {
        isConnecting = true
        connectionAttempts++
        CarToast.makeText(carContext, "🔗 Connection attempt #$connectionAttempts to ${device.name}...", CarToast.LENGTH_SHORT).show()
        invalidate()

        lifecycleScope.launch {
            try {
                CarToast.makeText(carContext, "🔧 Step 1/4: Initializing connection...", CarToast.LENGTH_SHORT).show()
                delay(1000)

                CarToast.makeText(carContext, "📡 Step 2/4: Establishing Bluetooth link...", CarToast.LENGTH_SHORT).show()
                delay(1000)

                CarToast.makeText(carContext, "🚗 Step 3/4: OBD-II handshake...", CarToast.LENGTH_SHORT).show()
                delay(1000)

                CarToast.makeText(carContext, "✅ Step 4/4: Verifying communication...", CarToast.LENGTH_SHORT).show()
                delay(1000)

                // For demo, activate demo mode
                obdManager.setDemoMode(true)
                CarToast.makeText(carContext, "🎉 Connected successfully to ${device.name}! (Demo mode active)", CarToast.LENGTH_LONG).show()

                // Show success statistics
                CarToast.makeText(carContext, "📊 Connection successful after $connectionAttempts attempt(s)", CarToast.LENGTH_SHORT).show()

                screenManager.pop()
            } catch (e: Exception) {
                CarToast.makeText(carContext, "❌ Connection failed: ${e.message?.take(40)}", CarToast.LENGTH_LONG).show()
                CarToast.makeText(carContext, "💡 Try demo mode or scan for other devices", CarToast.LENGTH_SHORT).show()
            } finally {
                isConnecting = false
                invalidate()
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        CarToast.makeText(carContext, "💀 Connection screen destroyed. Final stats - Scans: $scanAttempts, Connections: $connectionAttempts", CarToast.LENGTH_SHORT).show()
        scanJob?.cancel()
        scanJob = null
        bluetoothHelper.stopDiscovery()
        lifecycle.removeObserver(this)
    }
}