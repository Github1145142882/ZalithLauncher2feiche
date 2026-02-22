/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.backdrop

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Canvas as NativeCanvas
import android.graphics.Color as NativeColor
import android.os.Build
import android.os.SystemClock
import androidx.compose.ui.graphics.asImageBitmap
import com.movtery.zalithlauncher.game.launch.handler.BackdropFrame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors
import kotlin.math.roundToInt

enum class BackdropSamplingProfile {
    GAME_ULTRA_PERF,
    LAUNCHER_BALANCED
}

/**
 * 通用毛玻璃采样器（供游戏界面与启动器界面复用）
 */
class BackdropFrameSampler(
    private val refreshRateProvider: () -> Float
) {
    private enum class GovernorLevel {
        NORMAL,
        DEGRADED,
        CRITICAL
    }

    private val _frameFlow = MutableStateFlow<BackdropFrame?>(null)
    val frameFlow: StateFlow<BackdropFrame?> = _frameFlow.asStateFlow()

    private var lastCaptureAt = 0L
    private var bufferA: Bitmap? = null
    private var bufferB: Bitmap? = null
    private var opaqueBufferA: Bitmap? = null
    private var opaqueBufferB: Bitmap? = null
    private var convertCanvasA: NativeCanvas? = null
    private var convertCanvasB: NativeCanvas? = null
    private var useBufferA = true
    private var governorLevel = GovernorLevel.NORMAL
    private var captureFailureStreak = 0
    private var criticalCooldownUntilMs = 0L
    private var lastFrameBudgetMs = 16f
    private val captureCostHistoryMs = ArrayDeque<Long>()
    private val stateLock = Any()
    private val captureExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "BackdropFrameSampler").apply {
            isDaemon = true
        }
    }
    private var captureInFlight = false
    private var pendingRecycleBuffers = false
    private var pendingShutdown = false
    private var captureGeneration = 0L

    private val opaquePaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            blendMode = BlendMode.SRC
        } else {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        }
    }

    fun requestCapture(
        source: BackdropCaptureSource?,
        userFps: Int,
        blurRadius: Int = 0,
        profile: BackdropSamplingProfile = BackdropSamplingProfile.LAUNCHER_BALANCED,
        force: Boolean = false
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            clear(recycleBuffers = false)
            return
        }
        if (source == null) {
            clear(recycleBuffers = false)
            return
        }

        val now = SystemClock.elapsedRealtime()
        val effectiveFps: Int
        val intervalMs: Long
        val generation: Long

        synchronized(stateLock) {
            effectiveFps = getEffectiveFpsLocked(userFps, blurRadius, now, profile)
            intervalMs = (1000L / effectiveFps).coerceAtLeast(1L)
            if (!force && now - lastCaptureAt < intervalMs) return
            if (!force && governorLevel == GovernorLevel.CRITICAL && now < criticalCooldownUntilMs && now - lastCaptureAt < intervalMs * 2) return
            if (captureInFlight) return

            generation = captureGeneration
            captureInFlight = true
        }

        captureExecutor.execute {
            try {
                captureFrame(
                    source = source,
                    effectiveFps = effectiveFps,
                    blurRadius = blurRadius,
                    profile = profile,
                    generation = generation
                )
            } finally {
                synchronized(stateLock) {
                    captureInFlight = false
                    if (pendingRecycleBuffers) {
                        recycleBuffersInternal()
                        pendingRecycleBuffers = false
                    }
                    if (pendingShutdown) {
                        pendingShutdown = false
                        captureExecutor.shutdownNow()
                    }
                }
            }
        }
    }

    fun clear(recycleBuffers: Boolean) {
        synchronized(stateLock) {
            captureGeneration++
            _frameFlow.value = null
            lastCaptureAt = 0L
            governorLevel = GovernorLevel.NORMAL
            captureFailureStreak = 0
            criticalCooldownUntilMs = 0L
            lastFrameBudgetMs = 16f
            captureCostHistoryMs.clear()
            if (recycleBuffers) {
                if (captureInFlight) {
                    pendingRecycleBuffers = true
                    pendingShutdown = true
                } else {
                    recycleBuffersInternal()
                    captureExecutor.shutdownNow()
                }
            }
        }
    }

    private fun captureFrame(
        source: BackdropCaptureSource,
        effectiveFps: Int,
        blurRadius: Int,
        profile: BackdropSamplingProfile,
        generation: Long
    ) {
        val sourceSize = source.getSourceSize()
        val sourceWidth = sourceSize.width
        val sourceHeight = sourceSize.height
        if (sourceWidth <= 0 || sourceHeight <= 0) return

        synchronized(stateLock) {
            if (generation != captureGeneration) return
        }

        val governorSnapshot = synchronized(stateLock) {
            if (generation != captureGeneration) return
            governorLevel
        }
        val captureScale = getCaptureScale(
            blurRadius = blurRadius,
            governor = governorSnapshot,
            profile = profile
        )
        val blurRadiusFactor = getBlurRadiusFactor(governorSnapshot, profile)
        val captureWidth = (sourceWidth * captureScale).roundToInt().coerceAtLeast(1)
        val captureHeight = (sourceHeight * captureScale).roundToInt().coerceAtLeast(1)
        ensureBuffers(captureWidth, captureHeight)

        val captureTarget = (if (useBufferA) bufferA else bufferB) ?: return
        val opaqueTarget = (if (useBufferA) opaqueBufferA else opaqueBufferB) ?: return
        val convertCanvas = (if (useBufferA) convertCanvasA else convertCanvasB) ?: return

        val captureStartNs = SystemClock.elapsedRealtimeNanos()
        val sampled = source.capture(captureTarget)
        if (sampled == null) {
            markCaptureFailure(generation, effectiveFps, profile)
            return
        }
        if (source.rejectLikelyWhiteFrame && sampled.isLikelyInvalidWhiteFrame()) {
            markCaptureFailure(generation, effectiveFps, profile)
            return
        }

        synchronized(stateLock) {
            if (generation != captureGeneration) return
        }

        sampled.setHasAlpha(false)
        opaqueTarget.eraseColor(NativeColor.BLACK)
        convertCanvas.drawBitmap(sampled, 0f, 0f, opaquePaint)

        val captureNow = SystemClock.elapsedRealtime()
        _frameFlow.value = BackdropFrame(
            frame = opaqueTarget.asImageBitmap(),
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            captureWidth = captureWidth,
            captureHeight = captureHeight,
            qualityScale = captureScale,
            blurRadiusFactor = blurRadiusFactor,
            frameVersion = captureNow
        )

        val captureCostMs = ((SystemClock.elapsedRealtimeNanos() - captureStartNs) / 1_000_000L)
            .coerceAtLeast(0L)

        synchronized(stateLock) {
            updateCaptureCostState(
                captureCostMs = captureCostMs,
                effectiveFps = effectiveFps,
                nowMs = captureNow,
                profile = profile
            )
            useBufferA = !useBufferA
            lastCaptureAt = captureNow
        }
    }

    private fun markCaptureFailure(
        generation: Long,
        effectiveFps: Int,
        profile: BackdropSamplingProfile
    ) {
        val now = SystemClock.elapsedRealtime()
        synchronized(stateLock) {
            if (generation != captureGeneration) return
            captureFailureStreak = (captureFailureStreak + 1).coerceAtMost(12)
            val fallbackCost = ((1000f / effectiveFps.toFloat()) * 1.6f).roundToInt().toLong().coerceAtLeast(1L)
            pushCaptureCostSample(fallbackCost)
            lastFrameBudgetMs = (1000f / effectiveFps.toFloat()).coerceAtLeast(1f)
            recalculateGovernorLocked(now, profile)
            lastCaptureAt = now
        }
    }

    private fun getEffectiveFpsLocked(
        userFps: Int,
        blurRadius: Int,
        nowMs: Long,
        profile: BackdropSamplingProfile
    ): Int {
        val cappedUserFps = userFps.coerceIn(30, 120)
        val displayFps = refreshRateProvider().roundToInt().coerceIn(30, 240)
        val radiusCappedFps = getRadiusCapFps(blurRadius, profile)
        val governorCap = when {
            governorLevel == GovernorLevel.CRITICAL || nowMs < criticalCooldownUntilMs -> 20
            governorLevel == GovernorLevel.DEGRADED -> when (profile) {
                BackdropSamplingProfile.GAME_ULTRA_PERF -> 28
                BackdropSamplingProfile.LAUNCHER_BALANCED -> 36
            }
            else -> 60
        }
        return minOf(cappedUserFps, displayFps, radiusCappedFps, governorCap).coerceAtLeast(20)
    }

    private fun getRadiusCapFps(
        blurRadius: Int,
        profile: BackdropSamplingProfile
    ): Int {
        return when (profile) {
            BackdropSamplingProfile.GAME_ULTRA_PERF -> when (blurRadius.coerceAtLeast(0)) {
                in 0..4 -> 36
                in 5..10 -> 28
                else -> 20
            }
            BackdropSamplingProfile.LAUNCHER_BALANCED -> when (blurRadius.coerceAtLeast(0)) {
                in 0..4 -> 45
                in 5..10 -> 36
                else -> 24
            }
        }
    }

    private fun updateCaptureCostState(
        captureCostMs: Long,
        effectiveFps: Int,
        nowMs: Long,
        profile: BackdropSamplingProfile
    ) {
        pushCaptureCostSample(captureCostMs)
        lastFrameBudgetMs = (1000f / effectiveFps.toFloat()).coerceAtLeast(1f)
        captureFailureStreak = if (captureFailureStreak > 0) {
            captureFailureStreak - 1
        } else {
            0
        }
        recalculateGovernorLocked(nowMs, profile)
    }

    private fun pushCaptureCostSample(costMs: Long) {
        captureCostHistoryMs.addLast(costMs.coerceAtLeast(0L))
        while (captureCostHistoryMs.size > 8) {
            captureCostHistoryMs.removeFirst()
        }
    }

    private fun recalculateGovernorLocked(
        nowMs: Long,
        profile: BackdropSamplingProfile
    ) {
        if (nowMs < criticalCooldownUntilMs) {
            governorLevel = GovernorLevel.CRITICAL
            return
        }

        val averageCost = if (captureCostHistoryMs.isNotEmpty()) {
            captureCostHistoryMs.average().toFloat()
        } else {
            0f
        }
        val ratio = if (lastFrameBudgetMs > 0f) {
            averageCost / lastFrameBudgetMs
        } else {
            0f
        }

        governorLevel = when (profile) {
            BackdropSamplingProfile.GAME_ULTRA_PERF -> when {
                captureFailureStreak >= 2 || ratio >= 1.15f -> {
                    criticalCooldownUntilMs = nowMs + 900L
                    GovernorLevel.CRITICAL
                }
                captureFailureStreak >= 1 || ratio >= 0.78f -> GovernorLevel.DEGRADED
                else -> {
                    criticalCooldownUntilMs = 0L
                    GovernorLevel.NORMAL
                }
            }
            BackdropSamplingProfile.LAUNCHER_BALANCED -> when {
                captureFailureStreak >= 3 || ratio >= 1.35f -> {
                    criticalCooldownUntilMs = nowMs + 700L
                    GovernorLevel.CRITICAL
                }
                captureFailureStreak >= 1 || ratio >= 0.95f -> GovernorLevel.DEGRADED
                else -> {
                    criticalCooldownUntilMs = 0L
                    GovernorLevel.NORMAL
                }
            }
        }
    }

    private fun getCaptureScale(
        blurRadius: Int,
        governor: GovernorLevel,
        profile: BackdropSamplingProfile
    ): Float {
        val baseScale = when (profile) {
            BackdropSamplingProfile.GAME_ULTRA_PERF -> when (blurRadius.coerceAtLeast(0)) {
                in 0..4 -> 0.22f
                in 5..10 -> 0.17f
                else -> 0.13f
            }
            BackdropSamplingProfile.LAUNCHER_BALANCED -> when (blurRadius.coerceAtLeast(0)) {
                in 0..4 -> 0.30f
                in 5..10 -> 0.24f
                else -> 0.18f
            }
        }
        val governorFactor = when (governor) {
            GovernorLevel.NORMAL -> 1.00f
            GovernorLevel.DEGRADED -> when (profile) {
                BackdropSamplingProfile.GAME_ULTRA_PERF -> 0.82f
                BackdropSamplingProfile.LAUNCHER_BALANCED -> 0.85f
            }
            GovernorLevel.CRITICAL -> when (profile) {
                BackdropSamplingProfile.GAME_ULTRA_PERF -> 0.70f
                BackdropSamplingProfile.LAUNCHER_BALANCED -> 0.72f
            }
        }
        val minScale = when (profile) {
            BackdropSamplingProfile.GAME_ULTRA_PERF -> 0.10f
            BackdropSamplingProfile.LAUNCHER_BALANCED -> 0.12f
        }
        val maxScale = when (profile) {
            BackdropSamplingProfile.GAME_ULTRA_PERF -> 0.22f
            BackdropSamplingProfile.LAUNCHER_BALANCED -> 0.30f
        }
        return (baseScale * governorFactor).coerceIn(minScale, maxScale)
    }

    private fun getBlurRadiusFactor(
        governor: GovernorLevel,
        profile: BackdropSamplingProfile
    ): Float {
        return when (profile) {
            BackdropSamplingProfile.GAME_ULTRA_PERF -> when (governor) {
                GovernorLevel.NORMAL -> 0.65f
                GovernorLevel.DEGRADED -> 0.52f
                GovernorLevel.CRITICAL -> 0.40f
            }
            BackdropSamplingProfile.LAUNCHER_BALANCED -> when (governor) {
                GovernorLevel.NORMAL -> 0.78f
                GovernorLevel.DEGRADED -> 0.62f
                GovernorLevel.CRITICAL -> 0.50f
            }
        }
    }

    private fun ensureBuffers(width: Int, height: Int) {
        val matchesA = bufferA?.let { it.width == width && it.height == height } == true
        val matchesB = bufferB?.let { it.width == width && it.height == height } == true
        if (matchesA && matchesB) return

        bufferA?.recycle()
        bufferB?.recycle()
        opaqueBufferA?.recycle()
        opaqueBufferB?.recycle()

        bufferA = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bufferB = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        opaqueBufferA = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565).apply {
            setHasAlpha(false)
        }
        opaqueBufferB = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565).apply {
            setHasAlpha(false)
        }
        convertCanvasA = opaqueBufferA?.let { NativeCanvas(it) }
        convertCanvasB = opaqueBufferB?.let { NativeCanvas(it) }
        useBufferA = true
    }

    private fun recycleBuffersInternal() {
        bufferA?.recycle()
        bufferB?.recycle()
        opaqueBufferA?.recycle()
        opaqueBufferB?.recycle()
        bufferA = null
        bufferB = null
        opaqueBufferA = null
        opaqueBufferB = null
        convertCanvasA = null
        convertCanvasB = null
        useBufferA = true
    }

    private fun Bitmap.isLikelyInvalidWhiteFrame(): Boolean {
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return true

        val stepX = (w / 8).coerceAtLeast(1)
        val stepY = (h / 8).coerceAtLeast(1)
        var total = 0
        var whiteCount = 0

        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                val pixel = getPixel(x, y)
                val r = NativeColor.red(pixel)
                val g = NativeColor.green(pixel)
                val b = NativeColor.blue(pixel)
                if (r >= 245 && g >= 245 && b >= 245) {
                    whiteCount++
                }
                total++
                x += stepX
            }
            y += stepY
        }

        if (total <= 0) return true
        return (whiteCount * 100 / total) >= 92
    }
}
