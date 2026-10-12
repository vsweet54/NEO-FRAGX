package com.example.tuner

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ThermalSafetyManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("neo_fragx_thermal", Context.MODE_PRIVATE)
    private val shellExecutor = ShellExecutor(context)

    private val KEY_THERMAL_ALERT_ENABLED = "thermal_alert_enabled"
    private val KEY_THERMAL_LIMIT_CELSIUS = "thermal_limit_celsius"
    private val KEY_COOLING_MODE_ACTIVE = "cooling_mode_active"

    fun isThermalAlertEnabled(): Boolean = prefs.getBoolean(KEY_THERMAL_ALERT_ENABLED, true)
    fun setThermalAlertEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_THERMAL_ALERT_ENABLED, enabled).apply()

    fun getThermalLimitCelsius(): Float = prefs.getFloat(KEY_THERMAL_LIMIT_CELSIUS, 43.5f)
    fun setThermalLimitCelsius(temp: Float) = prefs.edit().putFloat(KEY_THERMAL_LIMIT_CELSIUS, temp).apply()

    fun isCoolingModeActive(): Boolean = prefs.getBoolean(KEY_COOLING_MODE_ACTIVE, false)
    fun setCoolingModeActive(active: Boolean) = prefs.edit().putBoolean(KEY_COOLING_MODE_ACTIVE, active).apply()

    fun triggerThermalWarningVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 150), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 150), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(300)
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun applyCoolingDownMode(enable: Boolean): String = withContext(Dispatchers.IO) {
        setCoolingModeActive(enable)
        val rootStatus = shellExecutor.checkRootAndShizuku()
        val msg = StringBuilder()

        if (enable) {
            if (rootStatus.hasRoot) {
                // Throttle refresh rate to 60Hz to reduce heat output
                shellExecutor.executeCommand("settings put system peak_refresh_rate 60.0", preferRoot = true)
                shellExecutor.executeCommand("settings put system min_refresh_rate 60.0", preferRoot = true)
                // Set governor to powersave
                shellExecutor.executeCommand("echo powersave > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", preferRoot = true)
            }
            // Trigger haptic alert
            triggerThermalWarningVibration()
            msg.append("Mode Pendinginan Aktif: Frekuensi refresh rate dibatasi ke 60Hz & Governor diturunkan sementara untuk meredakan suhu SoC.")
        } else {
            if (rootStatus.hasRoot) {
                shellExecutor.executeCommand("settings put system peak_refresh_rate 120.0", preferRoot = true)
                shellExecutor.executeCommand("echo schedutil > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", preferRoot = true)
            }
            msg.append("Mode Pendinginan Dinonaktifkan: Performa hardware kembali ke profil game standar.")
        }
        msg.toString()
    }
}
