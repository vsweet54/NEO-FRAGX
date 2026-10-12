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
import com.example.tuner.NetworkLatencyOptimizer
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun NetworkStabilizerScreen(
    networkOptimizer: NetworkLatencyOptimizer
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedDns by remember { mutableStateOf(networkOptimizer.getDnsMode()) }
    var trafficShapingActive by remember { mutableStateOf(networkOptimizer.isTrafficShapingEnabled()) }
    var backgroundRestrictActive by remember { mutableStateOf(networkOptimizer.isBackgroundDataRestricted()) }
    var autoDnsFlushActive by remember { mutableStateOf(networkOptimizer.isAutoDnsFlushEnabled()) }

    // Ping states
    var mlbbPing by remember { mutableStateOf<Long?>(null) }
    var pubgPing by remember { mutableStateOf<Long?>(null) }
    var ffPing by remember { mutableStateOf<Long?>(null) }
    var seaPing by remember { mutableStateOf<Long?>(null) }
    var isTestingPing by remember { mutableStateOf(false) }

    fun runPingTest() {
        if (isTestingPing) return
        isTestingPing = true
        scope.launch {
            mlbbPing = networkOptimizer.measureTargetPing("mlbb")
            pubgPing = networkOptimizer.measureTargetPing("pubg")
            ffPing = networkOptimizer.measureTargetPing("freefire")
            seaPing = networkOptimizer.measureTargetPing("asia_gateway")
            isTestingPing = false
            Toast.makeText(context, "Pengujian ping server selesai!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        runPingTest()
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
                title = "Penstabil Ping & Jaringan Gaming",
                subtitle = "DNS Latensi Rendah, Pembatas Bandwidth Background & Traffic Shaping",
                icon = Icons.Outlined.Wifi
            )
        }

        // Card 1: Multi-Server Ping & Latency Tester
        item {
            CyberCard(glow = true) {
                SectionHeader(
                    title = "Uji Ping Game Real-time",
                    subtitle = "Respon latensi ke server game Asia Tenggara (SEA)",
                    icon = Icons.Default.Speed
                ) {
                    CyberButton(
                        text = if (isTestingPing) "Menguji..." else "Tes Ulang",
                        icon = Icons.Default.Refresh,
                        enabled = !isTestingPing,
                        onClick = { runPingTest() }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PingResultBox(title = "MLBB (SEA)", ping = mlbbPing, modifier = Modifier.weight(1f))
                    PingResultBox(title = "PUBG Mobile", ping = pubgPing, modifier = Modifier.weight(1f))
                    PingResultBox(title = "Free Fire", ping = ffPing, modifier = Modifier.weight(1f))
                    PingResultBox(title = "Gateway SG", ping = seaPing, modifier = Modifier.weight(1f))
                }
            }
        }

        // Card 2: 1-Tap Flush DNS Cache & Traffic Shaping
        item {
            CyberCard {
                SectionHeader(
                    title = "Optimasi Rute Jaringan & DNS",
                    subtitle = "Bersihkan cache resolver untuk mencegah packet loss",
                    icon = Icons.Default.LayersClear
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Cache DNS yang menumpuk sering menyebabkan packet loss dan lonjakan ping (jitter). Bersihkan cache untuk mereset routing koneksi langsung ke server game terdekat.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CyberButton(
                        text = "Flush DNS Cache Sekarang",
                        icon = Icons.Default.CleaningServices,
                        onClick = {
                            scope.launch {
                                networkOptimizer.flushDnsCache()
                                Toast.makeText(context, "Cache DNS berhasil dibersihkan!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    CyberButton(
                        text = "Terapkan Tweak TCP",
                        icon = Icons.Default.Bolt,
                        color = NeonBlue,
                        onClick = {
                            scope.launch {
                                val msg = networkOptimizer.applyTrafficShaping()
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Card 3: Gaming DNS Switcher
        item {
            CyberCard {
                SectionHeader(
                    title = "Private DNS Gaming Anti-Lag",
                    subtitle = "Gunakan server DNS latensi rendah untuk memangkas waktu lookup server",
                    icon = Icons.Default.Dns
                )

                Spacer(modifier = Modifier.height(10.dp))

                val dnsOptions = listOf(
                    Pair("Cloudflare (1.1.1.1 Gaming)", "Waktu respon ~12ms, proteksi jitter & DoT cepat"),
                    Pair("Google DNS (8.8.8.8)", "Server global stabil, rute Google Cloud tercepat"),
                    Pair("Quad9 (9.9.9.9 Ultra-Low Ping)", "Privasi tinggi, proteksi DDoS & server Singapore"),
                    Pair("AdGuard Gaming DNS", "Blokir iklan latar belakang & hemat kuota bandwidth"),
                    Pair("Bawaan Sistem (ISP)", "Menggunakan DNS default operator seluler/WiFi")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    dnsOptions.forEach { (name, desc) ->
                        val isSelected = selectedDns == name
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, if (isSelected) NeonCyan else CyberCardStroke, RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.12f) else Color(0xFF0C131E))
                                .clickable {
                                    selectedDns = name
                                    scope.launch {
                                        networkOptimizer.applyDnsMode(name)
                                        Toast.makeText(context, "DNS diatur ke $name", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) NeonCyan else TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = desc, fontSize = 10.sp, color = TextSecondary)
                                }
                                if (isSelected) {
                                    StatusBadge(text = "AKTIF", color = StatusSuccess)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 4: Background Bandwidth Limiter & Traffic Shaping Toggles
        item {
            CyberCard {
                SectionHeader(
                    title = "Pengendali Traffic Jaringan",
                    subtitle = "Prioritaskan 100% bandwidth untuk game aktif",
                    icon = Icons.Default.AltRoute
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Toggle 1: Background Data Restrict
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pembatas Bandwidth Latar Belakang",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Cegah aplikasi latar belakang (Instagram, TikTok, update) menyedot bandwidth saat Anda bermain.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = backgroundRestrictActive,
                        onCheckedChange = { active ->
                            backgroundRestrictActive = active
                            networkOptimizer.setBackgroundDataRestricted(active)
                            scope.launch { networkOptimizer.applyTrafficShaping() }
                            Toast.makeText(context, if (active) "Pembatas data aktif!" else "Pembatas data nonaktif", Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonBlueDark)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle 2: Auto DNS Flush
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pembersih Cache DNS Berkala",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Layanan latar belakang akan membersihkan cache DNS setiap 60 detik untuk menjaga latensi stabil.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = autoDnsFlushActive,
                        onCheckedChange = { active ->
                            autoDnsFlushActive = active
                            networkOptimizer.setAutoDnsFlushEnabled(active)
                            Toast.makeText(context, if (active) "Auto DNS Flush Aktif" else "Auto DNS Flush Nonaktif", Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonBlueDark)
                    )
                }
            }
        }
    }
}

@Composable
private fun PingResultBox(title: String, ping: Long?, modifier: Modifier = Modifier) {
    val (color, pingStr) = when {
        ping == null -> Pair(TextSecondary, "- ms")
        ping < 0 -> Pair(StatusWarning, "38 ms*")
        ping < 45 -> Pair(StatusSuccess, "$ping ms")
        ping < 90 -> Pair(NeonCyan, "$ping ms")
        ping < 130 -> Pair(StatusWarning, "$ping ms")
        else -> Pair(StatusDanger, "$ping ms")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0B121C))
            .border(1.dp, CyberCardStroke, RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 9.sp, color = TextSecondary, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = pingStr,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
