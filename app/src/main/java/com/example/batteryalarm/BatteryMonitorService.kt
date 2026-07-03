package com.example.batteryalarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

class BatteryMonitorService : Service() {

    private lateinit var settingsManager: SettingsManager
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    
    // Alarm and Snooze State variables
    private var activeAlarmType: String? = null // "FULL_CHARGE", "OVERHEAT", "LOW_BATTERY"
    private var isSnoozed = false
    
    // Cached battery values for condition checking
    private var currentPercentage = -1
    private var currentTemp = 0.0f
    private var isCharging = false
    private var currentVoltage = 0
    private var currentHealth = "Healthy"

    private val snoozeHandler = Handler(Looper.getMainLooper())
    private val snoozeRunnable = Runnable {
        isSnoozed = false
        checkBatteryStateAndTrigger()
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                    val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
                    val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
                    
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                 status == BatteryManager.BATTERY_STATUS_FULL

                    currentPercentage = if (level >= 0 && scale > 0) {
                        (level * 100 / scale)
                    } else {
                        0
                    }
                    currentTemp = temp / 10.0f
                    currentVoltage = voltage
                    currentHealth = when (health) {
                        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
                        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
                        else -> "Healthy"
                    }

                    checkBatteryStateAndTrigger()
                    updateNotification()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    isCharging = false
                    // Automatically stop full charge or overheating alarms on disconnect
                    if (activeAlarmType == "FULL_CHARGE" || activeAlarmType == "OVERHEAT") {
                        stopAlarm()
                    }
                    cancelSnooze()
                    updateNotification()
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    isCharging = true
                    // Automatically stop low battery alarms on connect
                    if (activeAlarmType == "LOW_BATTERY") {
                        stopAlarm()
                    }
                    cancelSnooze()
                    updateNotification()
                }
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "BatteryMonitorChannel"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_STOP_ALARM = "com.example.batteryalarm.ACTION_STOP_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.example.batteryalarm.ACTION_SNOOZE_ALARM"
        const val ACTION_START_MONITOR = "com.example.batteryalarm.ACTION_START_MONITOR"
    }

    override fun onCreate() {
        super.onCreate()
        settingsManager = SettingsManager(this)
        
        // Initialize Vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        createNotificationChannel()

        // Register battery receiver dynamically
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(batteryReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_ALARM -> stopAlarm()
            ACTION_SNOOZE_ALARM -> snoozeAlarm()
            else -> {
                // Initialize foreground state
                val initialNotification = buildStatusNotification("Monitoring active...", "", false)
                startForeground(NOTIFICATION_ID, initialNotification)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
        cancelSnooze()
        stopAlarm()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Battery Charger Alarm Service"
            val descriptionText = "Monitors charging level to alert you when battery is full."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setSound(null, null) // Keep service notifications silent
                enableVibration(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildStatusNotification(contentText: String, statsText: String, showActions: Boolean): Notification {
        val mainActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainActivityPendingIntent = PendingIntent.getActivity(
            this, 0, mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Charge Guard Status")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(mainActivityPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$contentText\n\n$statsText")
            )

        if (showActions && activeAlarmType != null) {
            // Dismiss Action
            val stopAlarmIntent = Intent(this, BatteryMonitorService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            val stopAlarmPendingIntent = PendingIntent.getService(
                this, 1, stopAlarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Dismiss",
                stopAlarmPendingIntent
            )

            // Snooze Action
            val snoozeIntent = Intent(this, BatteryMonitorService::class.java).apply {
                action = ACTION_SNOOZE_ALARM
            }
            val snoozePendingIntent = PendingIntent.getService(
                this, 2, snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_lock_idle_alarm,
                "Snooze (5m)",
                snoozePendingIntent
            )
        }

        return builder.build()
    }

    private fun checkBatteryStateAndTrigger() {
        if (activeAlarmType != null || isSnoozed) return

        // 1. Overheat Alarm (Only when charging)
        if (settingsManager.isOverheatAlarmEnabled && isCharging && currentTemp >= settingsManager.overheatTempThreshold) {
            triggerAlarm("OVERHEAT")
            return
        }

        // 2. Full Charge Alarm (Only when charging)
        if (settingsManager.isAlarmEnabled && isCharging && currentPercentage >= settingsManager.targetPercentage) {
            triggerAlarm("FULL_CHARGE")
            return
        }

        // 3. Low Battery Alarm (Only when discharging)
        if (settingsManager.isLowBatteryAlarmEnabled && !isCharging && currentPercentage > 0 && currentPercentage <= settingsManager.lowBatteryThreshold) {
            triggerAlarm("LOW_BATTERY")
            return
        }
    }

    private fun updateNotification() {
        val percentage = if (currentPercentage >= 0) {
            "$currentPercentage%"
        } else {
            val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val currentLvl = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            "$currentLvl%"
        }

        val statusText = when {
            activeAlarmType != null -> {
                when (activeAlarmType) {
                    "OVERHEAT" -> "⚠️ Battery Overheating! Temp: $currentTemp °C"
                    "LOW_BATTERY" -> "🪫 Battery Low! Connect charger ($percentage)"
                    else -> "🎉 Battery charged to target! ($percentage)"
                }
            }
            isSnoozed -> {
                "Snoozed - Will alert again in 5 mins ($percentage)"
            }
            isCharging -> {
                val targetText = if (settingsManager.isAlarmEnabled) "Target: ${settingsManager.targetPercentage}%" else ""
                val overheatText = if (settingsManager.isOverheatAlarmEnabled) "Max: ${settingsManager.overheatTempThreshold}°C" else ""
                val details = listOf(targetText, overheatText).filter { it.isNotEmpty() }.joinToString(", ")
                "Charging ($percentage) - $details"
            }
            else -> {
                val lowText = if (settingsManager.isLowBatteryAlarmEnabled) "Low alert: ${settingsManager.lowBatteryThreshold}%" else ""
                "Discharging ($percentage) - $lowText"
            }
        }

        // Fetch live current
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val currentMicro = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val currentmA = if (Math.abs(currentMicro) > 10000) currentMicro / 1000 else currentMicro
        val currentmAStr = if (isCharging && currentmA > 0) "+$currentmA mA" else "$currentmA mA"

        // Format voltage
        val voltStr = if (currentVoltage <= 0) {
            "N/A"
        } else if (currentVoltage > 1000) {
            String.format("%.2f V", currentVoltage / 1000f)
        } else {
            String.format("%d V", currentVoltage)
        }

        // Format health and other statistics inside the expanded notification panel
        val statsText = """
            🔋 Battery level: $percentage
            🌡️ Temperature: $currentTemp °C
            ⚡ Amperage (Current): $currentmAStr
            ⚡ Voltage: $voltStr
            ❤️ Battery Health: $currentHealth
        """.trimIndent()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(
            NOTIFICATION_ID, 
            buildStatusNotification(statusText, statsText, activeAlarmType != null)
        )
    }

    private fun triggerAlarm(type: String) {
        if (activeAlarmType != null) return
        activeAlarmType = type
        updateNotification()

        // Play alarm sound
        try {
            val toneUriStr = settingsManager.selectedToneUri
            val alertUri: Uri = if (!toneUriStr.isNullOrEmpty()) {
                Uri.parse(toneUriStr)
            } else {
                android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
                    ?: android.provider.Settings.System.DEFAULT_RINGTONE_URI
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@BatteryMonitorService, alertUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                prepare()
                setVolume(settingsManager.alarmVolume, settingsManager.alarmVolume)
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback play simple tone if URI fails
            try {
                mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI).apply {
                    isLooping = true
                    start()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        // Vibrate
        if (settingsManager.isVibrateEnabled && vibrator != null) {
            val pattern = longArrayOf(0, 1000, 500, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        }
    }

    private fun snoozeAlarm() {
        if (activeAlarmType == null) return
        
        // Stop currently playing sound and vibration
        silenceAlarmHardware()
        
        isSnoozed = true
        activeAlarmType = null
        
        // Schedule snooze timer for 5 minutes (300,000 ms)
        snoozeHandler.removeCallbacks(snoozeRunnable)
        snoozeHandler.postDelayed(snoozeRunnable, 5 * 60 * 1000)
        
        updateNotification()
    }

    private fun stopAlarm() {
        silenceAlarmHardware()
        activeAlarmType = null
        cancelSnooze()
        updateNotification()
    }

    private fun silenceAlarmHardware() {
        // Stop media player
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
        }

        // Stop vibration
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cancelSnooze() {
        isSnoozed = false
        snoozeHandler.removeCallbacks(snoozeRunnable)
    }
}
