package com.example.czujnik_swiatla

import AppDatabase
import LightReading
import LightReadingAdapter
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.czujnik_swiatla.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private lateinit var adapter: LightReadingAdapter
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        adapter = LightReadingAdapter(emptyList())
        binding.readingsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.readingsRecyclerView.adapter = adapter

        db = AppDatabase.getDatabase(this)

        // Handle Clear History button click
        binding.clearButton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                db.lightReadingDao().deleteAll()
                withContext(Dispatchers.Main) {
                    adapter.updateData(emptyList())
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lightSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        loadReadings()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_LIGHT) {
            val lux = event.values[0]
            binding.luxValue.text = "Lux: $lux"

            // Zapis do bazy asynchronicznie
            lifecycleScope.launch(Dispatchers.IO) {
                db.lightReadingDao().insert(
                    LightReading(
                        timestamp = System.currentTimeMillis(),
                        lux = lux
                    )
                )
                loadReadings()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Nic nie robimy
    }

    private fun loadReadings() {
        lifecycleScope.launch(Dispatchers.IO) {
            val readings = db.lightReadingDao().getAll()
            withContext(Dispatchers.Main) {
                adapter.updateData(readings)
            }
        }
    }
}