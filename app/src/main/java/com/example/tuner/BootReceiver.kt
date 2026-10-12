package com.example.tuner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val gameOptimizer = GameOptimizer(context)
            if (gameOptimizer.isLightningTouchEnabled() || gameOptimizer.isAutoJunkCleanerEnabled()) {
                NeoFragxBackgroundService.startService(context)
                gameOptimizer.addLog("BOOT", "NEO FRAGX Background Engine otomatis dipulihkan saat startup sistem", "SUCCESS")
            }
        }
    }
}
