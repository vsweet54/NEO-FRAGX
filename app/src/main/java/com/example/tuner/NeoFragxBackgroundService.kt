package com.example.tuner

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NeoFragxBackgroundService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private lateinit var gameOptimizer: GameOptimizer
    private lateinit var shellExecutor: ShellExecutor
    private lateinit var networkOptimizer: NetworkLatencyOptimizer
    private lateinit var thermalManager: ThermalSafetyManager
    private lateinit var telemetryManager: SystemTelemetryManager
    private val notificationId = 1001
    private val channelId = "neo_fragx_background_channel"

    override fun onCreate() {
        super.onCreate()
        gameOptimizer = GameOptimizer(applicationContext)
        shellExecutor = ShellExecutor(applicationContext)
        networkOptimizer = NetworkLatencyOptimizer(applicationContext)
        thermalManager = ThermalSafetyManager(applicationContext)
        telemetryManager = SystemTelemetryManager(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_CLEAN_NOW) {
            serviceScope.launch {
                val (_, freed) = gameOptimizer.cleanAllJunkAndCache()
                val mb = freed / (1024 * 1024)
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                updateNotification("Pembersihan manual selesai: membebaskan ~$mb MB ($timeStr)")
            }
        } else if (action == ACTION_STOP_SERVICE) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(notificationId, buildNotification("Engine Gaming Aktif di Latar Belakang"))

        // Start background monitoring and persistent cleaner loop
        startBackgroundLoops()

        return START_STICKY
    }

    private fun startBackgroundLoops() {
        // Loop 1: Persistent Touch Responsiveness Watchdog (Checks every 25s)
        serviceScope.launch {
            while (isActive) {
                if (gameOptimizer.isLightningTouchEnabled()) {
                    applyPersistentTouch()
                }
                delay(25000L)
            }
        }

        // Loop 2: Auto Junk Cleaner Loop
        serviceScope.launch {
            while (isActive) {
                val isAutoCleaner = gameOptimizer.isAutoJunkCleanerEnabled()
                val intervalMins = gameOptimizer.getAutoCleanIntervalMinutes().coerceAtLeast(5)

                if (isAutoCleaner) {
                    val (_, freed) = gameOptimizer.cleanAllJunkAndCache()
                    val mb = freed / (1024 * 1024)
                    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    updateNotification("Otomatis dibersihkan: ~$mb MB dibebaskan ($timeStr)")
                }

                delay(intervalMins * 60 * 1000L)
            }
        }

        // Loop 3: Network Latency Optimizer & Periodic DNS Flush (Every 60s)
        serviceScope.launch {
            while (isActive) {
                if (networkOptimizer.isAutoDnsFlushEnabled()) {
                    networkOptimizer.flushDnsCache()
                }
                if (networkOptimizer.isTrafficShapingEnabled()) {
                    networkOptimizer.applyTrafficShaping()
                }
                delay(60000L)
            }
        }

        // Loop 4: Thermal Safety Guard Watchdog (Every 10s)
        serviceScope.launch {
            while (isActive) {
                if (thermalManager.isThermalAlertEnabled()) {
                    val telemetry = telemetryManager.getTelemetry()
                    val limit = thermalManager.getThermalLimitCelsius()
                    if (telemetry.batteryTempCelsius >= limit && !thermalManager.isCoolingModeActive()) {
                        thermalManager.triggerThermalWarningVibration()
                        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        updateNotification("⚠️ PERINGATAN PANAS: Suhu ${telemetry.batteryTempCelsius}°C melebihi batas aman ($timeStr)")
                    }
                }
                delay(10000L)
            }
        }
    }

    private suspend fun applyPersistentTouch() {
        // 1. Direct Settings if write permitted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(this)) {
            try {
                Settings.System.putInt(contentResolver, "pointer_speed", 7)
            } catch (_: Exception) {}
        }

        // 2. Shell / Root if available
        val rootStatus = shellExecutor.checkRootAndShizuku()
        if (rootStatus.hasRoot) {
            shellExecutor.executeCommand("settings put system pointer_speed 7", preferRoot = true)
            shellExecutor.executeCommand("setprop debug.touch.press_delay 0", preferRoot = true)
            shellExecutor.executeCommand("setprop view.touch_slop 4", preferRoot = true)
            shellExecutor.executeCommand("setprop touch.pressure.scale 0.001", preferRoot = true)
        } else {
            shellExecutor.executeCommand("settings put system pointer_speed 7", preferRoot = false)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "NEO FRAGX Active Engine",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menjaga responsivitas sentuhan dan pembersih sampah otomatis tetap aktif saat keluar aplikasi."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(subText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cleanIntent = Intent(this, NeoFragxBackgroundService::class.java).apply {
            action = ACTION_CLEAN_NOW
        }
        val pendingClean = PendingIntent.getService(
            this, 1, cleanIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val touchStatus = if (gameOptimizer.isLightningTouchEnabled()) "⚡ Sentuhan: AKTIF" else "Sentuhan: Normal"
        val autoJunkStatus = if (gameOptimizer.isAutoJunkCleanerEnabled()) "🧹 Auto Clean: ON (${gameOptimizer.getAutoCleanIntervalMinutes()}m)" else "Auto Clean: OFF"

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("NEO FRAGX — Gaming Engine Berjalan")
            .setContentText("$touchStatus | $autoJunkStatus")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$touchStatus\n$autoJunkStatus\n$subText"))
            .setOngoing(true)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_menu_rotate, "Bersihkan Sekarang", pendingClean)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(subText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(notificationId, buildNotification(subText))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_CLEAN_NOW = "com.example.action.CLEAN_NOW"
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_SERVICE"

        fun startService(context: Context) {
            val intent = Intent(context, NeoFragxBackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, NeoFragxBackgroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
