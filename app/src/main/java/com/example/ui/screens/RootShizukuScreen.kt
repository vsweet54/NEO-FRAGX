package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ModuleAddon
import com.example.data.model.RootShizukuStatus
import com.example.tuner.CommandResult
import com.example.tuner.GameOptimizer
import com.example.tuner.ShellExecutor
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun RootShizukuScreen(
    rootStatus: RootShizukuStatus,
    shellExecutor: ShellExecutor,
    gameOptimizer: GameOptimizer
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var addons by remember { mutableStateOf(gameOptimizer.getInstalledAddons()) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Modul Addon, 1: Baris Perintah Terminal

    // Terminal state
    var terminalInput by remember { mutableStateOf("") }
    var terminalOutput by remember { mutableStateOf("NEO FRAGX Shell v3.0 [Ketik 'id' atau 'getprop' untuk memulai]\n# ") }
    var isRunningCmd by remember { mutableStateOf(false) }

    fun refreshAddons() {
        addons = gameOptimizer.getInstalledAddons()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Status Root & Shizuku Card
        item {
            CyberCard(glow = rootStatus.hasRoot, glowColor = if (rootStatus.hasRoot) StatusSuccess else NeonCyan) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Text(
                            text = "Status Akses Tingkat Sistem",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = rootStatus.permissionSummary,
                            fontSize = 11.sp,
                            color = if (rootStatus.hasRoot) StatusSuccess else NeonCyan
                        )
                    }
                    StatusBadge(
                        text = if (rootStatus.hasRoot) "ROOT AKTIF" else if (rootStatus.hasShizuku) "SHIZUKU" else "USERLAND",
                        color = if (rootStatus.hasRoot) StatusSuccess else if (rootStatus.hasShizuku) NeonBlue else StatusWarning
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPill(
                        label = "Root SU",
                        active = rootStatus.hasRoot,
                        activeText = "Ada",
                        inactiveText = "Tidak Ada"
                    )
                    StatusPill(
                        label = "Shizuku",
                        active = rootStatus.hasShizuku,
                        activeText = "Tersedia",
                        inactiveText = "Nonaktif"
                    )
                    StatusPill(
                        label = "Magisk / KSU",
                        active = rootStatus.magiskDetected || rootStatus.kernelSuDetected,
                        activeText = "Terdeteksi",
                        inactiveText = "Tidak Ada"
                    )
                }
            }
        }

        // Tab Selector: Modul Addon vs Baris Perintah
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberCard)
                    .border(1.dp, CyberCardStroke, RoundedCornerShape(10.dp))
                    .padding(4.dp)
            ) {
                TabButton(
                    text = "Modul Addon",
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    text = "Baris Perintah Terminal",
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (selectedTab == 0) {
            // Modul Addon Section (Matching Screenshot 1)
            item {
                Text(
                    text = "Addon adalah skrip modular berupa tweak atau alat khusus yang dapat dipasang dan dihapus dengan bebas. Setiap addon menjalankan skripnya sendiri untuk menambahkan mod UI, peningkatan performa, atau patch sistem ke NEO FRAGX dan aman untuk diuji.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }

            // Install Button (Screenshot 1)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .background(Color(0xFF0C1624))
                        .clickable {
                            val newModule = ModuleAddon(
                                id = "custom_addon_${System.currentTimeMillis()}",
                                name = "GameBoost Pro Tweak",
                                description = "Skrip alokasi governor CPU multi-core dan pembersihan cache otomatis saat game berjalan.",
                                version = "v1.2.0",
                                author = "NEO Community",
                                permissionType = "root",
                                isRunning = true,
                                isInstalled = true
                            )
                            gameOptimizer.addAddon(newModule)
                            refreshAddons()
                            Toast.makeText(context, "Modul 'GameBoost Pro' berhasil diinstal!", Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Instal dari Penyimpanan Internal",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
            }

            // Header Addon Terinstal
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.ViewInAr, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Addon Terinstal",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "• ${addons.size} Installed",
                        fontSize = 11.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Installed Addon Cards
            items(addons, key = { it.id }) { addon ->
                AddonModuleCard(
                    addon = addon,
                    onToggle = { active ->
                        gameOptimizer.toggleAddon(addon.id, active)
                        refreshAddons()
                    },
                    onDelete = {
                        gameOptimizer.deleteAddon(addon.id)
                        refreshAddons()
                        Toast.makeText(context, "Modul dihapus", Toast.LENGTH_SHORT).show()
                    },
                    onReload = {
                        Toast.makeText(context, "Modul '${addon.name}' dimuat ulang", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        } else {
            // Baris Perintah Terminal Section (Screenshot 2)
            item {
                CyberCard(glow = true) {
                    Text(
                        text = "Baris Perintah Shell (Terminal)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Jalankan perintah langsung ke sistem perangkat. Perintah dieksekusi via shell dengan izin yang tersedia (Root/User).",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Terminal Console Output Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF04070B))
                            .border(1.dp, CyberCardStroke, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = terminalOutput,
                            fontSize = 11.sp,
                            color = TextCyan,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Command Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = terminalInput,
                            onValueChange = { terminalInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Contoh: id, getprop, dumpsys battery", fontSize = 11.sp, color = TextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = CyberCardStroke,
                                focusedContainerColor = CyberCard,
                                unfocusedContainerColor = CyberCard,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CyberButton(
                            text = if (isRunningCmd) "..." else "Kirim",
                            enabled = !isRunningCmd && terminalInput.isNotBlank(),
                            onClick = {
                                val cmd = terminalInput.trim()
                                terminalInput = ""
                                isRunningCmd = true
                                scope.launch {
                                    val res = shellExecutor.executeCommand(cmd, preferRoot = true)
                                    val out = if (res.stdout.isNotBlank()) res.stdout else if (res.stderr.isNotBlank()) "ERR: ${res.stderr}" else "[Selesai code ${res.exitCode}]"
                                    terminalOutput = "$terminalOutput\n$cmd\n$out\n# "
                                    isRunningCmd = false
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset Quick Commands
                    Text(text = "Perintah Cepat:", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QuickCmdChip(label = "id", onClick = { terminalInput = "id" })
                        QuickCmdChip(label = "getprop ro.product.model", onClick = { terminalInput = "getprop ro.product.model" })
                        QuickCmdChip(label = "dumpsys battery", onClick = { terminalInput = "dumpsys battery" })
                        QuickCmdChip(label = "Bersihkan Layar", onClick = { terminalOutput = "# " })
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, active: Boolean, activeText: String, inactiveText: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) StatusSuccess.copy(alpha = 0.12f) else Color(0xFF101620))
            .border(1.dp, if (active) StatusSuccess.copy(alpha = 0.4f) else CyberCardStroke, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$label: ${if (active) activeText else inactiveText}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) StatusSuccess else TextSecondary
        )
    }
}

@Composable
private fun TabButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) NeonCyan.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) NeonCyan else TextSecondary
        )
    }
}

@Composable
private fun AddonModuleCard(
    addon: ModuleAddon,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onReload: () -> Unit
) {
    CyberCard {
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
                        .background(Color(0xFF0C1A29))
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = addon.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = addon.description, fontSize = 11.sp, color = TextSecondary, maxLines = 2)
                }
            }
            StatusBadge(
                text = if (addon.isRunning) "RUNNING" else "STOPPED",
                color = if (addon.isRunning) NeonCyan else TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Metadata badges row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MiniBadge(addon.version)
            MiniBadge(addon.author)
            MiniBadge(addon.permissionType)
            MiniBadge("installed")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Actions: Hapus, Muat Ulang, Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.clickable { onDelete() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "HAPUS", fontSize = 11.sp, color = StatusDanger, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.clickable { onReload() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "MUAT ULANG", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                }
            }

            Switch(
                checked = addon.isRunning,
                onCheckedChange = { onToggle(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NeonCyan,
                    checkedTrackColor = NeonBlueDark,
                    uncheckedTrackColor = CyberDark
                )
            )
        }
    }
}

@Composable
private fun MiniBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF091018))
            .border(1.dp, CyberCardStroke, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun QuickCmdChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0E1724))
            .border(1.dp, CyberCardStroke, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = label, fontSize = 10.sp, color = NeonCyan, fontFamily = FontFamily.Monospace)
    }
}
