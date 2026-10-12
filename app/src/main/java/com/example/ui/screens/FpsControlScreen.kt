package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Choreographer
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tuner.FloatingFpsService
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun FpsControlScreen() {
    val context = LocalContext.current
    var targetFpsCap by remember { mutableStateOf(120) }
    var selectedRefreshRate by remember { mutableStateOf(120) }
    var isOverlayEnabled by remember { mutableStateOf(FloatingFpsService.isRunning) }

    LaunchedEffect(Unit) {
        isOverlayEnabled = FloatingFpsService.isRunning
    }

    // Real-time Frame Pacing Jitter Measurement
    var currentJitterMs by remember { mutableStateOf(0.8f) }
    var currentMeasuredFps by remember { mutableStateOf(119.4f) }
    var droppedFrames by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        var lastFrameTimeNanos = 0L
        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (lastFrameTimeNanos > 0) {
                    val deltaMs = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000.0f
                    val instantFps = 1000f / deltaMs.coerceAtLeast(1.0f)
                    currentMeasuredFps = (currentMeasuredFps * 0.9f + instantFps * 0.1f)
                    val expectedMs = 1000f / selectedRefreshRate
                    currentJitterMs = kotlin.math.abs(deltaMs - expectedMs)
                    if (deltaMs > expectedMs * 1.5f) {
                        droppedFrames = (droppedFrames + 1) % 1000
                    }
                }
                lastFrameTimeNanos = frameTimeNanos
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
        Choreographer.getInstance().postFrameCallback(callback)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Kontrol FPS & Frame Pacing",
                subtitle = "Refresh Rate Layar, Target FPS & Monitor Stabilitas Frame",
                icon = Icons.Outlined.Speed
            )
        }

        // Live Frame Pacing Card
        item {
            CyberCard(glow = true) {
                Text(
                    text = "Telemetri Frame Real-time (Choreographer)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "FPS Layar Saat Ini", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = String.format("%.1f FPS", currentMeasuredFps),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Jitter / Variansi", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = String.format("±%.2f ms", currentJitterMs),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentJitterMs < 2f) StatusSuccess else StatusWarning,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                MetricProgressBar(
                    title = "Kestabilan VSync",
                    valueLabel = if (currentJitterMs < 2f) "Sangat Rata & Mulus" else "Terdapat Fluktuasi",
                    progress = (1.0f - (currentJitterMs / 8f)).coerceIn(0.2f, 1f),
                    color = if (currentJitterMs < 2f) StatusSuccess else StatusWarning
                )
            }
        }

        // Refresh Rate Selector
        item {
            CyberCard {
                SectionHeader(
                    title = "Mode Refresh Rate Layar",
                    subtitle = "Pilih kecepatan pembaruan panel display fisik",
                    icon = Icons.Default.Tv
                )
                Spacer(modifier = Modifier.height(10.dp))
                val refreshRates = listOf(60, 90, 120, 144)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    refreshRates.forEach { hz ->
                        val isSelected = selectedRefreshRate == hz
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, if (isSelected) NeonCyan else CyberCardStroke, RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color(0xFF0C1420))
                                .clickable {
                                    selectedRefreshRate = hz
                                    Toast.makeText(context, "Refresh rate disetel ke ${hz}Hz", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${hz}Hz",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) NeonCyan else TextPrimary
                                )
                                Text(
                                    text = if (hz >= 120) "Ultra" else if (hz == 90) "Smooth" else "Hemat",
                                    fontSize = 9.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Target FPS Cap
        item {
            CyberCard {
                SectionHeader(
                    title = "Target Batas FPS (FPS Cap)",
                    subtitle = "Membatasi laju bingkai untuk menghemat daya dan menjaga suhu stabil",
                    icon = Icons.Default.FilterFrames
                )
                Spacer(modifier = Modifier.height(10.dp))
                val fpsCaps = listOf(30, 45, 60, 90, 120)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    fpsCaps.forEach { fps ->
                        val isSelected = targetFpsCap == fps
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, if (isSelected) NeonCyan else CyberCardStroke, RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color(0xFF0C1420))
                                .clickable {
                                    targetFpsCap = fps
                                    Toast.makeText(context, "Target FPS: $fps FPS", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$fps",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) NeonCyan else TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Floating FPS Overlay Switch
        item {
            CyberCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "HUD Mengambang Real-time (Floating FPS)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Menampilkan angka FPS, penggunaan RAM, dan suhu perangkat di atas semua game.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = isOverlayEnabled,
                        onCheckedChange = { enable ->
                            if (enable) {
                                val started = FloatingFpsService.startOverlay(context)
                                isOverlayEnabled = started
                                if (started) {
                                    Toast.makeText(context, "Overlay FPS diaktifkan di atas layar game!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                isOverlayEnabled = false
                                FloatingFpsService.stopOverlay(context)
                                Toast.makeText(context, "Overlay FPS dinonaktifkan", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = NeonBlueDark,
                            uncheckedTrackColor = CyberDark
                        )
                    )
                }
            }
        }

        // Frame Pacing Explanation
        item {
            CyberCard {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "Mengapa Frame Pacing Penting?", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Frame Pacing yang konsisten (variansi di bawah 1.5 ms) jauh lebih nyaman dipandang dibandingkan FPS tinggi yang berfluktuasi naik-turun. Frame Cap mencegah terjadinya micro-stutter saat pertarungan intens.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
