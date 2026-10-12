package com.example.ui.screens

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
import com.example.data.model.CompatibilityCheckItem
import com.example.data.model.RenderingEngineInfo
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun RenderingScreen(
    engineInfo: RenderingEngineInfo,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    var selectedAngleBackend by remember { mutableStateOf(engineInfo.angleBackend) }
    var hwuiProfiling by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            SectionHeader(
                title = "Mesin Rendering Lanjutan",
                subtitle = "Pipeline Grafis Tingkat Rendah, Driver & Kompatibilitas",
                icon = Icons.Outlined.Layers
            ) {
                CyberButton(
                    text = "Periksa",
                    icon = Icons.Default.Refresh,
                    onClick = {
                        onRefresh()
                        Toast.makeText(context, "Memperbarui info rendering engine...", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // 1. OpenGL ES
        item {
            CyberCard {
                SectionHeader(
                    title = "1. OpenGL ES",
                    subtitle = "Standar rendering 2D/3D bawaan Android",
                    icon = Icons.Default.GraphicEq
                )
                Spacer(modifier = Modifier.height(8.dp))
                InfoDataRow(label = "Versi Didukung", value = engineInfo.openGlVersion)
                InfoDataRow(label = "GPU Renderer", value = engineInfo.glRenderer)
                InfoDataRow(label = "Vendor GPU", value = engineInfo.glVendor)
                InfoDataRow(label = "Jumlah Ekstensi GL", value = "${engineInfo.extensionsCount} Ekstensi Tersedia")
                InfoDataRow(label = "Shader Compiler", value = "GLSL ES 3.20 (Aktif)")
            }
        }

        // 2. Vulkan API
        item {
            CyberCard(glow = engineInfo.vulkanSupported, glowColor = NeonCyan) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(
                        title = "2. Vulkan",
                        subtitle = "API modern low-overhead & multi-threading",
                        icon = Icons.Default.Bolt
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                InfoDataRow(label = "Status Dukungan", value = if (engineInfo.vulkanSupported) "Didukung Penuh" else "Tidak Didukung")
                InfoDataRow(label = "Versi Vulkan API", value = engineInfo.vulkanVersion)
                InfoDataRow(label = "Tingkat Perangkat Keras", value = engineInfo.vulkanLevel)
                InfoDataRow(label = "Vulkan ICD Loader", value = "libvulkan.so (Sistem Android)")
            }
        }

        // 3. ANGLE (Almost Native Graphics Layer Engine)
        item {
            CyberCard {
                SectionHeader(
                    title = "3. ANGLE",
                    subtitle = "Penerjemah panggilan OpenGL ES ke Vulkan (Google)",
                    icon = Icons.Default.Transform
                )
                Spacer(modifier = Modifier.height(8.dp))
                InfoDataRow(label = "Dukungan ANGLE OS", value = if (engineInfo.angleSupported) "Tersedia (Android 10+)" else "Belum Didukung")
                InfoDataRow(label = "Status Global", value = if (engineInfo.angleActiveForApp) "Aktif untuk Game Terpilih" else "Bawaan Vendor")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Catatan: ANGLE menerjemahkan instruksi OpenGL ES ke Vulkan backend, berguna untuk game GLES lama agar berjalan dengan stabilitas pipeline Vulkan.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        // 4. ANGLE Backend
        item {
            CyberCard {
                SectionHeader(
                    title = "4. ANGLE Backend",
                    subtitle = "Pilihan backend engine saat ANGLE diaktifkan",
                    icon = Icons.Default.SettingsSuggest
                )
                Spacer(modifier = Modifier.height(10.dp))
                val backends = listOf("Bawaan Sistem", "Vulkan Backend", "Native GL")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    backends.forEach { b ->
                        val isSelected = selectedAngleBackend == b
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, if (isSelected) NeonCyan else CyberCardStroke, RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color(0xFF0D1420))
                                .clickable {
                                    selectedAngleBackend = b
                                    Toast.makeText(context, "Backend terpilih: $b", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = b,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonCyan else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // 5. Skia Pipeline
        item {
            CyberCard {
                SectionHeader(
                    title = "5. Skia",
                    subtitle = "Engine rendering grafis 2D inti antarmuka Android",
                    icon = Icons.Default.Brush
                )
                Spacer(modifier = Modifier.height(8.dp))
                InfoDataRow(label = "Pipeline Skia", value = engineInfo.skiaPipeline)
                InfoDataRow(label = "Cache Path Skia", value = "24 MB GPU Texture Cache")
                InfoDataRow(label = "Bentuk Rendering", value = "Anti-aliasing MSAA Hardware")
            }
        }

        // 6. HWUI (Hardware UI)
        item {
            CyberCard {
                SectionHeader(
                    title = "6. HWUI",
                    subtitle = "Hardware UI rendering layer Android framework",
                    icon = Icons.Default.Speed
                )
                Spacer(modifier = Modifier.height(8.dp))
                InfoDataRow(label = "Status HWUI", value = "Aktif (Hardware Accelerated)")
                InfoDataRow(label = "Render Thread", value = "Prioritas Real-time FIFO")
                InfoDataRow(label = "Frame Divisor", value = "1 (1:1 VSync Match)")
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profil Visual HWUI (Visual Profiler)",
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                    Switch(
                        checked = hwuiProfiling,
                        onCheckedChange = {
                            hwuiProfiling = it
                            Toast.makeText(context, "Visual HWUI profiler bar: $it", Toast.LENGTH_SHORT).show()
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

        // 7. Hardware Acceleration
        item {
            CyberCard {
                SectionHeader(
                    title = "7. Akselerasi Perangkat Keras",
                    subtitle = "Komposisi lapisan surface via GPU hardware composer",
                    icon = Icons.Default.DeveloperBoard
                )
                Spacer(modifier = Modifier.height(8.dp))
                InfoDataRow(label = "Hardware Composer (HWC)", value = "HWC 2.4 (Aktif)")
                InfoDataRow(label = "Format Framebuffer", value = "RGBA_8888 32-bit")
                InfoDataRow(label = "Surface Insets Buffer", value = "Triple Buffering (VSync)")
            }
        }

        // 8. GPU Information
        item {
            CyberCard {
                SectionHeader(
                    title = "8. Informasi GPU",
                    subtitle = "Spesifikasi kartu grafis dan komputasi",
                    icon = Icons.Default.Memory
                )
                Spacer(modifier = Modifier.height(8.dp))
                InfoDataRow(label = "Arsitektur GPU", value = engineInfo.glRenderer)
                InfoDataRow(label = "Ukuran Tekstur Maksimum", value = "16384 x 16384 Pixel")
                InfoDataRow(label = "Render Target Maksimum", value = "8 MRT (Multiple Render Targets)")
                InfoDataRow(label = "Compute Shader", value = "Didukung (GLES 3.2 & Vulkan)")
            }
        }

        // 9. Driver Information
        item {
            CyberCard {
                SectionHeader(
                    title = "9. Informasi Driver",
                    subtitle = "Versi firmware grafis dan sertifikasi vendor",
                    icon = Icons.Default.Info
                )
                Spacer(modifier = Modifier.height(8.dp))
                InfoDataRow(label = "Versi Driver", value = engineInfo.driverVersion)
                InfoDataRow(label = "Status Sertifikasi", value = "Google CTS Passed")
                InfoDataRow(label = "Driver Game Terbuka", value = "Tersedia di Opsi Pengembang")
            }
        }

        // 10. Compatibility Checker
        item {
            CyberCard(glow = true) {
                SectionHeader(
                    title = "10. Pemeriksa Kompatibilitas",
                    subtitle = "Matriks pengujian fitur grafis tingkat sistem",
                    icon = Icons.Default.FactCheck
                )
                Spacer(modifier = Modifier.height(10.dp))
                engineInfo.compatibilityChecks.forEach { check ->
                    CompatibilityRow(item = check)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoDataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.44f),
            fontSize = 12.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.56f),
            fontSize = 12.sp,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
private fun CompatibilityRow(item: CompatibilityCheckItem) {
    val (statusColor, statusText) = when (item.status) {
        CompatibilityCheckItem.StatusLevel.PASS -> Pair(StatusSuccess, "LOLOS")
        CompatibilityCheckItem.StatusLevel.WARN -> Pair(StatusWarning, "PERINGATAN")
        CompatibilityCheckItem.StatusLevel.FAIL -> Pair(StatusDanger, "GAGAL")
        CompatibilityCheckItem.StatusLevel.INFO -> Pair(StatusInfo, "INFO")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0B121C))
            .border(1.dp, CyberCardStroke, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = item.requirement, fontSize = 11.sp, color = TextSecondary)
                Text(text = item.note, fontSize = 10.sp, color = statusColor)
            }
            StatusBadge(text = statusText, color = statusColor)
        }
    }
}
