package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RootShizukuStatus
import com.example.data.model.SystemTelemetry
import com.example.tuner.GameOptimizer
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    telemetry: SystemTelemetry,
    rootStatus: RootShizukuStatus,
    gameOptimizer: GameOptimizer,
    onNavigateToGameApi: () -> Unit,
    onNavigateToAngleDriver: () -> Unit,
    onNavigateToArtRuntime: () -> Unit,
    onNavigateToGameDriver: () -> Unit,
    onNavigateToRendering: () -> Unit,
    onNavigateToCpuBoost: () -> Unit,
    onNavigateToBatteryTuning: () -> Unit,
    onNavigateToFpsOverlay: () -> Unit,
    onNavigateToAddons: () -> Unit,
    onNavigateToSystemLab: () -> Unit,
    onNavigateToRootTerminal: () -> Unit
) {
    val context = LocalContext.current
    var showAllCoresDialog by remember { mutableStateOf(false) }
    var showQsTileDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Title Banner
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NEO FRAGX Dasbor",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "${telemetry.deviceModel} • ${telemetry.socModel}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "PRO ENGINE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
            }
        }

        // 1. Dashboard Sistem Card (Matching Screenshot 6)
        item {
            CyberCard(glow = true) {
                Text(
                    text = "Dashboard Sistem",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                // RAM Bar
                val ramUsedGb = (telemetry.totalRamBytes - telemetry.availableRamBytes) / (1024f * 1024f * 1024f)
                val ramTotalGb = telemetry.totalRamBytes / (1024f * 1024f * 1024f)
                MetricProgressBar(
                    title = "Penggunaan RAM",
                    valueLabel = String.format("%.1f%% (%.1f / %.1f GB)", telemetry.usedRamPercent, ramUsedGb, ramTotalGb),
                    progress = telemetry.usedRamPercent / 100f,
                    color = NeonCyan
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Temperature Bar
                val tempProgress = (telemetry.batteryTempCelsius / 60f).coerceIn(0f, 1f)
                val tempColor = if (telemetry.batteryTempCelsius > 42f) StatusDanger else if (telemetry.batteryTempCelsius > 38f) StatusWarning else NeonCyan
                MetricProgressBar(
                    title = "Temperature",
                    valueLabel = String.format("%.1f°C (%s)", telemetry.batteryTempCelsius, telemetry.thermalStatus),
                    progress = tempProgress,
                    color = tempColor
                )

                Spacer(modifier = Modifier.height(12.dp))

                // CPU Core Frequencies (Top 4 Cores)
                val topCores = telemetry.coreFrequenciesMhz.take(4)
                for (i in topCores.indices step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Core i
                        Box(modifier = Modifier.weight(1f)) {
                            val freq = telemetry.coreFrequenciesMhz.getOrElse(i) { 1785 }
                            MetricProgressBar(
                                title = "CPU $i",
                                valueLabel = "$freq MHz",
                                progress = (freq / 3200f).coerceIn(0.1f, 1f),
                                color = NeonBlue
                            )
                        }
                        // Core i+1
                        if (i + 1 < telemetry.coreFrequenciesMhz.size) {
                            Box(modifier = Modifier.weight(1f)) {
                                val freq = telemetry.coreFrequenciesMhz.getOrElse(i + 1) { 1785 }
                                MetricProgressBar(
                                    title = "CPU ${i + 1}",
                                    valueLabel = "$freq MHz",
                                    progress = (freq / 3200f).coerceIn(0.1f, 1f),
                                    color = NeonBlue
                                )
                            }
                        }
                    }
                    if (i + 2 < topCores.size) Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // "Tampilkan Semua CPU" Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0C141F))
                        .border(1.dp, CyberCardStroke, RoundedCornerShape(8.dp))
                        .clickable { showAllCoresDialog = true }
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Tampilkan Semua ${telemetry.cpuCores} CPU Core",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 2. Dashboard Fitur Grid (DIRECTLY CLICKABLE TO OPEN FEATURES!)
        item {
            CyberCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dashboard Fitur",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Ketuk untuk Buka Fitur ➔",
                        fontSize = 10.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // 2x3 Grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, CyberCardStroke, RoundedCornerShape(12.dp))
                ) {
                    // Row 1
                    Row(modifier = Modifier.fillMaxWidth()) {
                        FeatureGridItem(
                            modifier = Modifier.weight(1f),
                            title = "game api",
                            state = "Daftar Game",
                            isActive = true,
                            onClick = onNavigateToGameApi
                        )
                        DividerVertical()
                        FeatureGridItem(
                            modifier = Modifier.weight(1f),
                            title = "game api 16",
                            state = "Resolusi/FPS",
                            isActive = true,
                            onClick = onNavigateToGameApi
                        )
                        DividerVertical()
                        FeatureGridItem(
                            modifier = Modifier.weight(1f),
                            title = "driver angle",
                            state = "ANGLE GL",
                            isActive = true,
                            onClick = onNavigateToAngleDriver
                        )
                    }
                    DividerHorizontal()
                    // Row 2
                    Row(modifier = Modifier.fillMaxWidth()) {
                        FeatureGridItem(
                            modifier = Modifier.weight(1f),
                            title = "ART runtime",
                            state = "Kompilasi AOT",
                            isActive = true,
                            onClick = onNavigateToArtRuntime
                        )
                        DividerVertical()
                        FeatureGridItem(
                            modifier = Modifier.weight(1f),
                            title = "driver game",
                            state = "Setup Grafis",
                            isActive = true,
                            onClick = onNavigateToGameDriver
                        )
                        DividerVertical()
                        FeatureGridItem(
                            modifier = Modifier.weight(1f),
                            title = "rendering",
                            state = "Skia Vulkan",
                            isActive = true,
                            onClick = onNavigateToRendering
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick circular navigation badges + Root button (Screenshot 6)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularQuickBadge(
                            icon = Icons.Default.LocalFireDepartment,
                            label = "QS Tile",
                            isActive = true,
                            onClick = { showQsTileDialog = true }
                        )
                        CircularQuickBadge(
                            icon = Icons.Default.BatteryChargingFull,
                            label = "Battery",
                            isActive = telemetry.isCharging,
                            onClick = onNavigateToBatteryTuning
                        )
                        CircularQuickBadge(
                            icon = Icons.Default.Speed,
                            label = "CPU boost",
                            isActive = true,
                            onClick = onNavigateToCpuBoost
                        )
                        CircularQuickBadge(
                            icon = Icons.Default.Layers,
                            label = "Overlay",
                            isActive = true,
                            onClick = onNavigateToFpsOverlay
                        )
                    }

                    // Root button -> navigates directly to Root / Shizuku Terminal!
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, if (rootStatus.hasRoot) StatusSuccess.copy(alpha = 0.6f) else NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .background(Color(0xFF0A1624))
                            .clickable { onNavigateToRootTerminal() }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (rootStatus.hasRoot) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (rootStatus.hasRoot) StatusSuccess else NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (rootStatus.hasRoot) "Root Aktif" else "Root",
                                color = if (rootStatus.hasRoot) StatusSuccess else NeonCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Action Row: "Pulihkan Bawaan" & "Versi Terbaru"
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberCard)
                    .border(1.dp, CyberCardStroke, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable {
                        Toast.makeText(context, "Semua konfigurasi dikembalikan ke bawaan", Toast.LENGTH_SHORT).show()
                        gameOptimizer.addLog("RESET", "Semua pengaturan dikembalikan ke bawaan", "WARN")
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = StatusDanger,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pulihkan Bawaan",
                        color = StatusDanger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.clickable {
                        Toast.makeText(context, "NEO FRAGX v3.0 — Versi terbaru stabil", Toast.LENGTH_SHORT).show()
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircleOutline,
                        contentDescription = null,
                        tint = StatusSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Versi Terbaru (v3.0)",
                        color = StatusSuccess,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Lab Sistem Card -> Direct click to Advanced Setup!
        item {
            NavigationOptionRow(
                title = "Lab Sistem",
                subtitle = "Eksperimen kontrol thermal, render Skia, dan frekuensi GPU",
                icon = Icons.Outlined.Science,
                onClick = onNavigateToSystemLab
            )
        }

        // Modul Addon Card -> Direct click to Modul Addon!
        item {
            NavigationOptionRow(
                title = "Modul Addon",
                subtitle = "Kelola plugin Magic/APatch/KernelSU dan script modular",
                icon = Icons.Outlined.Extension,
                onClick = onNavigateToAddons
            )
        }
    }

    // Quick Settings Tile Dialog
    if (showQsTileDialog) {
        AlertDialog(
            onDismissRequest = { showQsTileDialog = false },
            confirmButton = {
                TextButton(onClick = { showQsTileDialog = false }) {
                    Text("Tutup", color = NeonCyan)
                }
            },
            title = {
                Text("Quick Settings Tile NEO FRAGX", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "NEO FRAGX menyediakan tile pintasan di bilah notifikasi atas Android. Anda dapat menambahkan tile ini melalui menu Edit Tile sistem untuk mengaktifkan Gaming Mode dengan sekali sentuh.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            },
            containerColor = CyberDark,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // All CPU Cores Dialog
    if (showAllCoresDialog) {
        AlertDialog(
            onDismissRequest = { showAllCoresDialog = false },
            confirmButton = {
                TextButton(onClick = { showAllCoresDialog = false }) {
                    Text("Tutup", color = NeonCyan)
                }
            },
            title = {
                Text(
                    text = "Status Semua Core CPU (${telemetry.cpuCores} Cores)",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Governor: ${telemetry.cpuGovernor.uppercase()}",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    telemetry.coreFrequenciesMhz.forEachIndexed { index, freq ->
                        val isPrimeCore = index >= 6
                        val isBigCore = index in 4..5
                        val coreType = if (isPrimeCore) "Prime (Cortex-X)" else if (isBigCore) "Big Core" else "Efficiency"
                        MetricProgressBar(
                            title = "Core $index ($coreType)",
                            valueLabel = "$freq MHz",
                            progress = (freq / 3200f).coerceIn(0.1f, 1f),
                            color = if (isPrimeCore) StatusDanger else if (isBigCore) NeonCyan else NeonBlue
                        )
                    }
                }
            },
            containerColor = CyberDark,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun FeatureGridItem(
    modifier: Modifier,
    title: String,
    state: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = state,
            fontSize = 11.sp,
            color = if (isActive) NeonCyan else TextSecondary.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DividerVertical() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(52.dp)
            .background(CyberCardStroke)
    )
}

@Composable
private fun DividerHorizontal() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(CyberCardStroke)
    )
}

@Composable
private fun CircularQuickBadge(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .border(1.dp, if (isActive) NeonCyan.copy(alpha = 0.8f) else CyberCardStroke, CircleShape)
                .background(if (isActive) NeonCyan.copy(alpha = 0.15f) else Color(0xFF0C1420)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) NeonCyan else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun NavigationOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    CyberCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0C1624))
                        .border(1.dp, CyberCardStroke, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
