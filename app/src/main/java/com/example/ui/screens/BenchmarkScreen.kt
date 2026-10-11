package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.BenchmarkResult
import com.example.data.model.SystemTelemetry
import com.example.tuner.BenchmarkEngine
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun BenchmarkScreen(
    telemetry: SystemTelemetry,
    benchmarkEngine: BenchmarkEngine
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRunningTest by remember { mutableStateOf(false) }
    var currentRunningName by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<BenchmarkResult>>(emptyList()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Tolok Ukur & Diagnostik (Benchmark)",
                subtitle = "Uji Komputasi Nyata CPU, RAM Throughput & Simulasi Render",
                icon = Icons.Outlined.Assessment
            )
        }

        // Active Test Status Box
        if (isRunningTest) {
            item {
                CyberCard(glow = true, glowColor = StatusWarning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = StatusWarning,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Sedang Menjalankan $currentRunningName...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Mengukur performa beban nyata hardware...",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // 3 Real Test Triggers
        item {
            CyberCard {
                Text(
                    text = "Pilih Pengujian Benchmark Nyata",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Test 1: Rendering
                BenchmarkTriggerRow(
                    title = "Simulasi Rendering & Frame Pacing",
                    desc = "Uji simulasi 2000 partikel fisika matriks & stabilitas 1% low FPS",
                    buttonText = "Mulai Render Test",
                    enabled = !isRunningTest,
                    onClick = {
                        isRunningTest = true
                        currentRunningName = "Render Test"
                        scope.launch {
                            val res = benchmarkEngine.runMicroRenderBenchmark(durationSec = 4)
                            results = listOf(res) + results
                            isRunningTest = false
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Test 2: CPU Multi-Thread
                BenchmarkTriggerRow(
                    title = "CPU Multi-Thread Stress Test",
                    desc = "Menghitung operasi matematika mengambang paralel di seluruh ${telemetry.cpuCores} Core",
                    buttonText = "Mulai CPU Test",
                    enabled = !isRunningTest,
                    onClick = {
                        isRunningTest = true
                        currentRunningName = "CPU Stress Test"
                        scope.launch {
                            val res = benchmarkEngine.runCpuMultiThreadTest(durationSec = 3)
                            results = listOf(res) + results
                            isRunningTest = false
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Test 3: RAM Bandwidth
                BenchmarkTriggerRow(
                    title = "RAM Bandwidth & Memory Throughput",
                    desc = "Mengukur kecepatan transfer buffer pembacaan/penulisan memori dalam MB/s",
                    buttonText = "Mulai RAM Test",
                    enabled = !isRunningTest,
                    onClick = {
                        isRunningTest = true
                        currentRunningName = "RAM Bandwidth Test"
                        scope.launch {
                            val res = benchmarkEngine.runRamBandwidthTest(durationSec = 2)
                            results = listOf(res) + results
                            isRunningTest = false
                        }
                    }
                )
            }
        }

        // Results Section
        if (results.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hasil Riwayat Pengujian",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${results.size} Pengujian Selesai",
                        fontSize = 11.sp,
                        color = NeonCyan
                    )
                }
            }

            items(results) { res ->
                BenchmarkResultCard(result = res)
            }
        }

        // Export Diagnostic Report
        item {
            CyberCard {
                SectionHeader(
                    title = "Laporan Diagnostik Sistem Lengkap",
                    subtitle = "Ekspor data hardware, baterai, thermal, dan benchmark ke Clipboard",
                    icon = Icons.Default.Description
                )
                Spacer(modifier = Modifier.height(10.dp))
                CyberButton(
                    text = "Salin Laporan ke Papan Klip",
                    icon = Icons.Default.ContentCopy,
                    onClick = {
                        val report = StringBuilder()
                            .append("=== LAPORAN DIAGNOSTIK NEO FRAGX ===\n")
                            .append("Perangkat: ${telemetry.deviceModel}\n")
                            .append("SoC: ${telemetry.socModel}\n")
                            .append("OS: ${telemetry.androidVersion}\n")
                            .append("RAM: Digunakan ${(telemetry.totalRamBytes - telemetry.availableRamBytes) / (1024*1024)} MB / Total ${telemetry.totalRamBytes / (1024*1024)} MB (${telemetry.usedRamPercent.toInt()}%)\n")
                            .append("Baterai: ${telemetry.batteryLevel}% (${telemetry.batteryTempCelsius}°C, ${telemetry.batteryVoltageMv} mV)\n")
                            .append("Thermal Status: ${telemetry.thermalStatus}\n")
                            .append("CPU Cores: ${telemetry.cpuCores} (Governor: ${telemetry.cpuGovernor})\n")
                            .append("Waktu Aktif: ${String.format("%.1f", telemetry.uptimeHours)} Jam\n")
                            .append("====================================\n")
                            .toString()

                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("NEO FRAGX Diagnostic", report)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Laporan berhasil disalin ke Papan Klip!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun BenchmarkTriggerRow(
    title: String,
    desc: String,
    buttonText: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0B121C))
            .border(1.dp, CyberCardStroke, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = desc, fontSize = 11.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(10.dp))
            CyberButton(
                text = buttonText,
                enabled = enabled,
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BenchmarkResultCard(result: BenchmarkResult) {
    CyberCard(glow = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = result.testName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = result.scoreValue,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = result.scoreUnit,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
            StatusBadge(text = result.scoreRating, color = StatusSuccess)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = result.metric1Label, fontSize = 10.sp, color = TextSecondary)
                Text(text = result.metric1Value, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = result.metric2Label, fontSize = 10.sp, color = TextSecondary)
                Text(text = result.metric2Value, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
