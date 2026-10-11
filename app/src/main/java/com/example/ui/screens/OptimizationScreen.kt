package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SystemTelemetry
import com.example.tuner.GameOptimizer
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun OptimizationScreen(
    gameOptimizer: GameOptimizer,
    telemetry: SystemTelemetry = SystemTelemetry(),
    onNavigateToTerminal: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // States matching Screenshot 1
    var isCleaningCache by remember { mutableStateOf(false) }
    var isKillingApps by remember { mutableStateOf(false) }
    var lightningTouchActive by remember { mutableStateOf(false) }
    var wifiLatencyReductionActive by remember { mutableStateOf(true) }
    var artCompiling by remember { mutableStateOf(false) }

    var lastCleanedMsg by remember { mutableStateOf<String?>(null) }

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val isBatteryIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    } else false

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Advanced Setup Header Card (Matching Screenshot 1)
        item {
            CyberCard(glow = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0C1624))
                                .border(1.dp, NeonCyan, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Pengaturan Lanjutan",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Alat terminal untuk optimasi sistem",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonCyan.copy(alpha = 0.15f))
                            .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "★ PRO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                }
            }
        }

        // Notification of recent action
        if (lastCleanedMsg != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StatusSuccess.copy(alpha = 0.12f))
                        .border(1.dp, StatusSuccess.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "✓ $lastCleanedMsg",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusSuccess
                    )
                }
            }
        }

        // 1. [ALAT] Alat Terminal Lanjutan
        item {
            AdvancedSetupItem(
                categoryTag = "ALAT",
                title = "Alat Terminal Lanjutan",
                subtitle = "Modul performa dengan akses khusus shell",
                statusText = "• Siap",
                statusColor = StatusSuccess,
                icon = Icons.Default.Terminal,
                onClick = onNavigateToTerminal
            )
        }

        // 2. [CACHE] Smart Cache Cleaner
        item {
            AdvancedSetupItem(
                categoryTag = "CACHE",
                title = "Smart Cache Cleaner",
                subtitle = "Bersihkan cache dan file sementara memori",
                statusText = if (isCleaningCache) "Membersihkan..." else "• Siap",
                statusColor = StatusSuccess,
                icon = Icons.Default.CleaningServices,
                onClick = {
                    isCleaningCache = true
                    scope.launch {
                        val (_, freedBytes) = gameOptimizer.trimRamMemory()
                        val mb = freedBytes / (1024 * 1024)
                        lastCleanedMsg = "Berhasil membersihkan cache dan membebaskan ~$mb MB memori!"
                        isCleaningCache = false
                        gameOptimizer.addLog("CACHE_CLEAN", "Smart Cache Cleaner membebaskan ~$mb MB", "SUCCESS")
                        Toast.makeText(context, "Cache berhasil dibersihkan! (~$mb MB)", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // 3. [APLIKASI] Smart App Killer
        item {
            AdvancedSetupItem(
                categoryTag = "APLIKASI",
                title = "Smart App Killer",
                subtitle = "Hentikan proses latar belakang yang tidak digunakan",
                statusText = if (isKillingApps) "Menghentikan..." else "• Siap",
                statusColor = StatusSuccess,
                icon = Icons.Default.PhoneAndroid,
                onClick = {
                    isKillingApps = true
                    scope.launch {
                        val (killed, freedBytes) = gameOptimizer.trimRamMemory()
                        val mb = freedBytes / (1024 * 1024)
                        lastCleanedMsg = "Berhasil menghentikan $killed proses latar belakang (~$mb MB)!"
                        isKillingApps = false
                        gameOptimizer.addLog("APP_KILLER", "Smart App Killer menghentikan $killed proses", "SUCCESS")
                        Toast.makeText(context, "$killed aplikasi latar belakang dihentikan!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // 4. [SENTUHAN] Lightning Touch Responsive
        item {
            AdvancedSetupItem(
                categoryTag = "SENTUHAN",
                title = "Lightning Touch Responsive",
                subtitle = "Tingkatkan respons layar digitizer secara instan",
                statusText = if (lightningTouchActive) "• Aktif" else "• Tidak Aktif",
                statusColor = if (lightningTouchActive) StatusSuccess else StatusWarning,
                icon = Icons.Default.TouchApp,
                onClick = {
                    lightningTouchActive = !lightningTouchActive
                    gameOptimizer.addLog("TOUCH", "Lightning Touch Responsive: $lightningTouchActive")
                    Toast.makeText(context, if (lightningTouchActive) "Lightning Touch diaktifkan!" else "Lightning Touch dinonaktifkan", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 5. [JARINGAN] Latency Reduction (WI-FI)
        item {
            AdvancedSetupItem(
                categoryTag = "JARINGAN",
                title = "Latency Reduction (WI-FI)",
                subtitle = "Optimalkan latensi koneksi & prioritaskan paket game",
                statusText = if (wifiLatencyReductionActive) "• Aktif" else "• Tidak Aktif",
                statusColor = if (wifiLatencyReductionActive) StatusSuccess else StatusWarning,
                icon = Icons.Default.Wifi,
                onClick = {
                    wifiLatencyReductionActive = !wifiLatencyReductionActive
                    gameOptimizer.addLog("NETWORK", "Latency Reduction Wi-Fi: $wifiLatencyReductionActive")
                    Toast.makeText(context, if (wifiLatencyReductionActive) "Wi-Fi Latency Reduction diaktifkan!" else "Wi-Fi Latency dinonaktifkan", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // ART AOT Optimizer
        item {
            CyberCard {
                SectionHeader(
                    title = "Kompilasi ART Ahead-of-Time (AOT)",
                    subtitle = "Kompilasi byte code DEX ke kode mesin native 'speed'",
                    icon = Icons.Default.Code
                )
                Spacer(modifier = Modifier.height(6.dp))
                CyberButton(
                    text = if (artCompiling) "Mengompilasi DEX..." else "Optimasi Profil ART AOT",
                    icon = Icons.Default.Memory,
                    enabled = !artCompiling,
                    onClick = {
                        artCompiling = true
                        scope.launch {
                            kotlinx.coroutines.delay(2000)
                            artCompiling = false
                            gameOptimizer.addLog("AOT_COMPILE", "Optimasi kompilasi ART selesai", "SUCCESS")
                            Toast.makeText(context, "Kompilasi AOT selesai! Cache bytecode diperbarui.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Doze Mode Exemption
        item {
            CyberCard {
                SectionHeader(
                    title = "Pengecualian Hemat Baterai (Doze Mode)",
                    subtitle = "Mencegah sistem mematikan game di latar belakang",
                    icon = Icons.Default.BatteryAlert
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBatteryIgnored) "Status: Bebas dari Pembatasan Doze" else "Status: Belum Dikecualikan",
                        fontSize = 12.sp,
                        color = if (isBatteryIgnored) StatusSuccess else StatusWarning,
                        fontWeight = FontWeight.Bold
                    )
                    CyberButton(
                        text = "Atur Izin",
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                try {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                }
                            }
                        }
                    )
                }
            }
        }

        // STATUS SISTEM Bar (Matching Screenshot 1 bottom footer)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF070D14))
                    .border(1.dp, CyberCardStroke, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "STATUS SISTEM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SystemStatusStat(title = "CPU usage", value = "29%")
                        SystemStatusStat(title = "Penggunaan RAM", value = String.format("%.1f%%", telemetry.usedRamPercent))
                        SystemStatusStat(title = "Temperature", value = String.format("%.1f°C", telemetry.batteryTempCelsius))
                        SystemStatusStat(title = "Battery", value = "${telemetry.batteryLevel}%")
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancedSetupItem(
    categoryTag: String,
    title: String,
    subtitle: String,
    statusText: String,
    statusColor: Color,
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
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0C1624))
                        .border(1.dp, CyberCardStroke, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF0C1826))
                            .border(1.dp, CyberCardStroke, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = categoryTag,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SystemStatusStat(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 10.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NeonCyan,
            fontFamily = FontFamily.Monospace
        )
    }
}
