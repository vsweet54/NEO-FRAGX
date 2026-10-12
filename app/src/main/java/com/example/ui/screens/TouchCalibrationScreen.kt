package com.example.ui.screens

import android.view.Choreographer
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class TouchPoint(val id: Long, val x: Float, val y: Float, val pressure: Float)

@Composable
fun TouchCalibrationScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1. Refresh Rate Measurement
    var measuredHz by remember { mutableStateOf(120f) }
    var frameJitterMs by remember { mutableStateOf(0.8f) }

    LaunchedEffect(Unit) {
        var lastTime = 0L
        var frames = 0
        val cb = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (lastTime > 0) {
                    val deltaMs = (frameTimeNanos - lastTime) / 1_000_000.0f
                    if (deltaMs > 0) {
                        val hz = (1000f / deltaMs).coerceIn(30f, 240f)
                        measuredHz = measuredHz * 0.95f + hz * 0.05f
                        frameJitterMs = (frameJitterMs * 0.9f + Math.abs(deltaMs - (1000f / measuredHz)) * 0.1f).coerceIn(0.1f, 15f)
                    }
                }
                lastTime = frameTimeNanos
                frames++
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
        Choreographer.getInstance().postFrameCallback(cb)
    }

    // 2. Interactive Touch Response Test States
    var testState by remember { mutableStateOf("IDLE") } // IDLE, WAITING, READY, FINISHED
    var targetStartTime by remember { mutableStateOf(0L) }
    var responseLatencyMs by remember { mutableStateOf<Long?>(null) }
    var reactionRating by remember { mutableStateOf("") }
    var testAttempts by remember { mutableStateOf(0) }
    var bestLatencyMs by remember { mutableStateOf<Long?>(null) }

    fun startTouchTest() {
        testState = "WAITING"
        responseLatencyMs = null
        scope.launch {
            val randomDelay = Random.nextLong(1500, 3500)
            delay(randomDelay)
            if (testState == "WAITING") {
                testState = "READY"
                targetStartTime = System.currentTimeMillis()
            }
        }
    }

    // 3. Multi-Touch Visualizer State
    var activePointers by remember { mutableStateOf<List<TouchPoint>>(emptyList()) }
    var maxPointersSeen by remember { mutableStateOf(0) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Kalibrasi Layar & Sentuhan",
                subtitle = "Uji Latensi Touch-to-Response, Multi-Touch & Kecepatan Hz Layar",
                icon = Icons.Outlined.Speed
            )
        }

        // Card 1: Display Refresh Rate & Frame Pacing
        item {
            CyberCard(glow = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Refresh Rate Perangkat Aktual",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Pendeteksian sinkronisasi VSYNC hardware via Android Choreographer",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    StatusBadge(
                        text = if (measuredHz >= 115) "ULTRA 120Hz" else if (measuredHz >= 85) "SMOOTH 90Hz" else "STANDARD 60Hz",
                        color = if (measuredHz >= 115) NeonCyan else StatusSuccess
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0C1624))
                            .border(1.dp, CyberCardStroke, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Kecepatan Layar", fontSize = 10.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format("%.1f Hz", measuredHz),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0C1624))
                            .border(1.dp, CyberCardStroke, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Frame Pacing Jitter", fontSize = 10.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format("±%.2f ms", frameJitterMs),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (frameJitterMs < 2.0f) StatusSuccess else StatusWarning,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Card 2: Interactive Touch-to-Response Test
        item {
            CyberCard {
                SectionHeader(
                    title = "Uji Latensi Touch-to-Response",
                    subtitle = "Tekan target begitu berubah warna neon secepat mungkin",
                    icon = Icons.Default.TouchApp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Interactive target area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when (testState) {
                                "WAITING" -> Color(0xFF1F1508)
                                "READY" -> Color(0xFF082618)
                                "FINISHED" -> Color(0xFF0C1A2E)
                                else -> Color(0xFF0A0F17)
                            }
                        )
                        .border(
                            2.dp,
                            when (testState) {
                                "WAITING" -> StatusWarning
                                "READY" -> NeonCyan
                                "FINISHED" -> StatusSuccess
                                else -> CyberCardStroke
                            },
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            if (testState == "READY") {
                                val lat = System.currentTimeMillis() - targetStartTime
                                responseLatencyMs = lat
                                testAttempts++
                                bestLatencyMs = if (bestLatencyMs == null) lat else Math.min(bestLatencyMs!!, lat)
                                reactionRating = when {
                                    lat < 180 -> "⚡ ESPORTS PRO (< 180ms)"
                                    lat < 230 -> "🔥 SANGAT CEPAT (180-230ms)"
                                    lat < 300 -> "👍 RESPON NORMAL (230-300ms)"
                                    else -> "🐢 BUTUH KALIBRASI SENTUHAN (> 300ms)"
                                }
                                testState = "FINISHED"
                            } else if (testState == "WAITING") {
                                Toast.makeText(context, "Terlalu cepat! Tunggu hingga warna berubah neon hijau.", Toast.LENGTH_SHORT).show()
                                testState = "IDLE"
                            } else if (testState == "IDLE" || testState == "FINISHED") {
                                startTouchTest()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        when (testState) {
                            "IDLE" -> {
                                Icon(imageVector = Icons.Default.PlayCircle, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "Ketuk Di Sini untuk Mulai Uji Latensi", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(text = "Target akan berubah warna secara acak", fontSize = 11.sp, color = TextSecondary)
                            }
                            "WAITING" -> {
                                Text(text = "SIAP-SIAP...", fontSize = 18.sp, fontWeight = FontWeight.Black, color = StatusWarning)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Tunggu hingga tombol berubah HIJAU NEON!", fontSize = 11.sp, color = TextSecondary)
                            }
                            "READY" -> {
                                Text(text = "KETUK SEKARANG!", fontSize = 22.sp, fontWeight = FontWeight.Black, color = NeonCyan)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "⚡ Mengukur input latency...", fontSize = 11.sp, color = StatusSuccess)
                            }
                            "FINISHED" -> {
                                Text(
                                    text = "${responseLatencyMs} ms",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeonCyan,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(text = reactionRating, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusSuccess)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "Ketuk lagi untuk mencoba ulang", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                }

                if (bestLatencyMs != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Rekor Terbaik: ${bestLatencyMs} ms", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.SemiBold)
                        Text(text = "Percobaan: $testAttempts kali", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }

        // Card 3: Multi-Touch & Ghost-Touch Diagnostic Canvas
        item {
            CyberCard {
                SectionHeader(
                    title = "Diagnostik Multi-Touch & Ghost-Touch",
                    subtitle = "Letakkan beberapa jari di canvas untuk menguji multi-titik sentuh",
                    icon = Icons.Default.Fingerprint
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF090E17))
                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            forEachGesture {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val points = event.changes.filter { it.pressed }.map {
                                            TouchPoint(
                                                id = it.id.value,
                                                x = it.position.x,
                                                y = it.position.y,
                                                pressure = it.pressure
                                            )
                                        }
                                        activePointers = points
                                        if (points.size > maxPointersSeen) {
                                            maxPointersSeen = points.size
                                        }
                                        if (points.isEmpty()) break
                                    }
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        activePointers.forEachIndexed { idx, pt ->
                            val color = when (idx % 4) {
                                0 -> NeonCyan
                                1 -> Color(0xFF00E676)
                                2 -> Color(0xFFFFB800)
                                else -> Color(0xFFFF3366)
                            }
                            drawCircle(
                                color = color.copy(alpha = 0.25f),
                                radius = 48f,
                                center = Offset(pt.x, pt.y)
                            )
                            drawCircle(
                                color = color,
                                radius = 18f,
                                center = Offset(pt.x, pt.y),
                                style = Stroke(width = 3f)
                            )
                            drawCircle(
                                color = color,
                                radius = 6f,
                                center = Offset(pt.x, pt.y)
                            )
                        }
                    }

                    if (activePointers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Sentuh layar dengan 1-10 jari bersamaan",
                                fontSize = 12.sp,
                                color = TextSecondary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Titik Sentuh Aktif: ${activePointers.size} Jari",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activePointers.isNotEmpty()) NeonCyan else TextSecondary
                    )
                    Text(
                        text = "Maksimum Terdeteksi: $maxPointersSeen Jari",
                        fontSize = 12.sp,
                        color = StatusSuccess,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
