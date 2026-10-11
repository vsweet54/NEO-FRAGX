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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tuner.GameOptimizer
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun GraphicsScreen(
    gameOptimizer: GameOptimizer
) {
    val context = LocalContext.current

    // Switches matching Screenshot 2 & 3
    var modeGrafisLancar by remember { mutableStateOf(true) }
    var akselerasiHardware by remember { mutableStateOf(true) }
    var akselerasiHardwareSF by remember { mutableStateOf(true) }
    var kontrolIntervalGpu by remember { mutableStateOf(false) }
    var modeAntiAliasingPaksa by remember { mutableStateOf(false) }

    // Chip selections matching Screenshot 2 & 3
    var komposisiTampilan by remember { mutableStateOf("GPU") } // CPU, GPU, SF, Auto
    var rendererGpu by remember { mutableStateOf("Vulkan") } // OpenGL ES, Vulkan, OpenGL Lite, Vulkan Lite

    // Info dialog state
    var infoDialogTitle by remember { mutableStateOf<String?>(null) }
    var infoDialogDesc by remember { mutableStateOf("") }

    var globalDownscalePercent by remember { mutableStateOf(100) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Graphic Setup",
                subtitle = "Penyetelan Grafis & Peningkatan Hardware GPU",
                icon = Icons.Outlined.PhotoSizeSelectLarge
            )
        }

        // Section PENINGKATAN GRAFIS (Matching Screenshot 2 & 3)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Memory, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PENINGKATAN GRAFIS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    letterSpacing = 1.sp
                )
            }
        }

        // Item 1: Mode Grafis Lancar
        item {
            GraphicToggleItem(
                icon = Icons.Default.Sync,
                title = "Mode Grafis Lancar",
                subtitle = "Utamakan output grafis yang stabil dan konsisten.",
                checked = modeGrafisLancar,
                onCheckedChange = {
                    modeGrafisLancar = it
                    gameOptimizer.addLog("GRAPHIC_SETUP", "Mode Grafis Lancar: $it")
                    Toast.makeText(context, "Mode Grafis Lancar: $it", Toast.LENGTH_SHORT).show()
                },
                onInfoClick = {
                    infoDialogTitle = "Mode Grafis Lancar"
                    infoDialogDesc = "Menyesuaikan alokasi swapchain SurfaceFlinger untuk memprioritaskan konsistensi frame time daripada burst rendering sementara."
                }
            )
        }

        // Item 2: Akselerasi Hardware
        item {
            GraphicToggleItem(
                icon = Icons.Default.DeveloperBoard,
                title = "Akselerasi Hardware",
                subtitle = "Gunakan sumber daya hardware untuk memproses grafis.",
                checked = akselerasiHardware,
                onCheckedChange = {
                    akselerasiHardware = it
                    gameOptimizer.addLog("GRAPHIC_SETUP", "Akselerasi Hardware: $it")
                },
                onInfoClick = {
                    infoDialogTitle = "Akselerasi Hardware"
                    infoDialogDesc = "Memastikan seluruh view UI dan render context dialihkan ke GPU processing alih-alih fallback CPU software rasterizer."
                }
            )
        }

        // Item 3: Akselerasi Hardware SF
        item {
            GraphicToggleItem(
                icon = Icons.Default.Layers,
                title = "Akselerasi Hardware SF",
                subtitle = "Optimalkan perilaku komposisi hardware SurfaceFlinger.",
                checked = akselerasiHardwareSF,
                onCheckedChange = {
                    akselerasiHardwareSF = it
                    gameOptimizer.addLog("GRAPHIC_SETUP", "Akselerasi Hardware SF: $it")
                },
                onInfoClick = {
                    infoDialogTitle = "Akselerasi Hardware SF"
                    infoDialogDesc = "SurfaceFlinger adalah compositor utama Android. Mengaktifkan optimasi ini meminimalkan delay antara frame commit dan scanout display."
                }
            )
        }

        // Item 4: Kontrol Interval GPU
        item {
            GraphicToggleItem(
                icon = Icons.Default.ElectricBolt,
                title = "Kontrol Interval GPU",
                subtitle = "Atur penanganan interval frame GPU.",
                checked = kontrolIntervalGpu,
                onCheckedChange = {
                    kontrolIntervalGpu = it
                    gameOptimizer.addLog("GRAPHIC_SETUP", "Kontrol Interval GPU: $it")
                },
                onInfoClick = {
                    infoDialogTitle = "Kontrol Interval GPU"
                    infoDialogDesc = "Mengatur VSync divisor dan cadence buffer queue untuk sinkronisasi render thread dengan GPU work queue."
                }
            )
        }

        // Item 5: Mode Anti-Aliasing Paksa
        item {
            GraphicToggleItem(
                icon = Icons.Default.BlurOn,
                title = "Mode Anti-Aliasing Paksa",
                subtitle = "Paksa tepian lebih halus pada aplikasi yang didukung.",
                checked = modeAntiAliasingPaksa,
                onCheckedChange = {
                    modeAntiAliasingPaksa = it
                    gameOptimizer.addLog("GRAPHIC_SETUP", "MSAA Anti-Aliasing Paksa: $it")
                },
                onInfoClick = {
                    infoDialogTitle = "Mode Anti-Aliasing Paksa"
                    infoDialogDesc = "Mengaktifkan 4x MSAA pada konteks OpenGL ES 2.0+ untuk mereduksi efek gerigi (jaggies) pada polygon 3D."
                }
            )
        }

        // Section KONTROL RENDERING (Matching Screenshot 2 & 3)
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.TrackChanges, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "KONTROL RENDERING",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    letterSpacing = 1.sp
                )
            }
        }

        // Komposisi Tampilan
        item {
            CyberCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Komposisi Tampilan",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Pilih cara Android menyusun lapisan tampilan.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = {
                        infoDialogTitle = "Komposisi Tampilan"
                        infoDialogDesc = "Menentukan apakah komposisi frame buffer layar dikerjakan oleh CPU, GPU rendering langsung, atau Hardware Composer (HWC)."
                    }) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val compOptions = listOf("CPU", "GPU", "HWC", "Auto")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    compOptions.forEach { opt ->
                        val isSelected = komposisiTampilan == opt
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, if (isSelected) NeonCyan else CyberCardStroke, RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color(0xFF0B121C))
                                .clickable {
                                    komposisiTampilan = opt
                                    Toast.makeText(context, "Komposisi tampilan: $opt", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (opt == "CPU") Icons.Default.Memory else if (opt == "GPU") Icons.Default.DeveloperBoard else Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = if (isSelected) NeonCyan else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = opt,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NeonCyan else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Atur Renderer GPU
        item {
            CyberCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Atur Renderer GPU",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Pilih renderer GPU yang digunakan Android.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = {
                        infoDialogTitle = "Atur Renderer GPU"
                        infoDialogDesc = "Mengalihkan backend grafis sistem antara OpenGL ES standar, Vulkan modern, atau varian Lite hemat resource."
                    }) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val renderers = listOf("OpenGL ES", "Vulkan", "OpenGL Lite", "Vulkan Lite")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    renderers.forEach { rend ->
                        val isSelected = rendererGpu == rend
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, if (isSelected) NeonCyan else CyberCardStroke, RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color(0xFF0B121C))
                                .clickable {
                                    rendererGpu = rend
                                    Toast.makeText(context, "Renderer terpilih: $rend", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 10.dp, horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = rend,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) NeonCyan else TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Skala Resolusi Buffer Surface
        item {
            CyberCard {
                SectionHeader(
                    title = "Skala Resolusi Buffer Rendering",
                    subtitle = "Menurunkan beban GPU dengan mengurangi ukuran render surface",
                    icon = Icons.Default.AspectRatio
                )
                Spacer(modifier = Modifier.height(10.dp))
                val scales = listOf(100, 85, 75, 65, 50)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    scales.forEach { s ->
                        val isSelected = globalDownscalePercent == s
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, if (isSelected) NeonCyan else CyberCardStroke, RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color(0xFF0C1420))
                                .clickable {
                                    globalDownscalePercent = s
                                    Toast.makeText(context, "Resolusi disetel ke $s%", Toast.LENGTH_SHORT).show()
                                    gameOptimizer.addLog("GRAPHICS", "Skala resolusi sistem disetel ke $s%")
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$s%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) NeonCyan else TextPrimary
                                )
                                Text(
                                    text = if (s == 100) "Asli" else if (s >= 75) "Optimal" else "Ringan",
                                    fontSize = 9.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Info Dialog
    if (infoDialogTitle != null) {
        AlertDialog(
            onDismissRequest = { infoDialogTitle = null },
            confirmButton = {
                TextButton(onClick = { infoDialogTitle = null }) {
                    Text("Tutup", color = NeonCyan)
                }
            },
            title = {
                Text(text = infoDialogTitle ?: "", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            },
            text = {
                Text(text = infoDialogDesc, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
            },
            containerColor = CyberDark,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun GraphicToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onInfoClick: () -> Unit
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onInfoClick) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonCyan,
                        checkedTrackColor = NeonBlueDark,
                        uncheckedTrackColor = CyberDark
                    )
                )
            }
        }
    }
}
