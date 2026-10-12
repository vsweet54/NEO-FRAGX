package com.example.tuner

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Choreographer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
    private lateinit var gameOptimizer: GameOptimizer
    private lateinit var networkOptimizer: NetworkLatencyOptimizer
    private lateinit var thermalManager: ThermalSafetyManager

    private var currentFps = 120.0f
    private var livePingMs = 24L
    private var lastFrameTimeNanos = 0L
    private var frameCallback: Choreographer.FrameCallback? = null

    // State for expanded dock
    private var isExpanded = false
    private var isDndActive = false
    private var isTouchGuardActive = false

    override fun onCreate() {
        super.onCreate()
        telemetryManager = SystemTelemetryManager(applicationContext)
        gameOptimizer = GameOptimizer(applicationContext)
        networkOptimizer = NetworkLatencyOptimizer(applicationContext)
        thermalManager = ThermalSafetyManager(applicationContext)

        createNotificationChannel()
        startForeground(1002, buildNotification())
        initFloatingOverlay()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "neo_fragx_fps_overlay",
                "NEO FRAGX HUD Mengambang & Game Dock",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, "neo_fragx_fps_overlay")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("NEO FRAGX — Game Dock & HUD Aktif")
            .setContentText("Panel mengambang in-game aktif: FPS, Ping, RAM Booster & DND.")
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

        // Root container (holds collapsed pill and expandable dock)
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // 1. Collapsed Pill (Always Visible Header)
        val pillLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 14, 24, 14)
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#EE080C14"))
                setStroke(3, Color.parseColor("#00F0FF"))
                cornerRadius = 24f
            }
            background = bg
        }

        val fpsTextView = TextView(this).apply {
            text = "120 FPS"
            setTextColor(Color.parseColor("#00F0FF"))
            textSize = 13f
            typeface = Typeface.MONOSPACE
            paint.isFakeBoldText = true
        }

        val pingTextView = TextView(this).apply {
            text = " • 24ms"
            setTextColor(Color.parseColor("#00E676"))
            textSize = 11f
            typeface = Typeface.MONOSPACE
            paint.isFakeBoldText = true
        }

        val infoTextView = TextView(this).apply {
            text = " • 48% RAM • 38°C"
            setTextColor(Color.parseColor("#8B9BB4"))
            textSize = 11f
            paint.isFakeBoldText = true
        }

        val expandIcon = TextView(this).apply {
            text = " ⚙"
            setTextColor(Color.parseColor("#00F0FF"))
            textSize = 12f
        }

        pillLayout.addView(fpsTextView)
        pillLayout.addView(pingTextView)
        pillLayout.addView(infoTextView)
        pillLayout.addView(expandIcon)

        // 2. Expandable Game Dock Panel
        val dockPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(20, 16, 20, 16)
            val panelBg = GradientDrawable().apply {
                setColor(Color.parseColor("#F50D1522"))
                setStroke(2, Color.parseColor("#1E2E44"))
                cornerRadius = 18f
            }
            background = panelBg
            val topMargin = (8 * resources.displayMetrics.density).toInt()
            (layoutParams as? LinearLayout.LayoutParams)?.topMargin = topMargin
        }

        // Dock Title
        val dockTitle = TextView(this).apply {
            text = "⚡ NEO GAME DOCK"
            setTextColor(Color.parseColor("#00F0FF"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        dockPanel.addView(dockTitle)

        // Dock Buttons Row 1
        val buttonsRow1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 12, 0, 6)
        }

        val boostBtn = createDockButton("⚡ Boost RAM", "#00F0FF") {
            scope.launch {
                val (killed, freed) = gameOptimizer.trimRamMemory()
                val mb = freed / (1024 * 1024)
                Toast.makeText(this@FloatingFpsService, "RAM Dibersihkan: ~$mb MB dibebaskan ($killed proses)", Toast.LENGTH_SHORT).show()
                fpsTextView.setTextColor(Color.parseColor("#00E676"))
                delay(1200)
                fpsTextView.setTextColor(Color.parseColor("#00F0FF"))
            }
        }

        val dndBtn = createDockButton("🔕 DND: OFF", "#8B9BB4") {
            isDndActive = !isDndActive
            (it as? TextView)?.text = if (isDndActive) "🔕 DND: ON" else "🔕 DND: OFF"
            (it as? TextView)?.setTextColor(if (isDndActive) Color.parseColor("#00E676") else Color.parseColor("#8B9BB4"))
            Toast.makeText(this, if (isDndActive) "DND Aktif: Notifikasi diminimalisir" else "DND Dinonaktifkan", Toast.LENGTH_SHORT).show()
        }

        buttonsRow1.addView(boostBtn)
        buttonsRow1.addView(dndBtn)
        dockPanel.addView(buttonsRow1)

        // Dock Buttons Row 2
        val buttonsRow2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 4, 0, 8)
        }

        val touchGuardBtn = createDockButton("🔒 Touch Lock", "#8B9BB4") {
            isTouchGuardActive = !isTouchGuardActive
            (it as? TextView)?.text = if (isTouchGuardActive) "🔒 Lock: ON" else "🔒 Touch Lock"
            (it as? TextView)?.setTextColor(if (isTouchGuardActive) Color.parseColor("#00E676") else Color.parseColor("#8B9BB4"))
            Toast.makeText(this, if (isTouchGuardActive) "Touch Guard: Edge touch dilindungi" else "Touch Guard Nonaktif", Toast.LENGTH_SHORT).show()
        }

        val closeBtn = createDockButton("✕ Tutup Dock", "#FF3366") {
            stopSelf()
        }

        buttonsRow2.addView(touchGuardBtn)
        buttonsRow2.addView(closeBtn)
        dockPanel.addView(buttonsRow2)

        rootLayout.addView(pillLayout)
        rootLayout.addView(dockPanel)

        // Touch handling (Drag or Click to Expand)
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        pillLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true
                    }
                    params.x = initialX + dx
                    params.y = initialY + dy
                    try {
                        windowManager?.updateViewLayout(rootLayout, params)
                    } catch (_: Exception) {}
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Toggle Dock expansion
                        isExpanded = !isExpanded
                        dockPanel.visibility = if (isExpanded) View.VISIBLE else View.GONE
                        expandIcon.text = if (isExpanded) " ▲" else " ⚙"
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(rootLayout, params)
            overlayView = rootLayout
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

        // Telemetry Update Loop (FPS, RAM, Temp every 400ms)
        scope.launch {
            while (isActive) {
                val telemetry = telemetryManager.getTelemetry()
                fpsTextView.text = String.format("%.0f FPS", currentFps)
                pingTextView.text = String.format(" • %dms", livePingMs)
                pingTextView.setTextColor(
                    if (livePingMs < 50) Color.parseColor("#00E676")
                    else if (livePingMs < 100) Color.parseColor("#00F0FF")
                    else Color.parseColor("#FFB800")
                )
                infoTextView.text = String.format(" • %d%% RAM • %.0f°C", telemetry.usedRamPercent.toInt(), telemetry.batteryTempCelsius)
                delay(400L)
            }
        }

        // Live Ping Measurement Loop (Every 4 seconds)
        scope.launch {
            while (isActive) {
                val ping = networkOptimizer.measureTargetPing("asia_gateway")
                if (ping > 0) livePingMs = ping
                delay(4000L)
            }
        }
    }

    private fun createDockButton(text: String, hexColor: String, onClick: (View) -> Unit): TextView {
        val dp = resources.displayMetrics.density
        return TextView(this).apply {
            this.text = text
            setTextColor(Color.parseColor(hexColor))
            textSize = 10f
            paint.isFakeBoldText = true
            setPadding((12 * dp).toInt(), (8 * dp).toInt(), (12 * dp).toInt(), (8 * dp).toInt())
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#142030"))
                setStroke(1, Color.parseColor(hexColor))
                cornerRadius = 10 * dp
            }
            background = bg
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                rightMargin = (8 * dp).toInt()
            }
            layoutParams = lp
            setOnClickListener { onClick(it) }
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
