package com.example.ui.screens

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
import com.example.data.model.RootShizukuStatus
import com.example.data.model.SystemTelemetry
import com.example.tuner.ShellExecutor
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CpuGpuScreen(
    telemetry: SystemTelemetry,
    rootStatus: RootShizukuStatus,
    shellExecutor: ShellExecutor
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedGovernor by remember { mutableStateOf(telemetry.cpuGovernor) }
    var activeProfile by remember { mutableStateOf("Game Ekstrem") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Penyetel CPU & GPU (CPU & GPU Tuner)",
                subtitle = "Frekuensi Core, Profil Daya & CPU Governor Kernel",
                icon = Icons.Outlined.Memory
            )
        }

        // Performance Mode Selector
        item {
            CyberCard(glow = true) {
                Text(
                    text = "Mode Profil Performa",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                val profiles = listOf(
                    Triple("Hemat Daya", "Baterai Maksimal", StatusSuccess),
                    Triple("Seimbang", "Stabil Harian", NeonBlue),
                    Triple("Game Ekstrem", "Core Frekuensi Tinggi", NeonCyan),
                    Triple("Tanpa Batas", "Throttle Disabled", StatusDanger)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    profiles.forEach { (name, desc, color) ->
                        val isSelected = activeProfile == name
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, if (isSelected) color else CyberCardStroke, RoundedCornerShape(10.dp))
                                .background(if (isSelected) color.copy(alpha = 0.12f) else Color(0xFF0C1420))
                                .clickable {
                                    activeProfile = name
                                    Toast.makeText(context, "Mode diaktifkan: $name", Toast.LENGTH_SHORT).show()
                                    scope.launch {
                                        if (rootStatus.hasRoot) {
                                            val gov = if (name == "Hemat Daya") "powersave" else if (name == "Seimbang") "schedutil" else "performance"
                                            shellExecutor.executeCommand("echo $gov > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", preferRoot = true)
                                        }
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { activeProfile = name },
                                        colors = RadioButtonDefaults.colors(selectedColor = color, unselectedColor = TextSecondary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSelected) color else TextPrimary)
                                        Text(text = desc, fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                                if (isSelected) {
                                    StatusBadge(text = "AKTIF", color = color)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live CPU Core Clusters
        item {
            CyberCard {
                SectionHeader(
                    title = "Kluster Frekuensi CPU Real-time",
                    subtitle = "Pemantauan core per detik (${telemetry.cpuCores} Core Aktif)",
                    icon = Icons.Default.Speed
                )
                Spacer(modifier = Modifier.height(10.dp))
                telemetry.coreFrequenciesMhz.forEachIndexed { idx, freq ->
                    val isPrime = idx >= 6
                    val isBig = idx in 4..5
                    val coreLabel = if (isPrime) "Core $idx (Cortex-X Prime)" else if (isBig) "Core $idx (Big Core)" else "Core $idx (Efficiency)"
                    val barColor = if (isPrime) StatusDanger else if (isBig) NeonCyan else NeonBlue
                    MetricProgressBar(
                        title = coreLabel,
                        valueLabel = "$freq MHz",
                        progress = (freq / 3200f).coerceIn(0.1f, 1f),
                        color = barColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // CPU Governor Selector
        item {
            CyberCard {
                SectionHeader(
                    title = "Kernel CPU Governor",
                    subtitle = "Algoritma scaling frekuensi governor Linux",
                    icon = Icons.Default.Tune
                )
                Spacer(modifier = Modifier.height(8.dp))
                val governors = listOf("schedutil", "performance", "interactive", "powersave")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    governors.forEach { gov ->
                        val isSelected = selectedGovernor.contains(gov)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, if (isSelected) NeonCyan else CyberCardStroke, RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color(0xFF0C1420))
                                .clickable {
                                    selectedGovernor = gov
                                    scope.launch {
                                        val res = shellExecutor.executeCommand("echo $gov > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", preferRoot = true)
                                        if (res.exitCode == 0) {
                                            Toast.makeText(context, "Governor diubah ke $gov via Root!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Governor $gov dipilih. Perlu Root untuk eksekusi kernel sysfs.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = gov,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonCyan else TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (rootStatus.hasRoot) "Status: Izin Root tersedia. Perintah langsung diinjeksikan ke kernel."
                           else "Status Non-Root: Menampilkan governor aktif saat ini ($selectedGovernor). Mengubah governor memerlukan Root/KernelSU.",
                    fontSize = 11.sp,
                    color = if (rootStatus.hasRoot) StatusSuccess else StatusWarning
                )
            }
        }

        // Thermal Throttling Sensor
        item {
            CyberCard {
                SectionHeader(
                    title = "Sensor & Status Thermal Throttling",
                    subtitle = "PowerManager & Kernel Thermal Governor",
                    icon = Icons.Default.Thermostat
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Status Suhu", fontSize = 12.sp, color = TextSecondary)
                        Text(text = telemetry.thermalStatus, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Suhu Baterai", fontSize = 12.sp, color = TextSecondary)
                        Text(
                            text = String.format("%.1f °C", telemetry.batteryTempCelsius),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (telemetry.batteryTempCelsius > 42f) StatusDanger else NeonCyan
                        )
                    }
                }
            }
        }
    }
}
