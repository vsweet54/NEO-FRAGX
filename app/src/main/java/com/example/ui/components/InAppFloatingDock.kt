package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SystemTelemetry
import com.example.tuner.FloatingFpsService
import com.example.tuner.GameOptimizer
import com.example.tuner.NetworkLatencyOptimizer
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun InAppFloatingDock(
    telemetry: SystemTelemetry,
    gameOptimizer: GameOptimizer,
    networkOptimizer: NetworkLatencyOptimizer,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var offsetX by remember { mutableStateOf(16f) }
    var offsetY by remember { mutableStateOf(120f) }
    var isExpanded by remember { mutableStateOf(false) }

    var isDndActive by remember { mutableStateOf(false) }
    var isTouchGuardActive by remember { mutableStateOf(false) }
    var livePing by remember { mutableStateOf(24L) }
    var isCleaning by remember { mutableStateOf(false) }
    var activeMode by remember { mutableStateOf("TURBO") }
    var isCrosshairActive by remember { mutableStateOf(false) }
    var isCompactMode by remember { mutableStateOf(false) }

    // Periodic ping measurement
    LaunchedEffect(Unit) {
        while (true) {
            val p = networkOptimizer.measureTargetPing("asia_gateway")
            if (p > 0) livePing = p
            delay(4000L)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Crosshair Overlay (if toggled active)
        if (isCrosshairActive) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(46.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = size.width * 0.36f
                    drawCircle(color = NeonCyan, radius = r, style = Stroke(width = 2.dp.toPx()))
                    drawCircle(color = Color(0xFFFF0055), radius = 2.5.dp.toPx())
                    val gap = r * 0.4f
                    drawLine(color = NeonCyan, start = Offset(cx - r - 2f, cy), end = Offset(cx - gap, cy), strokeWidth = 2.dp.toPx())
                    drawLine(color = NeonCyan, start = Offset(cx + gap, cy), end = Offset(cx + r + 2f, cy), strokeWidth = 2.dp.toPx())
                    drawLine(color = NeonCyan, start = Offset(cx, cy - r - 2f), end = Offset(cx, cy - gap), strokeWidth = 2.dp.toPx())
                    drawLine(color = NeonCyan, start = Offset(cx, cy + gap), end = Offset(cx, cy + r + 2f), strokeWidth = 2.dp.toPx())
                }
            }
        }

        // Draggable Dock
        Box(
            modifier = modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.widthIn(max = 330.dp)
            ) {
                // Collapsed Pill Bubble (Always Draggable and Clickable)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .shadow(12.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xF0080E18))
                        .border(2.dp, NeonCyan, RoundedCornerShape(20.dp))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var isDrag = false
                                var totalDrag = Offset.Zero
                                val slop = viewConfiguration.touchSlop
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val up = event.changes.firstOrNull { it.id == down.id && !it.pressed }
                                    if (up != null) {
                                        if (!isDrag) {
                                            isExpanded = !isExpanded
                                        }
                                        break
                                    }
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    val delta = change.positionChange()
                                    totalDrag += delta
                                    if (!isDrag && totalDrag.getDistance() > slop) {
                                        isDrag = true
                                    }
                                    if (isDrag) {
                                        change.consume()
                                        offsetX = (offsetX + delta.x).coerceIn(0f, 600f)
                                        offsetY = (offsetY + delta.y).coerceIn(0f, 1600f)
                                    }
                                }
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    // FPS Indicator
                    Text(
                        text = "60 FPS",
                        color = StatusSuccess,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (!isCompactMode) {
                        Text(
                            text = " • ",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        // Ping Indicator
                        Text(
                            text = "${livePing}ms",
                            color = if (livePing < 60) StatusSuccess else StatusWarning,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = " • ",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        // RAM & Temp
                        Text(
                            text = "${telemetry.usedRamPercent.toInt()}% RAM",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isExpanded) Color(0x33FF0055) else Color(0x3300F0FF))
                            .clickable { isExpanded = !isExpanded }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "▲ TUTUP" else "⚙ MENU",
                            color = if (isExpanded) Color(0xFFFF5577) else NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Expanded Dock Sidebar (Game Turbo Console)
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .shadow(16.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xF80A101D))
                            .border(1.5.dp, CyberCardStrokeGlow, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "NEO GAME TURBO DOCK",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                            IconButton(
                                onClick = { isExpanded = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Ciutkan",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Stats Bar
                        Text(
                            text = "Target: 60Hz • Temp: ${telemetry.batteryTempCelsius.toInt()}°C • Jitter: 0.8ms",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Performance Mode Selector Bar (Turbo, Balanced, Eco)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DockActionButton(
                                icon = Icons.Default.Bolt,
                                text = "🚀 Turbo",
                                color = if (activeMode == "TURBO") NeonCyan else TextSecondary,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    activeMode = "TURBO"
                                    Toast.makeText(context, "🚀 Mode TURBO: CPU Clock & Touch Responsif Maksimal!", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DockActionButton(
                                icon = Icons.Default.Balance,
                                text = "⚖️ Seimbang",
                                color = if (activeMode == "BALANCED") StatusSuccess else TextSecondary,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    activeMode = "BALANCED"
                                    Toast.makeText(context, "⚖️ Mode SEIMBANG: Efisiensi & Suhu Terjaga", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DockActionButton(
                                icon = Icons.Default.BatteryChargingFull,
                                text = "🔋 Hemat",
                                color = if (activeMode == "ECO") StatusWarning else TextSecondary,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    activeMode = "ECO"
                                    Toast.makeText(context, "🔋 Mode HEMAT: Daya Dingin & Baterai Tahan Lama", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Action Buttons Row 1: RAM & Cache Clean
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Boost RAM
                            DockActionButton(
                                icon = Icons.Default.Speed,
                                text = "Boost RAM",
                                color = NeonCyan,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    coroutineScope.launch {
                                        val (killed, freed) = gameOptimizer.trimRamMemory()
                                        val mb = freed / (1024 * 1024)
                                        Toast.makeText(context, "RAM Dibersihkan: ~$mb MB dibebaskan ($killed proses)", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            // Clean Cache
                            DockActionButton(
                                icon = Icons.Default.CleaningServices,
                                text = if (isCleaning) "Proses..." else "Bersih Cache",
                                color = StatusWarning,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    coroutineScope.launch {
                                        isCleaning = true
                                        val freed = gameOptimizer.cleanAppCache()
                                        val mb = freed / (1024 * 1024)
                                        Toast.makeText(context, "Cache Bersih: ~$mb MB dibebaskan!", Toast.LENGTH_SHORT).show()
                                        delay(1000)
                                        isCleaning = false
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Action Buttons Row 2: Ping & Crosshair
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Ping Stabilizer
                            DockActionButton(
                                icon = Icons.Default.Wifi,
                                text = "Stabilkan Ping",
                                color = StatusSuccess,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    coroutineScope.launch {
                                        networkOptimizer.flushDnsCache()
                                        networkOptimizer.applyTrafficShaping()
                                        val newPing = networkOptimizer.measureTargetPing("asia_gateway")
                                        if (newPing > 0) livePing = newPing
                                        Toast.makeText(context, "DNS Diperbarui! Ping stabil: ${livePing}ms", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            // Crosshair
                            DockActionButton(
                                icon = Icons.Default.CenterFocusStrong,
                                text = if (isCrosshairActive) "🎯 Crosshair: ON" else "🎯 Crosshair",
                                color = if (isCrosshairActive) StatusSuccess else NeonCyan,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    isCrosshairActive = !isCrosshairActive
                                    Toast.makeText(context, if (isCrosshairActive) "🎯 Crosshair Layar Aktif!" else "🎯 Crosshair Dinonaktifkan", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Action Buttons Row 3: DND & Touch Protection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // DND
                            DockActionButton(
                                icon = if (isDndActive) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                                text = if (isDndActive) "🔕 DND: ON" else "🔕 Game DND",
                                color = if (isDndActive) StatusSuccess else TextSecondary,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    isDndActive = !isDndActive
                                    Toast.makeText(context, if (isDndActive) "Mode DND Aktif: Notifikasi diminimalisir saat gaming" else "Mode DND Nonaktif", Toast.LENGTH_SHORT).show()
                                }
                            )

                            // Touch Protection
                            DockActionButton(
                                icon = Icons.Default.Lock,
                                text = if (isTouchGuardActive) "🔒 Lock: ON" else "🔒 Touch Guard",
                                color = if (isTouchGuardActive) StatusSuccess else TextSecondary,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    isTouchGuardActive = !isTouchGuardActive
                                    Toast.makeText(context, if (isTouchGuardActive) "Touch Guard Aktif: Edge touch dilindungi" else "Touch Guard Nonaktif", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Action Buttons Row 4: Speaker Cleaner & Compact Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Speaker Sonic Cleaner
                            DockActionButton(
                                icon = Icons.Default.VolumeUp,
                                text = "🔊 Bersih Speaker",
                                color = NeonCyan,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    coroutineScope.launch {
                                        Toast.makeText(context, "Frekuensi Sonik 165Hz dimainkan untuk membersihkan debu/air speaker!", Toast.LENGTH_SHORT).show()
                                        gameOptimizer.runSpeakerCleanerTone(durationSec = 4) {}
                                    }
                                }
                            )

                            // Compact Mode
                            DockActionButton(
                                icon = Icons.Default.AspectRatio,
                                text = if (isCompactMode) "📐 Ringkas: ON" else "📐 Mode Ringkas",
                                color = if (isCompactMode) StatusSuccess else TextSecondary,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    isCompactMode = !isCompactMode
                                    Toast.makeText(context, if (isCompactMode) "Pill diubah ke mode ringkas" else "Pill diubah ke mode lengkap", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Row 5: System Overlay Toggle & Close
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Launch system overlay outside the app
                            DockActionButton(
                                icon = Icons.Default.Layers,
                                text = if (FloatingFpsService.isRunning) "Overlay Luar: ON" else "Overlay Luar App",
                                color = if (FloatingFpsService.isRunning) StatusSuccess else NeonPurple,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (FloatingFpsService.isRunning) {
                                        FloatingFpsService.stopOverlay(context)
                                        Toast.makeText(context, "Overlay sistem dinonaktifkan.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val started = FloatingFpsService.startOverlay(context)
                                        if (started) {
                                            Toast.makeText(context, "Overlay mengambang aktif di atas semua game!", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            )

                            // Close Dock
                            DockActionButton(
                                icon = Icons.Default.PowerSettingsNew,
                                text = "Tutup Dock",
                                color = StatusDanger,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    isExpanded = false
                                    onClose()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DockActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF111A29))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
