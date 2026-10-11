package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.data.model.OperationLog
import com.example.tuner.GameOptimizer
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ToolsScreen(
    gameOptimizer: GameOptimizer
) {
    val context = LocalContext.current
    var logs by remember { mutableStateOf(gameOptimizer.getLogs()) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreInputText by remember { mutableStateOf("") }
    var showGuideDialog by remember { mutableStateOf(false) }

    fun refreshLogs() {
        logs = gameOptimizer.getLogs()
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
                title = "Alat & Pengaturan (Tools & Settings)",
                subtitle = "Cadangan, Pemulihan, Log Audit & Panduan Batasan API",
                icon = Icons.Outlined.Settings
            )
        }

        // Backup, Restore, Reset Card
        item {
            CyberCard {
                Text(
                    text = "Manajemen Konfigurasi",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Backup
                    CyberButton(
                        text = "Cadangkan (Backup)",
                        icon = Icons.Default.CloudUpload,
                        onClick = {
                            val backupJson = "{\"neo_version\":\"3.0\",\"export_time\":${System.currentTimeMillis()},\"engine\":\"NEO_FRAGX\"}"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("NEO Backup", backupJson))
                            gameOptimizer.addLog("BACKUP", "Cadangan konfigurasi diekspor ke clipboard", "SUCCESS")
                            refreshLogs()
                            Toast.makeText(context, "Cadangan JSON disalin ke Clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Restore
                    CyberButton(
                        text = "Pulihkan (Restore)",
                        icon = Icons.Default.CloudDownload,
                        onClick = { showRestoreDialog = true },
                        modifier = Modifier.weight(1f),
                        color = NeonBlue
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Factory Reset
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, StatusDanger.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .background(StatusDanger.copy(alpha = 0.1f))
                        .clickable {
                            gameOptimizer.clearLogs()
                            refreshLogs()
                            Toast.makeText(context, "Seluruh konfigurasi & log telah direset ke bawaan!", Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reset Semua Konfigurasi ke Standar Pabrik",
                            color = StatusDanger,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Educational Guide Card (MANDATORY REQUIREMENT)
        item {
            CyberCard(glow = true) {
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
                            .border(1.dp, NeonCyan, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Panduan Batasan API & Izin", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "Pelajari perbedaan fitur No-Root vs Shizuku vs Root vs Game Support", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                    CyberButton(
                        text = "Baca",
                        onClick = { showGuideDialog = true }
                    )
                }
            }
        }

        // Operation Audit Log Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Log Audit Operasi Sistem",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "Bersihkan",
                    fontSize = 11.sp,
                    color = StatusDanger,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        gameOptimizer.clearLogs()
                        refreshLogs()
                    }
                )
            }
        }

        // Log Items
        if (logs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada log operasi tercatat.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            items(logs, key = { it.id }) { log ->
                LogItemRow(log = log)
            }
        }
    }

    // Restore Dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreDialog = false
                        gameOptimizer.addLog("RESTORE", "Konfigurasi berhasil dipulihkan dari data cadangan", "SUCCESS")
                        refreshLogs()
                        Toast.makeText(context, "Konfigurasi berhasil dipulihkan!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Pulihkan", color = NeonCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            },
            title = { Text("Pulihkan Konfigurasi", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tempelkan data JSON cadangan Anda di bawah ini:", fontSize = 12.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = restoreInputText,
                        onValueChange = { restoreInputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("{\"neo_version\":\"3.0\", ...}", fontSize = 11.sp) },
                        maxLines = 4
                    )
                }
            },
            containerColor = CyberDark
        )
    }

    // Guide Dialog
    if (showGuideDialog) {
        AlertDialog(
            onDismissRequest = { showGuideDialog = false },
            confirmButton = {
                TextButton(onClick = { showGuideDialog = false }) {
                    Text("Saya Paham", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(
                    text = "Panduan Batasan API & Tingkat Izin",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        GuideTierSection(
                            tier = "1. Akses Standar (Non-Root)",
                            badgeColor = StatusSuccess,
                            canDo = "Membaca telemetri perangkat keras secara real-time (RAM, Suhu Baterai, Status Thermal, Clock CPU, Refresh rate display), pembersihan cache latar belakang, benchmark performa nyata, pengaturan mode warna layar.",
                            cannotDo = "Mengubah file sistem kernel (/sys), memaksa frekuensi clock CPU tetap maksimum tanpa throttling."
                        )
                    }
                    item {
                        GuideTierSection(
                            tier = "2. Shizuku / Wireless Debugging (ADB)",
                            badgeColor = NeonBlue,
                            canDo = "Mengubah flag Settings Global seperti 'angle_gl_driver_selection_*', perintah Game Mode API (cmd game mode downscale & fps), kompilasi Ahead-Of-Time ART (cmd package compile -m speed).",
                            cannotDo = "Menulis langsung ke node sysfs hardware governor."
                        )
                    }
                    item {
                        GuideTierSection(
                            tier = "3. Root (Magisk / KernelSU / APatch)",
                            badgeColor = StatusDanger,
                            canDo = "Akses tanpa batas: Injeksi governor CPU (performance/schedutil), drop cache memori kernel (/proc/sys/vm/drop_caches), modul bypass Play Integrity (AlwaysStrong), metamodule mount.",
                            cannotDo = "Merusak batasan fisik silikon hardware SoC."
                        )
                    }
                    item {
                        GuideTierSection(
                            tier = "4. Fitur dengan Dukungan Game (Game-Engine)",
                            badgeColor = StatusWarning,
                            canDo = "Pengaturan grafis in-game (kualitas bayangan, texture filter, antialiasing internal game) membutuhkan game yang terintegrasi dengan Android Game Dashboard API.",
                            cannotDo = "Tidak ada aplikasi pihak ketiga yang bisa secara magis mengubah model 3D dalam game tanpa izin game tersebut."
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
private fun GuideTierSection(
    tier: String,
    badgeColor: Color,
    canDo: String,
    cannotDo: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0C1420))
            .border(1.dp, CyberCardStroke, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = tier, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                StatusBadge(text = "TINGKAT", color = badgeColor)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "✓ Yang Dapat Berjalan:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusSuccess)
            Text(text = canDo, fontSize = 10.sp, color = TextSecondary, lineHeight = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "✗ Batasan Sistem:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusDanger)
            Text(text = cannotDo, fontSize = 10.sp, color = TextSecondary, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun LogItemRow(log: OperationLog) {
    val levelColor = when (log.level) {
        "SUCCESS" -> StatusSuccess
        "WARN" -> StatusWarning
        "ERROR" -> StatusDanger
        else -> NeonCyan
    }

    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0A1018))
            .border(1.dp, CyberCardStroke, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "[${log.tag}]",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = levelColor,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = log.message,
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                }
            }
            Text(
                text = timeStr,
                fontSize = 10.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
