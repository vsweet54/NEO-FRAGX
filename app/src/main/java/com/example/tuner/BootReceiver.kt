package com.example.tuner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val gameOptimizer = GameOptimizer(context)
            if (gameOptimizer.isAutoStartOnBootEnabled() || gameOptimizer.isLightningTouchEnabled() || 
                gameOptimizer.isAutoJunkCleanerEnabled() || gameOptimizer.isAutoDetectGameEnabled()) {
                NeoFragxBackgroundService.startService(context)
                gameOptimizer.addLog("BOOT", "Auto Start saat HP Menyala: Layanan pemantauan & engine game aktif setelah boot", "SUCCESS")
            }
        }
    }
}
