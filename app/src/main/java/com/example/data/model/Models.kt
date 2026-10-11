package com.example.data.model

data class SystemTelemetry(
    val totalRamBytes: Long = 0L,
    val availableRamBytes: Long = 0L,
    val usedRamPercent: Float = 0f,
    val batteryLevel: Int = 0,
    val batteryTempCelsius: Float = 0f,
    val batteryVoltageMv: Int = 0,
    val isCharging: Boolean = false,
    val thermalStatus: String = "Normal",
    val cpuCores: Int = 8,
    val coreFrequenciesMhz: List<Int> = emptyList(),
    val cpuGovernor: String = "schedutil",
    val socModel: String = "ARM64 Processor",
    val deviceModel: String = "",
    val androidVersion: String = "",
    val uptimeHours: Float = 0f
)

data class CompatibilityCheckItem(
    val title: String,
    val requirement: String,
    val status: StatusLevel,
    val note: String
) {
    enum class StatusLevel { PASS, WARN, FAIL, INFO }
}

data class RenderingEngineInfo(
    val openGlVersion: String = "OpenGL ES 3.2",
    val glRenderer: String = "Adreno / Mali GPU",
    val glVendor: String = "GPU Vendor",
    val glVersion: String = "OpenGL ES 3.2 v1.0",
    val extensionsCount: Int = 0,
    val vulkanSupported: Boolean = true,
    val vulkanVersion: String = "1.3",
    val vulkanLevel: String = "Level 1 (Full Support)",
    val angleSupported: Boolean = false,
    val angleActiveForApp: Boolean = false,
    val angleBackend: String = "Bawaan Sistem",
    val skiaPipeline: String = "Skia Vulkan (skiavk)",
    val hardwareAcceleration: Boolean = true,
    val driverVersion: String = "V@0512.0",
    val driverDate: String = "Terbaru",
    val compatibilityChecks: List<CompatibilityCheckItem> = emptyList()
)

data class GameProfile(
    val packageName: String,
    val appName: String,
    val isGameCategory: Boolean = true,
    val resolutionScale: Int = 100, // 50, 65, 75, 85, 100
    val targetFps: Int = 0, // 0 = Default/Uncapped, 30, 45, 60, 90, 120
    val runtimeCompilerMode: String = "Bawaan", // Bawaan, speed-profile, speed, everything
    val angleGlDriver: String = "Bawaan", // Bawaan, Aktif (ANGLE), Nonaktif (Native)
    val driverPreference: String = "Bawaan", // Bawaan, Game Driver, System Driver, Prerelease
    val isTuned: Boolean = false,
    val lastOptimizedTime: Long = 0L
)

data class ModuleAddon(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val author: String,
    val permissionType: String = "root", // root, shizuku, system
    val isRunning: Boolean = true,
    val isInstalled: Boolean = true
)

data class BenchmarkResult(
    val testName: String,
    val scoreValue: String,
    val scoreUnit: String,
    val scoreRating: String, // EXTREME, TINGGI, SEDANG, RENDAH
    val metric1Label: String,
    val metric1Value: String,
    val metric2Label: String,
    val metric2Value: String,
    val durationSec: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class OperationLog(
    val id: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String,
    val level: String = "INFO", // INFO, SUCCESS, WARN, ERROR
    val timestamp: Long = System.currentTimeMillis()
)

data class RootShizukuStatus(
    val hasRoot: Boolean = false,
    val rootMethod: String = "Tidak Terdeteksi",
    val suBinaryPath: String = "",
    val hasShizuku: Boolean = false,
    val shizukuVersion: Int = 0,
    val magiskDetected: Boolean = false,
    val kernelSuDetected: Boolean = false,
    val apatchDetected: Boolean = false,
    val permissionSummary: String = "Akses Standar (Non-Root)"
)
