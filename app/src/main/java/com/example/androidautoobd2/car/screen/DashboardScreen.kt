import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.car.app.CarToast // Import CarToast
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.androidautoobd2.R
import com.example.androidautoobd2.data.VehicleData
import com.example.androidautoobd2.obd.OBDManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DashboardScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

    private val obdManager = OBDManager.getInstance()
    private var updateJob: Job? = null
    private var vehicleData = VehicleData()

    init {
        lifecycle.addObserver(this)
    }

    override fun onGetTemplate(): Template {
        // Sport meter style grid
        val gridItems = mutableListOf<GridItem>()

        // Speed gauge
        gridItems.add(
            GridItem.Builder()
                .setTitle("Speed")
                .setText("${vehicleData.speed} km/h")
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_speed)
                    ).build()
                )
                .build()
        )

        // RPM gauge
        gridItems.add(
            GridItem.Builder()
                .setTitle("RPM")
                .setText("${vehicleData.rpm}")
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_rpm)
                    ).build()
                )
                .build()
        )

        // Engine Temperature
        gridItems.add(
            GridItem.Builder()
                .setTitle("Engine Temp")
                .setText("${vehicleData.engineTemp}°C")
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_temp)
                    ).build()
                )
                .build()
        )

        // Fuel Level
        gridItems.add(
            GridItem.Builder()
                .setTitle("Fuel Level")
                .setText("${vehicleData.fuelLevel}%")
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_fuel)
                    ).build()
                )
                .build()
        )

        // Throttle Position
        gridItems.add(
            GridItem.Builder()
                .setTitle("Throttle")
                .setText("${vehicleData.throttlePosition}%")
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_throttle)
                    ).build()
                )
                .build()
        )

        // Battery Voltage
        gridItems.add(
            GridItem.Builder()
                .setTitle("Battery")
                .setText("${String.format("%.1f", vehicleData.batteryVoltage)}V")
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_battery)
                    ).build()
                )
                .build()
        )

        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle(if (updateJob?.isActive == true) "Stop" else "Start")
                    .setOnClickListener {
                        toggleDataUpdate()
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle(if (obdManager.isLogging) "Stop Log" else "Log")
                    .setOnClickListener {
                        toggleDataLogging()
                    }
                    .build()
            )
            .build()

        return GridTemplate.Builder()
            .setTitle("Vehicle Dashboard")
            .setSingleList(ItemList.Builder().apply {
                gridItems.forEach { addItem(it) }
            }.build())
            .setHeaderAction(Action.BACK)
            .setActionStrip(actionStrip)
            .build()
    }

    private fun startDataUpdate() {
        updateJob?.cancel()
        updateJob = lifecycleScope.launch {
            while (isActive) {
                updateVehicleData()
                invalidate()
                delay(1000) // Update every second
            }
        }
    }

    private fun stopDataUpdate() {
        updateJob?.cancel()
        updateJob = null
    }

    private fun toggleDataUpdate() {
        if (updateJob?.isActive == true) {
            stopDataUpdate()
        } else {
            startDataUpdate()
        }
        invalidate()
    }

    private suspend fun updateVehicleData() {
        vehicleData = obdManager.getVehicleData()
    }

    private fun toggleDataLogging() {
        if (obdManager.isLogging) {
            obdManager.stopLogging()
            CarToast.makeText(carContext, "Logging stopped", CarToast.LENGTH_SHORT).show()
        } else {
            obdManager.startLogging()
            CarToast.makeText(carContext, "Logging started", CarToast.LENGTH_SHORT).show()
        }
        invalidate()
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        startDataUpdate()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        stopDataUpdate()
    }
}