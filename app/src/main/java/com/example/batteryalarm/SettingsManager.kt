package com.example.batteryalarm

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("battery_alarm_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ALARM_ENABLED = "alarm_enabled"
        private const val KEY_TARGET_PERCENTAGE = "target_percentage"
        private const val KEY_ALARM_VOLUME = "alarm_volume"
        private const val KEY_VIBRATE_ENABLED = "vibrate_enabled"
        private const val KEY_TONE_URI = "tone_uri"
        
        private const val KEY_OVERHEAT_ALARM_ENABLED = "overheat_alarm_enabled"
        private const val KEY_OVERHEAT_TEMP_THRESHOLD = "overheat_temp_threshold"
        private const val KEY_LOW_BATTERY_ALARM_ENABLED = "low_battery_alarm_enabled"
        private const val KEY_LOW_BATTERY_THRESHOLD = "low_battery_threshold"
    }

    var isAlarmEnabled: Boolean
        get() = prefs.getBoolean(KEY_ALARM_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ALARM_ENABLED, value).apply()

    var targetPercentage: Int
        get() = prefs.getInt(KEY_TARGET_PERCENTAGE, 100)
        set(value) = prefs.edit().putInt(KEY_TARGET_PERCENTAGE, value).apply()

    var alarmVolume: Float
        get() = prefs.getFloat(KEY_ALARM_VOLUME, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_ALARM_VOLUME, value).apply()

    var isVibrateEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATE_ENABLED, value).apply()

    var selectedToneUri: String?
        get() = prefs.getString(KEY_TONE_URI, null)
        set(value) = prefs.edit().putString(KEY_TONE_URI, value).apply()

    var isOverheatAlarmEnabled: Boolean
        get() = prefs.getBoolean(KEY_OVERHEAT_ALARM_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_OVERHEAT_ALARM_ENABLED, value).apply()

    var overheatTempThreshold: Float
        get() = prefs.getFloat(KEY_OVERHEAT_TEMP_THRESHOLD, 40.0f)
        set(value) = prefs.edit().putFloat(KEY_OVERHEAT_TEMP_THRESHOLD, value).apply()

    var isLowBatteryAlarmEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOW_BATTERY_ALARM_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_LOW_BATTERY_ALARM_ENABLED, value).apply()

    var lowBatteryThreshold: Int
        get() = prefs.getInt(KEY_LOW_BATTERY_THRESHOLD, 20)
        set(value) = prefs.edit().putInt(KEY_LOW_BATTERY_THRESHOLD, value).apply()
}
