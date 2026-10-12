package com.example.tuner

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Choreographer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FloatingFpsService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var telemetryManager: SystemTelemetryManager

    private var currentFps = 120.0f
    private var lastFrameTimeNanos = 0L
    private var frameCallback: Choreographer.FrameCallback? = null

    override fun onCreate() {
        super.onCreate()
        telemetryManager = SystemTelemetryManager(applicationContext)
        createNotificationChannel()
        startForeground(1002, buildNotification())
        initFloatingOverlay()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "neo_fragx_fps_overlay",
                "NEO FRAGX HUD Mengambang",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, "neo_fragx_fps_overlay")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("NEO FRAGX — FPS Overlay Aktif")
            .setContentText("Menampilkan HUD frame & performa real-time di atas game.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun initFloatingOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (windowManager == null) return

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 180
        }

        // Create UI programmatically with glowing neon cyan style
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 14, 24, 14)
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#E60A0F17"))
                setStroke(3, Color.parseColor("#00F0FF"))
                cornerRadius = 24f
            }
            background = bg
        }

        val fpsTextView = TextView(this).apply {
            text = "120 FPS"
            setTextColor(Color.parseColor("#00F0FF"))
            textSize = 14f
            typeface = android.graphics.Typeface.MONOSPACE
            paint.isFakeBoldText = true
        }

        val infoTextView = TextView(this).apply {
            text = " • 48% RAM"
            setTextColor(Color.parseColor("#A0B4C8"))
            textSize = 11f
            paint.isFakeBoldText = true
        }

        container.addView(fpsTextView)
        container.addView(infoTextView)

        // Make overlay draggable across the screen
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    try {
                        windowManager?.updateViewLayout(container, params)
                    } catch (_: Exception) {}
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(container, params)
            overlayView = container
        } catch (_: Exception) {}

        // Start Choreographer real frame counter
        frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (lastFrameTimeNanos > 0) {
                    val deltaMs = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000.0f
                    val instantFps = (1000f / deltaMs.coerceAtLeast(1.0f)).coerceIn(10f, 240f)
                    currentFps = currentFps * 0.9f + instantFps * 0.1f
                }
                lastFrameTimeNanos = frameTimeNanos
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
        Choreographer.getInstance().postFrameCallback(frameCallback!!)

        // Update Text Every 400ms
        scope.launch {
            while (isActive) {
                val telemetry = telemetryManager.getTelemetry()
                fpsTextView.text = String.format("%.0f FPS", currentFps)
                infoTextView.text = String.format(" • %d%% RAM • %.0f°C", telemetry.usedRamPercent.toInt(), telemetry.batteryTempCelsius)
                delay(400L)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        frameCallback?.let { Choreographer.getInstance().removeFrameCallback(it) }
        scope.cancel()
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun startOverlay(context: Context) {
            val intent = Intent(context, FloatingFpsService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopOverlay(context: Context) {
            val intent = Intent(context, FloatingFpsService::class.java)
            context.stopService(intent)
        }
    }
}
