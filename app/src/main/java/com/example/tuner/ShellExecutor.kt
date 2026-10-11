package com.example.tuner

import android.content.Context
import android.content.pm.PackageManager
import com.example.data.model.RootShizukuStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class CommandResult(
    val command: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long,
    val executedAsRoot: Boolean
)

class ShellExecutor(private val context: Context) {

    suspend fun checkRootAndShizuku(): RootShizukuStatus = withContext(Dispatchers.IO) {
        // 1. Check Root Binary
        val suPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su"
        )
        var suPathFound = ""
        for (path in suPaths) {
            if (File(path).exists()) {
                suPathFound = path
                break
            }
        }

        var isRootWorking = false
        var rootMethod = "Tidak Terdeteksi"
        if (suPathFound.isNotEmpty()) {
            val testRes = executeInternal("id", asRoot = true, timeoutMs = 2500)
            if (testRes.exitCode == 0 && testRes.stdout.contains("uid=0")) {
                isRootWorking = true
            }
        }

        // 2. Check Magisk / KernelSU / APatch
        val magiskDir = File("/data/adb/magisk")
        val magiskModules = File("/data/adb/modules")
        val ksuFile = File("/data/adb/ksu")
        val apatchFile = File("/data/adb/ap")

        val hasMagisk = magiskDir.exists() || magiskModules.exists()
        val hasKsu = ksuFile.exists()
        val hasApatch = apatchFile.exists()

        if (isRootWorking) {
            rootMethod = when {
                hasKsu -> "KernelSU (Kernel-level)"
                hasApatch -> "APatch (Kernel Patch)"
                hasMagisk -> "Magisk SuperUser"
                else -> "Standard SU ($suPathFound)"
            }
        } else if (suPathFound.isNotEmpty()) {
            rootMethod = "Binary SU Ada (Izin Belum Diberikan)"
        }

        // 3. Check Shizuku
        var hasShizuku = false
        var shizukuVersion = 0
        try {
            val shizukuPkg = context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            hasShizuku = true
            shizukuVersion = shizukuPkg.versionCode
        } catch (_: PackageManager.NameNotFoundException) {
            hasShizuku = false
        }

        val summary = when {
            isRootWorking -> "Akses Root Penuh Aktif ($rootMethod)"
            hasShizuku -> "Shizuku Service Tersedia (ADB Privileged)"
            else -> "Mode Standar (Non-Root / Userland)"
        }

        RootShizukuStatus(
            hasRoot = isRootWorking,
            rootMethod = rootMethod,
            suBinaryPath = suPathFound,
            hasShizuku = hasShizuku,
            shizukuVersion = shizukuVersion,
            magiskDetected = hasMagisk,
            kernelSuDetected = hasKsu,
            apatchDetected = hasApatch,
            permissionSummary = summary
        )
    }

    suspend fun executeCommand(command: String, preferRoot: Boolean = false): CommandResult = withContext(Dispatchers.IO) {
        val rootStatus = checkRootAndShizuku()
        val runAsRoot = preferRoot && rootStatus.hasRoot
        executeInternal(command, asRoot = runAsRoot, timeoutMs = 7000)
    }

    private fun executeInternal(command: String, asRoot: Boolean, timeoutMs: Long): CommandResult {
        val startTime = System.currentTimeMillis()
        var process: Process? = null
        val stdout = StringBuilder()
        val stderr = StringBuilder()
        var exitCode = -1

        try {
            val shell = if (asRoot) "su" else "sh"
            process = ProcessBuilder(shell, "-c", command).start()

            val outReader = BufferedReader(InputStreamReader(process.inputStream))
            val errReader = BufferedReader(InputStreamReader(process.errorStream))

            val outThread = Thread {
                try {
                    outReader.forEachLine { line ->
                        if (stdout.length < 10000) stdout.append(line).append("\n")
                    }
                } catch (_: Exception) {}
            }
            val errThread = Thread {
                try {
                    errReader.forEachLine { line ->
                        if (stderr.length < 10000) stderr.append(line).append("\n")
                    }
                } catch (_: Exception) {}
            }

            outThread.start()
            errThread.start()

            // Wait with safety timeout
            val finished = process.waitFor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            if (finished) {
                outThread.join(500)
                errThread.join(500)
                exitCode = process.exitValue()
            } else {
                process.destroyForcibly()
                stderr.append("Timeout eksekusi perintah melebihi batas waktu.\n")
                exitCode = 124
            }
        } catch (e: Exception) {
            stderr.append("Gagal mengeksekusi shell: ").append(e.message ?: "Unknown error").append("\n")
            exitCode = 1
        } finally {
            process?.destroy()
        }

        val duration = System.currentTimeMillis() - startTime
        return CommandResult(
            command = command,
            exitCode = exitCode,
            stdout = stdout.toString().trim(),
            stderr = stderr.toString().trim(),
            durationMs = duration,
            executedAsRoot = asRoot
        )
    }
}
