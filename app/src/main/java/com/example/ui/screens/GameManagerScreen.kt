package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameProfile
import com.example.tuner.BackupManager
import com.example.tuner.FloatingFpsService
import com.example.tuner.GameOptimizer
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GameManagerScreen(
    gameOptimizer: GameOptimizer
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var games by remember { mutableStateOf<List<GameProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // Auto Feature states
    var isAutoDetectGame by remember { mutableStateOf(gameOptimizer.isAutoDetectGameEnabled()) }
    var isAutoLaunchAutoConfig by remember { mutableStateOf(gameOptimizer.isAutoLaunchAutoConfigEnabled()) }
    var isAutoStartOnBoot by remember { mutableStateOf(gameOptimizer.isAutoStartOnBootEnabled()) }

    // Dialog states
    var selectedGameForEdit by remember { mutableStateOf<GameProfile?>(null) }
    var showResolutionSubDialog by remember { mutableStateOf(false) }
    var showAddGameDialog by remember { mutableStateOf(false) }
    var showImportPresetDialog by remember { mutableStateOf(false) }
    var importPresetInput by remember { mutableStateOf("") }
    var isFloatingDockRunning by remember { mutableStateOf(FloatingFpsService.isRunning) }

    LaunchedEffect(Unit) {
        isFloatingDockRunning = FloatingFpsService.isRunning
    }

    fun refreshGames() {
        scope.launch {
            isLoading = true
            games = gameOptimizer.getInstalledGames()
            isLoading = false
        }
    }

    val importPresetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (!content.isNullOrEmpty()) {
                        val count = BackupManager.importGamePresets(gameOptimizer, content, games)
                        refreshGames()
                        Toast.makeText(context, "Berhasil mengimpor $count preset game dari file!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "File kosong!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal membaca file: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshGames()
    }

    val filteredGames = games.filter {
        it.appName.contains(searchQuery, ignoreCase = true) ||
        it.packageName.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Eksperimental Tweak Banner (Matching Screenshot 3)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F1A2A))
                        .border(1.dp, NeonCyan, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "eksperimental ",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tweak",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusWarning
                        )
                    }
                    Text(
                        text = "Optimalkan perilaku aplikasi tingkat lanjut untuk performa lebih lancar, rendering lebih bersih, dan kontrol lebih baik.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // 3 Quick Action Cards (Screenshot 3)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Muat Ulang Game
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Sync,
                    iconColor = StatusWarning,
                    title = "Muat Ulang Game",
                    subtitle = "Muat ulang daftar game dan perbarui...",
                    onClick = {
                        refreshGames()
                        Toast.makeText(context, "Daftar game diperbarui", Toast.LENGTH_SHORT).show()
                    }
                )

                // Tambah Game
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AddCircleOutline,
                    iconColor = StatusWarning,
                    title = "Tambah Game",
                    subtitle = "Tambahkan package game ke daftar...",
                    onClick = { showAddGameDialog = true }
                )

                // Reset Semua Konfigurasi
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.DeleteOutline,
                    iconColor = StatusWarning,
                    title = "Reset Semua",
                    subtitle = "Hapus semua konfigurasi game...",
                    onClick = {
                        scope.launch {
                            games.forEach { gameOptimizer.resetProfile(it.packageName) }
                            refreshGames()
                            Toast.makeText(context, "Semua profil game direset", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        // Fitur Otomatisasi (Auto Engine) Card
        item {
            CyberCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0C1726))
                            .border(1.dp, NeonCyan, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.AutoMode, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Fitur Otomatisasi (Auto Engine)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NeonCyan.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isAutoDetectGame) "AKTIF" else "STANDBY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAutoDetectGame) NeonCyan else StatusWarning
                                )
                            }
                        }
                        Text(
                            text = "Deteksi otomatis peluncuran game, auto profil & start saat boot",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 1. Auto Detect Game
                AutoToggleRow(
                    title = "Auto Detect Game",
                    description = "NEO FRAGX mendeteksi game yang mulai berjalan, lalu menerapkan konfigurasi yang didukung secara otomatis.",
                    icon = Icons.Default.Radar,
                    checked = isAutoDetectGame,
                    onCheckedChange = {
                        isAutoDetectGame = it
                        gameOptimizer.setAutoDetectGameEnabled(it)
                        Toast.makeText(context, if (it) "Auto Detect Game diaktifkan" else "Auto Detect Game dinonaktifkan", Toast.LENGTH_SHORT).show()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Auto Launch + Auto Config
                AutoToggleRow(
                    title = "Auto Launch + Auto Config",
                    description = "Pengguna membuka game dari ikon biasa, kemudian NEO FRAGX mengaktifkan profil game secara otomatis.",
                    icon = Icons.Default.RocketLaunch,
                    checked = isAutoLaunchAutoConfig,
                    onCheckedChange = {
                        isAutoLaunchAutoConfig = it
                        gameOptimizer.setAutoLaunchAutoConfigEnabled(it)
                        Toast.makeText(context, if (it) "Auto Launch + Config diaktifkan" else "Auto Launch + Config dinonaktifkan", Toast.LENGTH_SHORT).show()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Auto Start saat HP Menyala
                AutoToggleRow(
                    title = "Auto Start saat HP Menyala",
                    description = "NEO FRAGX memulai layanan pemantauan setelah boot jika Android mengizinkannya.",
                    icon = Icons.Default.PowerSettingsNew,
                    checked = isAutoStartOnBoot,
                    onCheckedChange = {
                        isAutoStartOnBoot = it
                        gameOptimizer.setAutoStartOnBootEnabled(it)
                        Toast.makeText(context, if (it) "Auto Start saat HP Menyala diaktifkan" else "Auto Start saat Boot dinonaktifkan", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // Floating In-Game Game Dock Overlay (Sidebar) Card
        item {
            CyberCard(glow = isFloatingDockRunning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0C1726))
                                .border(1.dp, NeonCyan, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Dock Game Mengambang (Overlay)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                StatusBadge(
                                    text = if (isFloatingDockRunning) "AKTIF" else "SIAP",
                                    color = if (isFloatingDockRunning) StatusSuccess else TextSecondary
                                )
                            }
                            Text(
                                text = "Panel mini di atas game: 1-Tap Boost RAM, DND, Touch Lock & FPS/Ping HUD.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    CyberButton(
                        text = if (isFloatingDockRunning) "Matikan" else "Buka Dock",
                        color = if (isFloatingDockRunning) StatusDanger else NeonCyan,
                        onClick = {
                            if (!isFloatingDockRunning) {
                                val started = FloatingFpsService.startOverlay(context)
                                isFloatingDockRunning = started
                                if (started) {
                                    Toast.makeText(context, "Game Dock mengambang aktif! Ketuk gelembung untuk membuka sidebar.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                FloatingFpsService.stopOverlay(context)
                                isFloatingDockRunning = false
                                Toast.makeText(context, "Game Dock ditutup.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }

        // Per-Game Performance Presets & Sharing
        item {
            CyberCard {
                SectionHeader(
                    title = "Preset Performa Game Populer",
                    subtitle = "Terapkan setelan optimal sekali sentuh atau bagikan preset ke teman",
                    icon = Icons.Default.AutoFixHigh
                )

                Spacer(modifier = Modifier.height(8.dp))

                val presets = listOf(
                    Triple("MLBB (SEA)", "120 FPS Max • Extreme Touch • Skia Vulkan", 120),
                    Triple("Genshin Impact", "60 FPS Stabil • Res 85% • ANGLE Vulkan", 60),
                    Triple("PUBG Mobile", "90 FPS Ultra • Game Driver • DND Auto", 90),
                    Triple("Free Fire", "90 FPS • Pointer Speed 7 • Prioritas Jaringan", 90)
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { (name, desc, targetFps) ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, CyberCardStroke, RoundedCornerShape(8.dp))
                                .background(Color(0xFF0C1420))
                                .clickable {
                                    val matchedGame = games.find { it.appName.contains(name.take(4), ignoreCase = true) }
                                    if (matchedGame != null) {
                                        val updated = matchedGame.copy(
                                            targetFps = targetFps,
                                            resolutionScale = if (name.contains("Genshin")) 85 else 100,
                                            angleGlDriver = if (name.contains("Genshin")) "Aktif (ANGLE)" else "Bawaan",
                                            isTuned = true
                                        )
                                        scope.launch {
                                            gameOptimizer.saveProfile(updated)
                                            refreshGames()
                                            Toast.makeText(context, "Preset $name diterapkan ke ${updated.appName}!", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Preset $name disiapkan! Pasang game ini untuk auto-apply.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                                    Text(text = desc, fontSize = 10.sp, color = TextSecondary)
                                }
                                StatusBadge(text = "TERAPKAN", color = NeonCyan)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CyberButton(
                        text = "Ekspor Preset",
                        icon = Icons.Default.Share,
                        modifier = Modifier.weight(1f),
                        fontSize = 11.5.sp,
                        onClick = {
                            val presetJson = BackupManager.createGamePresetsJson(games)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("NEO Game Preset", presetJson))
                            val (saved, path) = BackupManager.saveToDownloads(context, "neo_game_presets.json", presetJson)
                            if (saved) {
                                Toast.makeText(context, "Tersimpan di $path & disalin ke Clipboard!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Preset disalin ke Clipboard! ($path)", Toast.LENGTH_LONG).show()
                            }
                        }
                    )

                    CyberButton(
                        text = "Impor Preset",
                        icon = Icons.Default.Download,
                        modifier = Modifier.weight(1f),
                        fontSize = 11.5.sp,
                        color = NeonBlue,
                        onClick = { showImportPresetDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0A1422))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Folder Penyimpanan: Download/NEO FRAGX/neo_game_presets.json",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Search Bar & Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.VideogameAsset, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Daftar Game",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F1824))
                        .border(1.dp, CyberCardStroke, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Total ${games.size} Apps",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Search TextField
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cari judul game atau package...", fontSize = 12.sp, color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = CyberCardStroke,
                    focusedContainerColor = CyberCard,
                    unfocusedContainerColor = CyberCard,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        // Game List Items
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            }
        } else {
            items(filteredGames, key = { it.packageName }) { game ->
                GameListItem(
                    game = game,
                    onClick = {
                        selectedGameForEdit = game
                    }
                )
            }
        }

        // Instruction Footer Banner (Screenshot 3)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFF332A15), RoundedCornerShape(10.dp))
                .background(Color(0xFF14120B))
                .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = StatusWarning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Ketuk aplikasi untuk mengatur opsi lanjutan:",
                            fontSize = 12.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Runtime • Angle • Driver • Resolusi",
                            fontSize = 12.sp,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Modal Dialog 1: App Options (Matching Screenshot 4)
    if (selectedGameForEdit != null) {
        val currentGame = selectedGameForEdit!!
        AlertDialog(
            onDismissRequest = { selectedGameForEdit = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            gameOptimizer.saveProfile(currentGame)
                            selectedGameForEdit = null
                            refreshGames()
                            Toast.makeText(context, "Profil ${currentGame.appName} disimpan", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Terapkan", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedGameForEdit = null }) {
                    Text("Tutup", color = TextSecondary)
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppIconImage(
                        packageName = currentGame.packageName,
                        modifier = Modifier.size(36.dp),
                        isGameCategory = currentGame.isGameCategory,
                        isTuned = currentGame.isTuned
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = currentGame.appName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentGame.packageName,
                            fontSize = 10.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CyberButton(
                        text = "🚀 Buka Game & Auto Config",
                        icon = Icons.Default.RocketLaunch,
                        onClick = {
                            scope.launch {
                                gameOptimizer.saveProfile(currentGame)
                                gameOptimizer.launchGameWithAutoConfig(currentGame.packageName)
                                selectedGameForEdit = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Option 1: Runtime Compiler
                    TweakOptionItem(
                        title = "Runtime Compiler",
                        value = currentGame.runtimeCompilerMode,
                        onClick = {
                            val modes = listOf("Bawaan", "speed-profile", "speed", "everything")
                            val nextIdx = (modes.indexOf(currentGame.runtimeCompilerMode) + 1) % modes.size
                            selectedGameForEdit = currentGame.copy(runtimeCompilerMode = modes[nextIdx])
                        }
                    )

                    // Option 2: Angle GL Driver
                    TweakOptionItem(
                        title = "Angle GL Driver",
                        value = currentGame.angleGlDriver,
                        onClick = {
                            val states = listOf("Bawaan", "Aktif (ANGLE)", "Nonaktif (Native)")
                            val nextIdx = (states.indexOf(currentGame.angleGlDriver) + 1) % states.size
                            selectedGameForEdit = currentGame.copy(angleGlDriver = states[nextIdx])
                        }
                    )

                    // Option 3: Preferensi Driver
                    TweakOptionItem(
                        title = "Preferensi Driver",
                        value = currentGame.driverPreference,
                        onClick = {
                            val drivers = listOf("Bawaan", "Game Driver", "System Driver", "Prerelease")
                            val nextIdx = (drivers.indexOf(currentGame.driverPreference) + 1) % drivers.size
                            selectedGameForEdit = currentGame.copy(driverPreference = drivers[nextIdx])
                        }
                    )

                    // Option 4: Pengubah Resolusi
                    TweakOptionItem(
                        title = "Pengubah Resolusi",
                        value = "Buka",
                        isAction = true,
                        onClick = {
                            showResolutionSubDialog = true
                        }
                    )
                }
            },
            containerColor = CyberDark,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal Dialog 2: Game API Sub-Modal (Matching Screenshot 5)
    if (showResolutionSubDialog && selectedGameForEdit != null) {
        val game = selectedGameForEdit!!
        AlertDialog(
            onDismissRequest = { showResolutionSubDialog = false },
            confirmButton = {
                TextButton(onClick = { showResolutionSubDialog = false }) {
                    Text("Tutup", color = NeonCyan)
                }
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.SportsEsports, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = game.appName.uppercase(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row {
                            Text(text = "FPS Lebih Tinggi  •••••", fontSize = 10.sp, color = StatusSuccess)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Resolusi Ringan  •••••", fontSize = 10.sp, color = NeonCyan)
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Item 1: Turunkan Resolusi
                    GameApiOptionCard(
                        number = "1",
                        title = "Turunkan Resolusi (${game.resolutionScale}%)",
                        subtitle = "Turunkan resolusi agar performa GPU lebih ringan",
                        onClick = {
                            val scales = listOf(100, 85, 75, 65, 50)
                            val nextIdx = (scales.indexOf(game.resolutionScale) + 1) % scales.size
                            selectedGameForEdit = game.copy(resolutionScale = scales[nextIdx])
                        }
                    )

                    // Item 2: Atur FPS
                    GameApiOptionCard(
                        number = "2",
                        title = "Atur FPS (${if (game.targetFps == 0) "Bawaan" else "${game.targetFps} FPS"})",
                        subtitle = "Atur batas FPS sesuai kebutuhan layar",
                        onClick = {
                            val caps = listOf(0, 30, 60, 90, 120)
                            val nextIdx = (caps.indexOf(game.targetFps) + 1) % caps.size
                            selectedGameForEdit = game.copy(targetFps = caps[nextIdx])
                        }
                    )

                    // Item 3: Rendering
                    GameApiOptionCard(
                        number = "3",
                        title = "Rendering Backend",
                        subtitle = "Pilih backend rendering Vulkan atau ANGLE GLES",
                        onClick = {
                            Toast.makeText(context, "Rendering backend: ${game.angleGlDriver}", Toast.LENGTH_SHORT).show()
                        }
                    )

                    // Item 4: Reset Pengaturan
                    GameApiOptionCard(
                        number = "4",
                        title = "Reset Pengaturan",
                        subtitle = "Hapus semua konfigurasi Game API yang tersimpan",
                        isDestructive = true,
                        onClick = {
                            selectedGameForEdit = game.copy(resolutionScale = 100, targetFps = 0, runtimeCompilerMode = "Bawaan")
                            Toast.makeText(context, "Pengaturan game direset ke default", Toast.LENGTH_SHORT).show()
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    CyberButton(
                        text = "Buka Game Langsung",
                        icon = Icons.Default.PlayArrow,
                        onClick = {
                            gameOptimizer.launchGame(game.packageName)
                            showResolutionSubDialog = false
                            selectedGameForEdit = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            containerColor = CyberDark,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Add Game Dialog (With Installed Apps Picker & Real Logos)
    if (showAddGameDialog) {
        var selectedTab by remember { mutableStateOf(0) }
        var appFilterQuery by remember { mutableStateOf("") }
        var newPkgInput by remember { mutableStateOf("") }
        var newNameInput by remember { mutableStateOf("") }
        var installedList by remember { mutableStateOf<List<Triple<String, String, Boolean>>>(emptyList()) }
        var isAppsLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            isAppsLoading = true
            installedList = gameOptimizer.getAllInstalledApplicationsList()
            isAppsLoading = false
        }

        val filteredInstalled = installedList.filter {
            it.second.contains(appFilterQuery, ignoreCase = true) ||
            it.first.contains(appFilterQuery, ignoreCase = true)
        }

        AlertDialog(
            onDismissRequest = { showAddGameDialog = false },
            confirmButton = {
                if (selectedTab == 1) {
                    TextButton(
                        onClick = {
                            if (newPkgInput.isNotBlank()) {
                                val custom = GameProfile(
                                    packageName = newPkgInput.trim(),
                                    appName = newNameInput.ifBlank { newPkgInput.trim() },
                                    isGameCategory = true,
                                    isTuned = true
                                )
                                scope.launch {
                                    gameOptimizer.saveProfile(custom)
                                    showAddGameDialog = false
                                    refreshGames()
                                    Toast.makeText(context, "Game ditambahkan!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text("Simpan Manual", color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGameDialog = false }) {
                    Text("Tutup", color = TextSecondary)
                }
            },
            title = {
                Text("Tambah Game ke NEO FRAGX", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tab Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF09111C))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selectedTab == 0) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { selectedTab = 0 }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aplikasi Terpasang",
                                fontSize = 11.5.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) NeonCyan else TextSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selectedTab == 1) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { selectedTab = 1 }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Input Manual",
                                fontSize = 11.5.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) NeonCyan else TextSecondary
                            )
                        }
                    }

                    if (selectedTab == 0) {
                        OutlinedTextField(
                            value = appFilterQuery,
                            onValueChange = { appFilterQuery = it },
                            placeholder = { Text("Cari aplikasi / game...", fontSize = 11.5.sp) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp)) },
                            singleLine = true
                        )

                        if (isAppsLoading) {
                            Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(24.dp))
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredInstalled, key = { it.first }) { (pkg, label, isGame) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0C1522))
                                            .clickable {
                                                val profile = GameProfile(
                                                    packageName = pkg,
                                                    appName = label,
                                                    isGameCategory = isGame,
                                                    isTuned = true
                                                )
                                                scope.launch {
                                                    gameOptimizer.saveProfile(profile)
                                                    showAddGameDialog = false
                                                    refreshGames()
                                                    Toast.makeText(context, "$label ditambahkan!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AppIconImage(
                                            packageName = pkg,
                                            isGameCategory = isGame,
                                            isTuned = true,
                                            modifier = Modifier.size(34.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(text = pkg, fontSize = 10.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = FontFamily.Monospace)
                                        }
                                        Icon(
                                            imageVector = Icons.Default.AddCircle,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text("Masukkan Package Name game yang ingin ditambahkan:", fontSize = 12.sp, color = TextSecondary)
                        OutlinedTextField(
                            value = newPkgInput,
                            onValueChange = { newPkgInput = it },
                            placeholder = { Text("com.dts.freefireth", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newNameInput,
                            onValueChange = { newNameInput = it },
                            placeholder = { Text("Nama Game (Opsional)", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            },
            containerColor = CyberDark
        )
    }

    if (showImportPresetDialog) {
        AlertDialog(
            onDismissRequest = { showImportPresetDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (importPresetInput.isNotBlank()) {
                            scope.launch {
                                val count = BackupManager.importGamePresets(gameOptimizer, importPresetInput, games)
                                refreshGames()
                                showImportPresetDialog = false
                                Toast.makeText(context, "Berhasil mengimpor $count preset game!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            showImportPresetDialog = false
                        }
                    }
                ) {
                    Text("Terapkan Teks", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportPresetDialog = false }) {
                    Text("Tutup", color = TextSecondary)
                }
            },
            title = {
                Text("Impor Preset Game", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Impor preset konfigurasi game langsung dari file penyimpanan (.json) atau tempel kode:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    CyberButton(
                        text = "Pilih File dari Penyimpanan (.json)",
                        icon = Icons.Default.FolderOpen,
                        onClick = {
                            showImportPresetDialog = false
                            importPresetLauncher.launch("*/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Atau tempel teks JSON secara manual:", fontSize = 11.sp, color = TextSecondary)

                    OutlinedTextField(
                        value = importPresetInput,
                        onValueChange = { importPresetInput = it },
                        placeholder = { Text("{\"presets\":[...]}", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CyberCardStroke,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            containerColor = CyberDark
        )
    }
}

@Composable
private fun ActionCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CyberCardStroke, RoundedCornerShape(12.dp))
            .background(CyberCard)
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = subtitle, fontSize = 9.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun GameListItem(
    game: GameProfile,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CyberCardStroke, RoundedCornerShape(12.dp))
            .background(CyberCard)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Real Application Logo
                AppIconImage(
                    packageName = game.packageName,
                    isGameCategory = game.isGameCategory,
                    isTuned = game.isTuned,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = game.appName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = game.packageName,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (game.isTuned) "• Disesuaikan" else "• Bawaan",
                    fontSize = 11.sp,
                    color = if (game.isTuned) NeonCyan else StatusWarning,
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
fun AppIconImage(
    packageName: String,
    modifier: Modifier = Modifier,
    isGameCategory: Boolean = true,
    isTuned: Boolean = false
) {
    val context = LocalContext.current
    var bitmap by remember(packageName) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(packageName) {
        withContext(Dispatchers.IO) {
            try {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                val bmp = drawableToBitmap(drawable)
                if (bmp != null) {
                    bitmap = bmp.asImageBitmap()
                }
            } catch (_: Exception) {
                bitmap = null
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = null,
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, if (isTuned) NeonCyan else CyberCardStroke, RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0C1624))
                .border(1.dp, if (isTuned) NeonCyan else CyberCardStroke, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isGameCategory) Icons.Default.SportsEsports else Icons.Default.Apps,
                contentDescription = null,
                tint = if (isTuned) NeonCyan else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap? {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

@Composable
private fun AutoToggleRow(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (checked) NeonCyan.copy(alpha = 0.06f) else Color(0xFF0B121C))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (checked) NeonCyan.copy(alpha = 0.2f) else CyberCardStroke.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) NeonCyan else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (checked) TextPrimary else TextSecondary
                )
                Text(
                    text = description,
                    fontSize = 10.5.sp,
                    color = TextSecondary,
                    lineHeight = 14.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyberDark,
                checkedTrackColor = NeonCyan,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = CyberCardStroke
            )
        )
    }
}

@Composable
private fun TweakOptionItem(
    title: String,
    value: String,
    isAction: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, CyberCardStroke, RoundedCornerShape(8.dp))
            .background(Color(0xFF0B121C))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(
                text = value,
                fontSize = 12.sp,
                color = if (isAction) NeonCyan else if (value == "Off" || value == "Bawaan") StatusDanger else StatusSuccess,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GameApiOptionCard(
    number: String,
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, if (isDestructive) StatusDanger.copy(alpha = 0.4f) else CyberCardStroke, RoundedCornerShape(10.dp))
            .background(Color(0xFF0C1420))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
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
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDestructive) StatusDanger.copy(alpha = 0.2f) else NeonCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = number,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDestructive) StatusDanger else NeonCyan
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDestructive) StatusDanger else TextPrimary
                    )
                    Text(text = subtitle, fontSize = 10.sp, color = TextSecondary)
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
