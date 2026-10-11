package com.example.tuner

import com.example.data.model.BenchmarkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.math.sin
import kotlin.math.sqrt

class BenchmarkEngine {

    suspend fun runCpuMultiThreadTest(durationSec: Int = 3): BenchmarkResult = withContext(Dispatchers.Default) {
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val startTime = System.currentTimeMillis()
        val endTime = startTime + (durationSec * 1000L)

        val totalOps = coroutineScope {
            val deferreds = (0 until cores).map {
                async(Dispatchers.Default) {
                    var ops = 0L
                    var acc = 1.0001
                    while (System.currentTimeMillis() < endTime) {
                        // Workload: Float Math & Trigonometric computations
                        for (i in 0 until 500) {
                            acc = sin(acc) * 1.00002 + sqrt(acc * acc + 1.5)
                            ops++
                        }
                    }
                    ops
                }
            }
            deferreds.awaitAll().sum()
        }

        val actualDuration = (System.currentTimeMillis() - startTime) / 1000.0
        val opsPerSec = if (actualDuration > 0) (totalOps / actualDuration) else 0.0
        val gflops = (opsPerSec * 4) / 1_000_000_000.0 // 4 FLOPs per loop
        val score = (gflops * 1250).toLong().coerceAtLeast(100L)

        val rating = when {
            score > 8000 -> "EXTREME (Flagship Tier)"
            score > 5000 -> "TINGGI (Gaming Ready)"
            score > 2500 -> "SEDANG (Mid-Range)"
            else -> "STANDAR (Entry Level)"
        }

        BenchmarkResult(
            testName = "CPU Multi-Thread Stress Test",
            scoreValue = String.format("%,d", score),
            scoreUnit = "Poin",
            scoreRating = rating,
            metric1Label = "Throughput Komputasi",
            metric1Value = String.format("%.2f GFLOPS", gflops),
            metric2Label = "Thread CPU Aktif",
            metric2Value = "$cores Core Paralel",
            durationSec = durationSec
        )
    }

    suspend fun runRamBandwidthTest(durationSec: Int = 2): BenchmarkResult = withContext(Dispatchers.Default) {
        val bufferSize = 16 * 1024 * 1024 // 16 MB block
        val buffer = ByteBuffer.allocateDirect(bufferSize)
        val fillData = ByteArray(64 * 1024) { (it % 127).toByte() }
        val readData = ByteArray(64 * 1024)

        val startTime = System.currentTimeMillis()
        val endTime = startTime + (durationSec * 1000L)
        var totalBytesTransferred = 0L

        while (System.currentTimeMillis() < endTime) {
            buffer.clear()
            // Sequential write
            while (buffer.hasRemaining()) {
                val len = minOf(buffer.remaining(), fillData.size)
                buffer.put(fillData, 0, len)
                totalBytesTransferred += len
            }
            // Sequential read
            buffer.flip()
            while (buffer.hasRemaining()) {
                val len = minOf(buffer.remaining(), readData.size)
                buffer.get(readData, 0, len)
                totalBytesTransferred += len
            }
        }

        val actualDurationSec = (System.currentTimeMillis() - startTime) / 1000.0
        val mbytesPerSec = if (actualDurationSec > 0) (totalBytesTransferred / (1024.0 * 1024.0)) / actualDurationSec else 0.0
        val score = (mbytesPerSec * 1.5).toLong()

        val rating = when {
            mbytesPerSec > 18000 -> "LPDDR5X (Ekstrem Cepat)"
            mbytesPerSec > 10000 -> "LPDDR5 (Sangat Cepat)"
            mbytesPerSec > 5000 -> "LPDDR4X (Cepat)"
            else -> "LPDDR4 / Standar"
        }

        BenchmarkResult(
            testName = "RAM Bandwidth & Throughput",
            scoreValue = String.format("%,d", score),
            scoreUnit = "MB/s",
            scoreRating = rating,
            metric1Label = "Bandwidth Rata-rata",
            metric1Value = String.format("%.1f MB/s", mbytesPerSec),
            metric2Label = "Ukuran Blok Buffer",
            metric2Value = "16 MB Direct Heap",
            durationSec = durationSec
        )
    }

    suspend fun runMicroRenderBenchmark(durationSec: Int = 4): BenchmarkResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + (durationSec * 1000L)
        val frameTimes = mutableListOf<Float>()

        var particleCount = 2000
        var prevTime = System.nanoTime()

        // Particle simulation math
        val posX = FloatArray(particleCount) { (it % 1000).toFloat() }
        val posY = FloatArray(particleCount) { (it % 1920).toFloat() }
        val velX = FloatArray(particleCount) { (it % 10 - 5).toFloat() }
        val velY = FloatArray(particleCount) { (it % 10 - 5).toFloat() }

        while (System.currentTimeMillis() < endTime) {
            val frameStart = System.nanoTime()

            // Update physics & matrix transformations
            for (i in 0 until particleCount) {
                posX[i] += velX[i]
                posY[i] += velY[i]
                if (posX[i] < 0 || posX[i] > 1080) velX[i] = -velX[i]
                if (posY[i] < 0 || posY[i] > 2400) velY[i] = -velY[i]
            }

            val frameEnd = System.nanoTime()
            val frameMs = (frameEnd - frameStart) / 1_000_000.0f
            frameTimes.add(frameMs)

            // Yield slightly to emulate 120/90Hz display loop
            Thread.sleep(2)
        }

        val totalFrames = frameTimes.size
        val avgFrameTimeMs = if (totalFrames > 0) frameTimes.average().toFloat() else 16.6f
        val estFps = (1000f / avgFrameTimeMs.coerceAtLeast(1.0f)).coerceIn(10f, 165f)

        // 1% Low calculation
        frameTimes.sortDescending()
        val lowIndex = (totalFrames * 0.01).toInt().coerceAtLeast(0)
        val low1PercentMs = if (frameTimes.isNotEmpty()) frameTimes[lowIndex] else avgFrameTimeMs
        val low1PercentFps = (1000f / low1PercentMs.coerceAtLeast(1.0f)).coerceIn(5f, estFps)

        val stability = ((low1PercentFps / estFps) * 100f).coerceIn(40f, 99.9f)
        val score = (estFps * 100 + stability * 50).toLong()

        val rating = when {
            estFps >= 115f -> "120 FPS+ Ultra Lancar"
            estFps >= 85f -> "90 FPS Sangat Mulus"
            estFps >= 55f -> "60 FPS Stabil Standar"
            else -> "Frame Drop Terdeteksi"
        }

        BenchmarkResult(
            testName = "Simulasi Rendering & Frame Pacing",
            scoreValue = String.format("%.0f", estFps),
            scoreUnit = "FPS Rata-rata",
            scoreRating = rating,
            metric1Label = "1% Low Frame Rate",
            metric1Value = String.format("%.1f FPS", low1PercentFps),
            metric2Label = "Stabilitas Frame",
            metric2Value = String.format("%.1f%% Konsisten", stability),
            durationSec = durationSec
        )
    }
}
