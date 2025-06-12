package com.example.czujnik_swiatla

import AppDatabase
import LightReading
import LightReadingAdapter
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.czujnik_swiatla.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.text.Editable
import android.text.TextWatcher

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private lateinit var adapter: LightReadingAdapter
    private lateinit var db: AppDatabase

    private val NOTIFICATION_CHANNEL_ID = "lux_threshold_channel"
    private val NOTIFICATION_ID = 1
    private var minThreshold: Float = 0f
    private var maxThreshold: Float = 1000f

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

        binding.clearButton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                db.lightReadingDao().deleteAll()
                withContext(Dispatchers.Main) {
                    adapter.updateData(emptyList())
                }
            }
        }

        createNotificationChannel()
        loadThresholds()

        // Automatyczna aktualizacja przy zmianie tekstu
        binding.minThresholdEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateThresholds()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.maxThresholdEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateThresholds()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
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

            // Powiadomienia o przekroczeniu progu
            if (lux < minThreshold) {
                showThresholdNotification("Light level too low: $lux lux")
            } else if (lux > maxThreshold) {
                showThresholdNotification("Light level too high: $lux lux")
            }

            // Zapis do bazy i odświeżenie listy
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
        // Nie używane
    }

    private fun loadReadings() {
        lifecycleScope.launch(Dispatchers.IO) {
            val readings = db.lightReadingDao().getAll()
            withContext(Dispatchers.Main) {
                adapter.updateData(readings)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Lux Threshold Alerts"
            val descriptionText = "Notifications for lux threshold violations"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Uprawnienia na Androidzie 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }

    private fun showThresholdNotification(message: String) {
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_light_bulb) // Upewnij się, że ten plik istnieje w res/drawable
            .setContentTitle("Lux Threshold Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(NOTIFICATION_ID, builder.build())
            }
        }
    }

    private fun updateThresholds() {
        try {
            minThreshold = binding.minThresholdEditText.text.toString().toFloatOrNull() ?: 0f
            maxThreshold = binding.maxThresholdEditText.text.toString().toFloatOrNull() ?: 1000f

            val prefs = getSharedPreferences("lux_prefs", Context.MODE_PRIVATE)
            with(prefs.edit()) {
                putFloat("min_threshold", minThreshold)
                putFloat("max_threshold", maxThreshold)
                apply()
            }
        } catch (e: NumberFormatException) {
            binding.minThresholdEditText.error = "Invalid number"
            binding.maxThresholdEditText.error = "Invalid number"
        }
    }

    private fun loadThresholds() {
        val prefs = getSharedPreferences("lux_prefs", Context.MODE_PRIVATE)
        minThreshold = prefs.getFloat("min_threshold", 0f)
        maxThreshold = prefs.getFloat("max_threshold", 1000f)
        binding.minThresholdEditText.setText(minThreshold.toString())
        binding.maxThresholdEditText.setText(maxThreshold.toString())
    }
}
