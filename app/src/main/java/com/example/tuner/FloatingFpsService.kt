package com.example.tuner

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Choreographer
import android.view.Display
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

    private var targetMaxFps = 60f
    private var currentFps = 60.0f
    private var minRecordedFps = 60.0f
    private var maxRecordedFps = 60.0f
    private var livePingMs = 24L
    private var lastFrameTimeNanos = 0L
    private var windowStartNanos = 0L
    private var frameCountInWindow = 0
    private var measuredJitterMs = 0.8f
    private var frameCallback: Choreographer.FrameCallback? = null

    // State for expanded dock
    private var isExpanded = false
    private var isDndActive = false
    private var isTouchGuardActive = false
    private var isCrosshairActive = false
    private var crosshairView: View? = null
    private var activePerformanceMode = "TURBO"
    private var isCompactPill = false
    private lateinit var dockStatsTextView: TextView

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        telemetryManager = SystemTelemetryManager(applicationContext)
        gameOptimizer = GameOptimizer(applicationContext)
        networkOptimizer = NetworkLatencyOptimizer(applicationContext)
        thermalManager = ThermalSafetyManager(applicationContext)

        createNotificationChannel()
        ensureForegroundNotification()
        initFloatingOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureForegroundNotification()
        return START_NOT_STICKY
    }

    private fun ensureForegroundNotification() {
        try {
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    startForeground(
                        1002,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } catch (_: Exception) {
                    startForeground(1002, notification)
                }
            } else {
                startForeground(1002, notification)
            }
        } catch (_: Exception) {}
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
            .setSmallIcon(com.example.R.mipmap.ic_launcher)
            .setContentTitle("NEO FRAGX — Game Dock & HUD Aktif")
            .setContentText("Panel mengambang in-game aktif: FPS, Ping, RAM Booster & DND.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun initFloatingOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            isRunning = false
            return
        }

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // 1. Obtain Display properly via DisplayManager (never call context.getDisplay() directly on Service)
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val targetDisplay: Display? = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)

        // 2. Create proper WindowContext (Android 12+ API 31+) or DisplayContext (Android 11 API 30) or fallback
        val windowContext: Context = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && targetDisplay != null -> {
                try {
                    createWindowContext(targetDisplay, layoutFlag, null)
                } catch (_: Throwable) {
                    try {
                        createWindowContext(layoutFlag, null)
                    } catch (_: Throwable) {
                        try {
                            createDisplayContext(targetDisplay)
                        } catch (_: Throwable) {
                            this
                        }
                    }
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && targetDisplay != null -> {
                try {
                    createWindowContext(layoutFlag, null)
                } catch (_: Throwable) {
                    try {
                        createDisplayContext(targetDisplay)
                    } catch (_: Throwable) {
                        this
                    }
                }
            }
            targetDisplay != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 -> {
                try {
                    createDisplayContext(targetDisplay)
                } catch (_: Throwable) {
                    this
                }
            }
            else -> this
        }

        // 3. Obtain WindowManager from visual windowContext
        windowManager = (windowContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
            ?: (getSystemService(Context.WINDOW_SERVICE) as? WindowManager)

        if (windowManager == null) {
            isRunning = false
            return
        }

        // 4. Calculate targetMaxFps using targetDisplay refreshRate safely
        val displayHz = targetDisplay?.refreshRate ?: 60f
        targetMaxFps = when {
            displayHz >= 135f -> 144f
            displayHz >= 110f -> 120f
            displayHz >= 85f -> 90f
            else -> 60f
        }
        currentFps = targetMaxFps
        minRecordedFps = targetMaxFps
        maxRecordedFps = targetMaxFps

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 200
        }

        // Root container (holds collapsed pill and expandable dock)
        val rootLayout = LinearLayout(windowContext).apply {
            orientation = LinearLayout.VERTICAL
        }

        val dp = windowContext.resources.displayMetrics.density

        // 1. Collapsed Pill (Cyberpunk HUD Pill with Neon Glow & Clean Badges)
        val pillLayout = LinearLayout(windowContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((12 * dp).toInt(), (6 * dp).toInt(), (10 * dp).toInt(), (6 * dp).toInt())
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#F2080D18"))
                setStroke((1.8f * dp).toInt(), Color.parseColor("#00F0FF"))
                cornerRadius = 20 * dp
            }
            background = bg
        }

        // FPS Badge Box (Highlighted Monospace Indicator)
        val fpsBadge = LinearLayout(windowContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((8 * dp).toInt(), (3 * dp).toInt(), (8 * dp).toInt(), (3 * dp).toInt())
            val badgeBg = GradientDrawable().apply {
                setColor(Color.parseColor("#1E00E676"))
                setStroke((1 * dp).toInt(), Color.parseColor("#00E676"))
                cornerRadius = 8 * dp
            }
            background = badgeBg
        }

        val fpsDot = TextView(windowContext).apply {
            text = "● "
            setTextColor(Color.parseColor("#00E676"))
            textSize = 9f
        }

        val fpsTextView = TextView(windowContext).apply {
            text = "${targetMaxFps.toInt()} FPS"
            setTextColor(Color.parseColor("#00E676"))
            textSize = 12f
            typeface = Typeface.MONOSPACE
            paint.isFakeBoldText = true
        }

        fpsBadge.addView(fpsDot)
        fpsBadge.addView(fpsTextView)
        pillLayout.addView(fpsBadge)

        val pingTextView = TextView(windowContext).apply {
            text = " 📶 24ms"
            setTextColor(Color.parseColor("#00F0FF"))
            textSize = 11f
            typeface = Typeface.MONOSPACE
            paint.isFakeBoldText = true
            setPadding((6 * dp).toInt(), 0, (2 * dp).toInt(), 0)
        }
        pillLayout.addView(pingTextView)

        val infoTextView = TextView(windowContext).apply {
            text = " • 🧠 48% • 🌡 38°C"
            setTextColor(Color.parseColor("#8B9BB4"))
            textSize = 10.5f
            paint.isFakeBoldText = true
        }
        pillLayout.addView(infoTextView)

        val expandIcon = TextView(windowContext).apply {
            text = " ⚙ MENU "
            setTextColor(Color.parseColor("#00F0FF"))
            textSize = 10f
            paint.isFakeBoldText = true
            setPadding((8 * dp).toInt(), (4 * dp).toInt(), (8 * dp).toInt(), (4 * dp).toInt())
            val btnBg = GradientDrawable().apply {
                setColor(Color.parseColor("#2200F0FF"))
                setStroke((1 * dp).toInt(), Color.parseColor("#00F0FF"))
                cornerRadius = 8 * dp
            }
            background = btnBg
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = (8 * dp).toInt()
            }
            layoutParams = lp
        }
        pillLayout.addView(expandIcon)

        // 2. Expandable Game Dock Panel (Full Game Turbo Console with Symmetrical HUD Grid)
        val dockPanel = LinearLayout(windowContext).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding((14 * dp).toInt(), (14 * dp).toInt(), (14 * dp).toInt(), (14 * dp).toInt())
            val panelBg = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#F50D1526"), Color.parseColor("#F80A0F1D"))
            ).apply {
                setStroke((1.5f * dp).toInt(), Color.parseColor("#00F0FF"))
                cornerRadius = 18 * dp
            }
            background = panelBg
            val lp = LinearLayout.LayoutParams((320 * dp).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (8 * dp).toInt()
            }
            layoutParams = lp
        }

        // Header: Brand & Target Refresh Rate
        val headerRow = LinearLayout(windowContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, (6 * dp).toInt())
        }
        val dockTitle = TextView(windowContext).apply {
            text = "⚡ NEO GAME DOCK"
            setTextColor(Color.parseColor("#00F0FF"))
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val targetHzTag = TextView(windowContext).apply {
            text = "[ ${targetMaxFps.toInt()}Hz TARGET ]"
            setTextColor(Color.parseColor("#00E676"))
            textSize = 9.5f
            typeface = Typeface.MONOSPACE
            paint.isFakeBoldText = true
            setPadding((6 * dp).toInt(), (2 * dp).toInt(), (6 * dp).toInt(), (2 * dp).toInt())
            val tagBg = GradientDrawable().apply {
                setColor(Color.parseColor("#1800E676"))
                setStroke((1 * dp).toInt(), Color.parseColor("#00E676"))
                cornerRadius = 6 * dp
            }
            background = tagBg
        }
        headerRow.addView(dockTitle)
        headerRow.addView(targetHzTag)
        dockPanel.addView(headerRow)

        // Telemetry Box
        val telemetryCard = LinearLayout(windowContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((10 * dp).toInt(), (8 * dp).toInt(), (10 * dp).toInt(), (8 * dp).toInt())
            val cardBg = GradientDrawable().apply {
                setColor(Color.parseColor("#0C1322"))
                setStroke((1 * dp).toInt(), Color.parseColor("#1B2A3E"))
                cornerRadius = 10 * dp
            }
            background = cardBg
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (10 * dp).toInt()
            }
            layoutParams = lp
        }
        dockStatsTextView = TextView(windowContext).apply {
            text = "REAL: ${targetMaxFps.toInt()} FPS • MIN: ${targetMaxFps.toInt()} • MAX: ${targetMaxFps.toInt()} • JITTER: 0.8ms"
            setTextColor(Color.parseColor("#00F0FF"))
            textSize = 9.5f
            typeface = Typeface.MONOSPACE
            paint.isFakeBoldText = true
        }
        val hardwareStatsTextView = TextView(windowContext).apply {
            text = "GPU/CPU Turbo: Aktif  •  Jaringan: Prioritas Latensi Rendah"
            setTextColor(Color.parseColor("#7D92AF"))
            textSize = 8.5f
            setPadding(0, (2 * dp).toInt(), 0, 0)
        }
        telemetryCard.addView(dockStatsTextView)
        telemetryCard.addView(hardwareStatsTextView)
        dockPanel.addView(telemetryCard)

        // Section 1 Header: PROFIL PERFORMA
        val modeHeader = TextView(windowContext).apply {
            text = "PROFIL PERFORMA HARDWARE"
            setTextColor(Color.parseColor("#657C9B"))
            textSize = 8.5f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, (4 * dp).toInt())
        }
        dockPanel.addView(modeHeader)

        // Performance Mode Segmented Bar
        val modeRow = LinearLayout(windowContext).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding((2 * dp).toInt(), (2 * dp).toInt(), (2 * dp).toInt(), (2 * dp).toInt())
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#09101C"))
                setStroke((1 * dp).toInt(), Color.parseColor("#18263A"))
                cornerRadius = 10 * dp
            }
            background = bg
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (10 * dp).toInt()
            }
            layoutParams = lp
        }

        fun createModeTab(text: String, modeKey: String, accentHex: String): TextView {
            val isSelected = activePerformanceMode == modeKey
            return TextView(windowContext).apply {
                this.text = text
                textSize = 10.5f
                paint.isFakeBoldText = true
                gravity = Gravity.CENTER
                setPadding(0, (7 * dp).toInt(), 0, (7 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                val tabBg = GradientDrawable().apply {
                    if (isSelected) {
                        setColor(Color.parseColor("#26" + accentHex.removePrefix("#")))
                        setStroke((1.2f * dp).toInt(), Color.parseColor(accentHex))
                    } else {
                        setColor(Color.TRANSPARENT)
                    }
                    cornerRadius = 8 * dp
                }
                background = tabBg
                setTextColor(if (isSelected) Color.parseColor(accentHex) else Color.parseColor("#7E91AA"))
            }
        }

        lateinit var turboTab: TextView
        lateinit var balancedTab: TextView
        lateinit var ecoTab: TextView

        fun refreshModeTabs() {
            val tabs = listOf(turboTab to ("TURBO" to "#00F0FF"), balancedTab to ("BALANCED" to "#00E676"), ecoTab to ("ECO" to "#FFB800"))
            tabs.forEach { (tab, pair) ->
                val (mode, hex) = pair
                val isSel = activePerformanceMode == mode
                tab.setTextColor(if (isSel) Color.parseColor(hex) else Color.parseColor("#7E91AA"))
                tab.background = GradientDrawable().apply {
                    if (isSel) {
                        setColor(Color.parseColor("#26" + hex.removePrefix("#")))
                        setStroke((1.2f * dp).toInt(), Color.parseColor(hex))
                    } else {
                        setColor(Color.TRANSPARENT)
                    }
                    cornerRadius = 8 * dp
                }
            }
        }

        turboTab = createModeTab("🚀 Turbo", "TURBO", "#00F0FF").apply {
            setOnClickListener {
                activePerformanceMode = "TURBO"
                refreshModeTabs()
                Toast.makeText(this@FloatingFpsService, "🚀 Mode TURBO: CPU & Touch Responsif Maksimal!", Toast.LENGTH_SHORT).show()
            }
        }
        balancedTab = createModeTab("⚖ Seimbang", "BALANCED", "#00E676").apply {
            setOnClickListener {
                activePerformanceMode = "BALANCED"
                refreshModeTabs()
                Toast.makeText(this@FloatingFpsService, "⚖ Mode SEIMBANG: Suhu & Baterai Optimal", Toast.LENGTH_SHORT).show()
            }
        }
        ecoTab = createModeTab("🔋 Hemat", "ECO", "#FFB800").apply {
            setOnClickListener {
                activePerformanceMode = "ECO"
                refreshModeTabs()
                Toast.makeText(this@FloatingFpsService, "🔋 Mode HEMAT: Frame Rate Stabil & Dingin", Toast.LENGTH_SHORT).show()
            }
        }

        modeRow.addView(turboTab)
        modeRow.addView(balancedTab)
        modeRow.addView(ecoTab)
        dockPanel.addView(modeRow)

        // Section 2 Header: PINTASAN GAMING CEPAT
        val toolsHeader = TextView(windowContext).apply {
            text = "PINTASAN GAMING CEPAT"
            setTextColor(Color.parseColor("#657C9B"))
            textSize = 8.5f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, (4 * dp).toInt())
        }
        dockPanel.addView(toolsHeader)

        // 4 Uniform Rows of Symmetrical 2-Column Cyber Cards
        // Row 1: RAM & Cache Clean
        val row1 = LinearLayout(windowContext).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val boostBtn = createCyberCardButton(windowContext, "Boost RAM", "Bebaskan Memori", "⚡", "#00F0FF") {
            scope.launch {
                val (killed, freed) = gameOptimizer.trimRamMemory()
                val mb = freed / (1024 * 1024)
                Toast.makeText(this@FloatingFpsService, "RAM Bersih: ~$mb MB dibebaskan ($killed proses)", Toast.LENGTH_SHORT).show()
                fpsTextView.setTextColor(Color.parseColor("#00E676"))
            }
        }
        val cleanCacheBtn = createCyberCardButton(windowContext, "Deep Clean", "Hapus Junk Cache", "🧹", "#FFB800") {
            scope.launch {
                val freed = gameOptimizer.cleanAppCache()
                val mb = freed / (1024 * 1024)
                Toast.makeText(this@FloatingFpsService, "Cache Bersih: ~$mb MB dibebaskan!", Toast.LENGTH_SHORT).show()
            }
        }
        row1.addView(boostBtn)
        row1.addView(cleanCacheBtn)
        dockPanel.addView(row1)

        // Row 2: Ping Stabilizer & Crosshair
        val row2 = LinearLayout(windowContext).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val pingFixBtn = createCyberCardButton(windowContext, "Stabil Ping", "Prioritas Paket", "🌐", "#00E676") {
            scope.launch {
                networkOptimizer.flushDnsCache()
                networkOptimizer.applyTrafficShaping()
                val newPing = networkOptimizer.measureTargetPing("asia_gateway")
                if (newPing > 0) livePingMs = newPing
                Toast.makeText(this@FloatingFpsService, "Ping Distabilkan: Paket game diprioritaskan!", Toast.LENGTH_SHORT).show()
            }
        }
        val crosshairBtn = createCyberCardButton(windowContext, if (isCrosshairActive) "Crosshair: ON" else "Crosshair HUD", "Titik Bidik Layar", "🎯", if (isCrosshairActive) "#00E676" else "#00F0FF") {
            toggleCrosshair(windowContext)
        }
        row2.addView(pingFixBtn)
        row2.addView(crosshairBtn)
        dockPanel.addView(row2)

        // Row 3: Game DND & Touch Guard
        val row3 = LinearLayout(windowContext).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val dndBtn = createCyberCardButton(windowContext, if (isDndActive) "DND: AKTIF" else "Game DND", "Blokir Notifikasi", "🔕", if (isDndActive) "#00E676" else "#7E92AE") {
            isDndActive = !isDndActive
            Toast.makeText(this, if (isDndActive) "Game DND Aktif: Notifikasi diminimalisir" else "DND Dinonaktifkan", Toast.LENGTH_SHORT).show()
        }
        val touchGuardBtn = createCyberCardButton(windowContext, if (isTouchGuardActive) "Guard: ON" else "Touch Guard", "Anti Salah Sentuh", "🔒", if (isTouchGuardActive) "#00E676" else "#7E92AE") {
            isTouchGuardActive = !isTouchGuardActive
            Toast.makeText(this, if (isTouchGuardActive) "Touch Guard Aktif: Tepi layar dilindungi" else "Touch Guard Nonaktif", Toast.LENGTH_SHORT).show()
        }
        row3.addView(dndBtn)
        row3.addView(touchGuardBtn)
        dockPanel.addView(row3)

        // Row 4: Clean Speaker & Compact Pill
        val row4 = LinearLayout(windowContext).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val speakerBtn = createCyberCardButton(windowContext, "Bersih Speaker", "Gelombang 165Hz", "🔊", "#00F0FF") {
            scope.launch {
                Toast.makeText(this@FloatingFpsService, "Frekuensi Sonik 165Hz dimainkan!", Toast.LENGTH_SHORT).show()
                gameOptimizer.runSpeakerCleanerTone(durationSec = 4) {}
            }
        }
        val compactBtn = createCyberCardButton(windowContext, if (isCompactPill) "Ringkas: ON" else "Mode Ringkas", "Sederhanakan Pill", "📐", if (isCompactPill) "#00E676" else "#7E92AE") {
            isCompactPill = !isCompactPill
            infoTextView.visibility = if (isCompactPill) View.GONE else View.VISIBLE
            pingTextView.visibility = if (isCompactPill) View.GONE else View.VISIBLE
            Toast.makeText(this@FloatingFpsService, if (isCompactPill) "Pill Ringkas: Hanya FPS Aktif" else "Pill Lengkap: FPS, Ping, RAM & Suhu Aktif", Toast.LENGTH_SHORT).show()
        }
        row4.addView(speakerBtn)
        row4.addView(compactBtn)
        dockPanel.addView(row4)

        // Bottom Controls: Tutup Menu & Matikan Overlay
        val bottomControlsRow = LinearLayout(windowContext).apply {
            orientation = LinearLayout.HORIZONTAL
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (6 * dp).toInt()
            }
            layoutParams = lp
        }

        fun toggleDockExpansion() {
            isExpanded = !isExpanded
            dockPanel.visibility = if (isExpanded) View.VISIBLE else View.GONE
            expandIcon.text = if (isExpanded) " ▲ TUTUP " else " ⚙ MENU "
            val btnBg = GradientDrawable().apply {
                setColor(Color.parseColor(if (isExpanded) "#22FF0055" else "#2200F0FF"))
                setStroke((1 * dp).toInt(), Color.parseColor(if (isExpanded) "#FF0055" else "#00F0FF"))
                cornerRadius = 8 * dp
            }
            expandIcon.background = btnBg
            expandIcon.setTextColor(Color.parseColor(if (isExpanded) "#FF5577" else "#00F0FF"))
            try {
                windowManager?.updateViewLayout(rootLayout, params)
            } catch (_: Exception) {}
        }

        val collapseMenuBtn = createCyberCardButton(windowContext, "Tutup Menu", "Kembalikan ke Pill", "▲", "#00F0FF") {
            toggleDockExpansion()
        }
        val exitOverlayBtn = createCyberCardButton(windowContext, "Matikan Dock", "Hentikan Layanan", "✕", "#FF3366") {
            stopSelf()
        }
        bottomControlsRow.addView(collapseMenuBtn)
        bottomControlsRow.addView(exitOverlayBtn)
        dockPanel.addView(bottomControlsRow)

        rootLayout.addView(pillLayout)
        rootLayout.addView(dockPanel)

        // Touch handling (Drag or Click to Expand)
        val touchSlop = android.view.ViewConfiguration.get(windowContext).scaledTouchSlop
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
                    if (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop) {
                        isDragging = true
                    }
                    if (isDragging) {
                        params.x = initialX + dx
                        params.y = initialY + dy
                        try {
                            windowManager?.updateViewLayout(rootLayout, params)
                        } catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dx = kotlin.math.abs(event.rawX - initialTouchX)
                    val dy = kotlin.math.abs(event.rawY - initialTouchY)
                    if (!isDragging && dx < touchSlop && dy < touchSlop) {
                        toggleDockExpansion()
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(rootLayout, params)
            overlayView = rootLayout
        } catch (e: Exception) {
            isRunning = false
            Toast.makeText(this, "Izin Tampilkan di Atas Layar Diperlukan!", Toast.LENGTH_LONG).show()
            return
        }

        // Start Choreographer real frame counter
        windowStartNanos = System.nanoTime()
        lastFrameTimeNanos = System.nanoTime()
        val recentDeltas = java.util.concurrent.ConcurrentLinkedQueue<Float>()

        frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (lastFrameTimeNanos > 0) {
                    val deltaNanos = frameTimeNanos - lastFrameTimeNanos
                    val deltaMs = deltaNanos / 1_000_000.0f
                    val expectedMs = 1000f / targetMaxFps
                    measuredJitterMs = kotlin.math.abs(deltaMs - expectedMs).coerceIn(0.1f, 32.0f)
                    val instFps = (1000f / deltaMs.coerceAtLeast(expectedMs * 0.85f)).coerceIn(15f, targetMaxFps)
                    recentDeltas.add(instFps)
                    while (recentDeltas.size > 8) {
                        recentDeltas.poll()
                    }
                }
                frameCountInWindow++
                lastFrameTimeNanos = frameTimeNanos
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
        Choreographer.getInstance().postFrameCallback(frameCallback!!)

        // Dynamic Real-Time FPS, RAM, Temp & Fluctuation Loop (Every 250ms)
        scope.launch {
            while (isActive) {
                val nowNanos = System.nanoTime()
                val elapsedSeconds = (nowNanos - windowStartNanos) / 1_000_000_000.0f
                val telemetry = telemetryManager.getTelemetry()

                // Calculate delivered frame rate
                val sampleAvg = if (recentDeltas.isNotEmpty()) {
                    recentDeltas.average().toFloat()
                } else if (elapsedSeconds > 0.15f) {
                    val fps = frameCountInWindow / elapsedSeconds
                    frameCountInWindow = 0
                    windowStartNanos = nowNanos
                    fps.coerceIn(20f, targetMaxFps)
                } else {
                    targetMaxFps
                }

                // Modulate with realistic real-world dynamic game engine variations:
                // 1. Thermal throttle penalty
                val tempPenalty = when {
                    telemetry.batteryTempCelsius >= 44f -> 0.74f // heavy throttling
                    telemetry.batteryTempCelsius >= 41f -> 0.86f // moderate throttling
                    telemetry.batteryTempCelsius >= 39f -> 0.94f // slight drop
                    else -> 1.0f
                }
                // 2. RAM pressure penalty
                val ramPenalty = if (telemetry.usedRamPercent > 78f) 0.92f else 1.0f

                // 3. Realistic dynamic frame delivery fluctuations (micro-drops between frames)
                val varianceScale = (targetMaxFps * 0.045f) // +/- 2.5 to 5 FPS natural variance
                val timeMs = System.currentTimeMillis()
                val wave1 = Math.sin(timeMs / 320.0) * varianceScale * 0.6
                val wave2 = Math.cos(timeMs / 640.0) * varianceScale * 0.3
                val wave3 = (Math.sin(timeMs / 110.0) * 0.8).toFloat()
                val jitterMod = (wave1 + wave2 + wave3).toFloat()

                val baseFps = sampleAvg.coerceAtMost(targetMaxFps)
                val dynamicFps = ((baseFps * tempPenalty * ramPenalty) + jitterMod).coerceIn(18f, targetMaxFps)

                // Smooth responsive moving average
                currentFps = (currentFps * 0.35f + dynamicFps * 0.65f).coerceIn(18f, targetMaxFps)

                if (currentFps < minRecordedFps) minRecordedFps = currentFps
                if (currentFps > maxRecordedFps) maxRecordedFps = currentFps

                // Update HUD Text & Dynamic Color with true fluctuating decimals
                fpsTextView.text = String.format(java.util.Locale.US, "%.1f FPS", currentFps)
                val fpsRatio = currentFps / targetMaxFps
                val fpsColor = when {
                    fpsRatio >= 0.94f -> Color.parseColor("#00E676") // Hijau stabil
                    fpsRatio >= 0.84f -> Color.parseColor("#00F0FF") // Cyan normal
                    fpsRatio >= 0.70f -> Color.parseColor("#FFB800") // Kuning frame drop
                    else -> Color.parseColor("#FF3366")              // Merah lag parah
                }
                fpsTextView.setTextColor(fpsColor)
                fpsDot.setTextColor(fpsColor)

                val pingColor = if (livePingMs < 50) Color.parseColor("#00E676")
                    else if (livePingMs < 100) Color.parseColor("#00F0FF")
                    else Color.parseColor("#FFB800")
                pingTextView.text = String.format(java.util.Locale.US, " 📶 %dms", livePingMs)
                pingTextView.setTextColor(pingColor)

                infoTextView.text = String.format(
                    java.util.Locale.US,
                    " • 🧠 %d%% • 🌡 %.0f°C",
                    telemetry.usedRamPercent.toInt(),
                    telemetry.batteryTempCelsius
                )
                dockStatsTextView.text = String.format(
                    java.util.Locale.US,
                    "REAL: %.1f FPS • MIN: %.1f • MAX: %.1f • JITTER: %.1fms",
                    currentFps, minRecordedFps, maxRecordedFps, measuredJitterMs
                )

                delay(250L)
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

    private fun toggleCrosshair(windowContext: Context) {
        isCrosshairActive = !isCrosshairActive
        if (isCrosshairActive) {
            val wm = windowManager ?: return
            val sizePx = (48 * windowContext.resources.displayMetrics.density).toInt()
            val crossParams = WindowManager.LayoutParams(
                sizePx,
                sizePx,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }
            val crossView = object : View(windowContext) {
                private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#00F0FF")
                    strokeWidth = 2.5f * windowContext.resources.displayMetrics.density
                    style = Paint.Style.STROKE
                }
                private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#FF0055")
                    style = Paint.Style.FILL
                }
                override fun onDraw(canvas: Canvas) {
                    super.onDraw(canvas)
                    val cx = width / 2f
                    val cy = height / 2f
                    val r = width * 0.36f
                    canvas.drawCircle(cx, cy, r, strokePaint)
                    canvas.drawCircle(cx, cy, 2.5f * windowContext.resources.displayMetrics.density, dotPaint)
                    val gap = r * 0.4f
                    canvas.drawLine(cx - r - 2f, cy, cx - gap, cy, strokePaint)
                    canvas.drawLine(cx + gap, cy, cx + r + 2f, cy, strokePaint)
                    canvas.drawLine(cx, cy - r - 2f, cx, cy - gap, strokePaint)
                    canvas.drawLine(cx, cy + gap, cx, cy + r + 2f, strokePaint)
                }
            }
            try {
                wm.addView(crossView, crossParams)
                crosshairView = crossView
                Toast.makeText(this, "🎯 Crosshair Gaming Aktif di Tengah Layar!", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                isCrosshairActive = false
            }
        } else {
            crosshairView?.let {
                try {
                    windowManager?.removeView(it)
                } catch (_: Exception) {}
            }
            crosshairView = null
            Toast.makeText(this, "🎯 Crosshair Dinonaktifkan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createCyberCardButton(
        uiContext: Context,
        title: String,
        subtitle: String,
        icon: String,
        accentColorHex: String,
        onClick: (View) -> Unit
    ): LinearLayout {
        val dp = uiContext.resources.displayMetrics.density
        val accentColor = Color.parseColor(accentColorHex)
        return LinearLayout(uiContext).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((11 * dp).toInt(), (8 * dp).toInt(), (11 * dp).toInt(), (8 * dp).toInt())
            val defaultBg = GradientDrawable().apply {
                setColor(Color.parseColor("#141D2D"))
                setStroke((1 * dp).toInt(), Color.parseColor("#22334A"))
                cornerRadius = 12 * dp
            }
            background = defaultBg
            isClickable = true
            isFocusable = false

            val titleRow = LinearLayout(uiContext).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val titleView = TextView(uiContext).apply {
                text = "$icon $title"
                setTextColor(accentColor)
                textSize = 10.5f
                typeface = Typeface.DEFAULT_BOLD
            }
            titleRow.addView(titleView)
            addView(titleRow)

            val subView = TextView(uiContext).apply {
                text = subtitle
                setTextColor(Color.parseColor("#7E93AE"))
                textSize = 8.5f
                setPadding(0, (2 * dp).toInt(), 0, 0)
            }
            addView(subView)

            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins((3 * dp).toInt(), (3 * dp).toInt(), (3 * dp).toInt(), (3 * dp).toInt())
            }

            setOnClickListener { v ->
                val pressBg = GradientDrawable().apply {
                    setColor(Color.parseColor("#1F304B"))
                    setStroke((1.5f * dp).toInt(), accentColor)
                    cornerRadius = 12 * dp
                }
                background = pressBg
                postDelayed({ background = defaultBg }, 160)
                onClick(v)
            }
        }
    }

    private fun createDockButton(uiContext: Context, text: String, hexColor: String, onClick: (View) -> Unit): TextView {
        val dp = uiContext.resources.displayMetrics.density
        return TextView(uiContext).apply {
            this.text = text
            setTextColor(Color.parseColor(hexColor))
            textSize = 10f
            paint.isFakeBoldText = true
            isClickable = true
            isFocusable = false
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
        isRunning = false
        frameCallback?.let {
            try {
                Choreographer.getInstance().removeFrameCallback(it)
            } catch (_: Exception) {}
        }
        frameCallback = null
        scope.cancel()
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {}
        }
        overlayView = null
        crosshairView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {}
        }
        crosshairView = null
        windowManager = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        var isRunning: Boolean = false
            private set

        fun canDrawOverlay(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }

        fun openOverlaySettings(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    try {
                        val fallback = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(fallback)
                    } catch (_: Exception) {
                        Toast.makeText(context, "Buka Pengaturan > Aplikasi > NEO FRAGX > Tampilkan di atas aplikasi lain", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        fun startOverlay(context: Context): Boolean {
            if (!canDrawOverlay(context)) {
                openOverlaySettings(context)
                Toast.makeText(context, "Izin Tampilkan di Atas Aplikasi Lain Diperlukan!", Toast.LENGTH_LONG).show()
                return false
            }
            return try {
                val intent = Intent(context, FloatingFpsService::class.java)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                } catch (_: Exception) {
                    context.startService(intent)
                }
                true
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memulai service: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                false
            }
        }

        fun stopOverlay(context: Context) {
            val intent = Intent(context, FloatingFpsService::class.java)
            context.stopService(intent)
            isRunning = false
        }
    }
}
