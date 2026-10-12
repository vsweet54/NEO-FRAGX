package com.example.tuner

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.model.GameProfile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    /**
     * Saves file to Download/NEO FRAGX/ folder using MediaStore (Scoped Storage compliant)
     * or standard Downloads fallback.
     */
    fun saveToDownloads(context: Context, fileName: String, content: String): Pair<Boolean, String> {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/NEO FRAGX")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        os.write(content.toByteArray(Charsets.UTF_8))
                    }
                    return Pair(true, "Download/NEO FRAGX/$fileName")
                }
            }

            // Fallback for older API or direct file
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val neoDir = File(downloadsDir, "NEO FRAGX")
            if (!neoDir.exists()) neoDir.mkdirs()
            val targetFile = File(neoDir, fileName)
            targetFile.writeText(content, Charsets.UTF_8)
            return Pair(true, "Download/NEO FRAGX/$fileName")
        } catch (e: Exception) {
            return Pair(false, e.localizedMessage ?: "Gagal menulis ke penyimpanan")
        }
    }

    /**
     * Creates a complete configuration backup JSON
     */
    fun createFullConfigBackupJson(
        gameOptimizer: GameOptimizer,
        networkOptimizer: NetworkLatencyOptimizer? = null,
        games: List<GameProfile>? = null
    ): String {
        val root = JSONObject()
        root.put("app", "NEO_FRAGX")
        root.put("version", "3.2")
        root.put("timestamp", System.currentTimeMillis())
        root.put("date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

        // System Tweak Settings
        val tweaks = JSONObject()
        tweaks.put("lightning_touch", gameOptimizer.isLightningTouchEnabled())
        tweaks.put("wifi_latency_reduction", gameOptimizer.isWifiLatencyReductionEnabled())
        tweaks.put("auto_junk_cleaner", gameOptimizer.isAutoJunkCleanerEnabled())
        tweaks.put("auto_clean_interval", gameOptimizer.getAutoCleanIntervalMinutes())
        tweaks.put("gaming_dns", gameOptimizer.getActiveDnsMode())
        root.put("tweaks", tweaks)

        // Network Optimizer Settings
        if (networkOptimizer != null) {
            val net = JSONObject()
            net.put("dns_mode", networkOptimizer.getDnsMode())
            net.put("traffic_shaping", networkOptimizer.isTrafficShapingEnabled())
            net.put("bg_restrict", networkOptimizer.isBackgroundDataRestricted())
            net.put("tcp_tweaks", networkOptimizer.isTcpOptimizationEnabled())
            net.put("auto_flush_dns", networkOptimizer.isAutoDnsFlushEnabled())
            root.put("network", net)
        }

        // Game Presets
        if (!games.isNullOrEmpty()) {
            val array = JSONArray()
            for (g in games) {
                val item = JSONObject()
                item.put("packageName", g.packageName)
                item.put("appName", g.appName)
                item.put("targetFps", g.targetFps)
                item.put("resolutionScale", g.resolutionScale)
                item.put("runtimeCompilerMode", g.runtimeCompilerMode)
                item.put("angleGlDriver", g.angleGlDriver)
                item.put("driverPreference", g.driverPreference)
                item.put("isTuned", g.isTuned)
                array.put(item)
            }
            root.put("presets", array)
        }

        return root.toString(2)
    }

    /**
     * Restores configuration from backup JSON
     */
    suspend fun restoreFullConfig(
        gameOptimizer: GameOptimizer,
        jsonStr: String,
        networkOptimizer: NetworkLatencyOptimizer? = null
    ): Boolean {
        try {
            val root = JSONObject(jsonStr)
            if (root.has("tweaks")) {
                val tweaks = root.getJSONObject("tweaks")
                if (tweaks.has("lightning_touch")) {
                    gameOptimizer.setLightningTouchEnabled(tweaks.getBoolean("lightning_touch"))
                }
                if (tweaks.has("wifi_latency_reduction")) {
                    gameOptimizer.setWifiLatencyReductionEnabled(tweaks.getBoolean("wifi_latency_reduction"))
                }
                if (tweaks.has("auto_junk_cleaner")) {
                    gameOptimizer.setAutoJunkCleanerEnabled(tweaks.getBoolean("auto_junk_cleaner"))
                }
                if (tweaks.has("auto_clean_interval")) {
                    gameOptimizer.setAutoCleanIntervalMinutes(tweaks.getInt("auto_clean_interval"))
                }
                if (tweaks.has("gaming_dns")) {
                    gameOptimizer.setGamingDns(tweaks.getString("gaming_dns"))
                }
            }

            if (root.has("network") && networkOptimizer != null) {
                val net = root.getJSONObject("network")
                if (net.has("dns_mode")) networkOptimizer.setDnsMode(net.getString("dns_mode"))
                if (net.has("traffic_shaping")) networkOptimizer.setTrafficShapingEnabled(net.getBoolean("traffic_shaping"))
                if (net.has("bg_restrict")) networkOptimizer.setBackgroundDataRestricted(net.getBoolean("bg_restrict"))
                if (net.has("tcp_tweaks")) networkOptimizer.setTcpOptimizationEnabled(net.getBoolean("tcp_tweaks"))
                if (net.has("auto_flush_dns")) networkOptimizer.setAutoDnsFlushEnabled(net.getBoolean("auto_flush_dns"))
            }

            if (root.has("presets")) {
                val installed = gameOptimizer.getInstalledGames()
                importGamePresets(gameOptimizer, jsonStr, installed)
            }

            gameOptimizer.addLog("RESTORE", "Konfigurasi & setelan berhasil dipulihkan dari file backup", "SUCCESS")
            return true
        } catch (_: Exception) {
            return false
        }
    }

    /**
     * Creates a game preset bundle JSON
     */
    fun createGamePresetsJson(games: List<GameProfile>): String {
        val root = JSONObject()
        root.put("app", "NEO_FRAGX")
        root.put("type", "game_presets")
        root.put("timestamp", System.currentTimeMillis())

        val array = JSONArray()
        for (g in games) {
            val item = JSONObject()
            item.put("packageName", g.packageName)
            item.put("appName", g.appName)
            item.put("targetFps", g.targetFps)
            item.put("resolutionScale", g.resolutionScale)
            item.put("runtimeCompilerMode", g.runtimeCompilerMode)
            item.put("angleGlDriver", g.angleGlDriver)
            item.put("driverPreference", g.driverPreference)
            item.put("isTuned", g.isTuned)
            array.put(item)
        }
        root.put("presets", array)
        return root.toString(2)
    }

    /**
     * Imports game presets from JSON
     */
    suspend fun importGamePresets(gameOptimizer: GameOptimizer, jsonStr: String, currentGames: List<GameProfile>): Int {
        var count = 0
        try {
            val root = JSONObject(jsonStr)
            if (root.has("presets")) {
                val array = root.getJSONArray("presets")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val pkg = obj.optString("packageName")
                    if (pkg.isNotEmpty()) {
                        val existing = currentGames.find { it.packageName == pkg }
                        val updated = (existing ?: GameProfile(
                            packageName = pkg,
                            appName = obj.optString("appName", "Game")
                        )).copy(
                            targetFps = obj.optInt("targetFps", 60),
                            resolutionScale = obj.optInt("resolutionScale", 100),
                            runtimeCompilerMode = obj.optString("runtimeCompilerMode", "Bawaan"),
                            angleGlDriver = obj.optString("angleGlDriver", "Bawaan"),
                            driverPreference = obj.optString("driverPreference", "Bawaan"),
                            isTuned = true
                        )
                        gameOptimizer.saveProfile(updated)
                        count++
                    }
                }
            }
        } catch (_: Exception) {}
        return count
    }
}
