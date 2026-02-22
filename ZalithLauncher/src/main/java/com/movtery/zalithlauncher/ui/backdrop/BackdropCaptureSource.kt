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
import android.graphics.Canvas as NativeCanvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.HandlerThread
import android.view.PixelCopy
import android.view.Surface
import android.view.TextureView
import androidx.compose.ui.unit.IntSize
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 毛玻璃采样输入源
 */
interface BackdropCaptureSource {
    /**
     * 是否对采样结果执行白帧异常检测并丢弃
     */
    val rejectLikelyWhiteFrame: Boolean
        get() = true

    fun getSourceSize(): IntSize
    fun capture(target: Bitmap): Bitmap?
    fun release() {}
}

/**
 * 基于 TextureView 的动态采样源
 */
class TextureViewCaptureSource(
    private val textureViewProvider: () -> TextureView?,
    override val rejectLikelyWhiteFrame: Boolean = true
) : BackdropCaptureSource {
    override fun getSourceSize(): IntSize {
        val view = textureViewProvider() ?: return IntSize.Zero
        return IntSize(view.width, view.height)
    }

    override fun capture(target: Bitmap): Bitmap? {
        val view = textureViewProvider() ?: return null
        if (view.width <= 0 || view.height <= 0) return null
        return view.getBitmap(target)
    }
}

/**
 * 基于 PixelCopy 的 TextureView 动态采样源（GPU 拷贝）
 */
class PixelCopyTextureCaptureSource(
    private val textureViewProvider: () -> TextureView?,
    override val rejectLikelyWhiteFrame: Boolean = true
) : BackdropCaptureSource {
    private val timeoutMs = 45L

    override fun getSourceSize(): IntSize {
        val view = textureViewProvider() ?: return IntSize.Zero
        return IntSize(view.width, view.height)
    }

    override fun capture(target: Bitmap): Bitmap? {
        val view = textureViewProvider() ?: return null
        if (view.width <= 0 || view.height <= 0) return null
        val texture = view.surfaceTexture ?: return null

        val done = CountDownLatch(1)
        var resultCode = PixelCopy.ERROR_UNKNOWN
        val surface = Surface(texture)
        try {
            PixelCopy.request(
                surface,
                target,
                { result ->
                    resultCode = result
                    done.countDown()
                },
                PixelCopyDispatcher.handler
            )

            val completed = done.await(timeoutMs, TimeUnit.MILLISECONDS)
            return if (completed && resultCode == PixelCopy.SUCCESS) {
                target
            } else {
                null
            }
        } catch (_: Throwable) {
            return null
        } finally {
            surface.release()
        }
    }
}

/**
 * 基于静态图片的采样源（按 ContentScale.Crop 方式绘制）
 */
class StaticImageCaptureSource(
    private val bitmap: Bitmap,
    private val sourceSizeProvider: () -> IntSize
) : BackdropCaptureSource {
    override val rejectLikelyWhiteFrame: Boolean = false

    private val drawCanvas = NativeCanvas()
    private val drawPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val srcRect = Rect()
    private val dstRect = Rect()

    override fun getSourceSize(): IntSize = sourceSizeProvider()

    override fun capture(target: Bitmap): Bitmap? {
        val sourceSize = getSourceSize()
        val sourceWidth = sourceSize.width
        val sourceHeight = sourceSize.height
        if (sourceWidth <= 0 || sourceHeight <= 0) return null
        if (target.width <= 0 || target.height <= 0) return null
        if (bitmap.width <= 0 || bitmap.height <= 0) return null

        val srcAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        val dstAspect = sourceWidth.toFloat() / sourceHeight.toFloat()

        if (srcAspect > dstAspect) {
            val croppedWidth = (bitmap.height * dstAspect).toInt().coerceIn(1, bitmap.width)
            val left = ((bitmap.width - croppedWidth) / 2).coerceAtLeast(0)
            srcRect.set(left, 0, left + croppedWidth, bitmap.height)
        } else {
            val croppedHeight = (bitmap.width / dstAspect).toInt().coerceIn(1, bitmap.height)
            val top = ((bitmap.height - croppedHeight) / 2).coerceAtLeast(0)
            srcRect.set(0, top, bitmap.width, top + croppedHeight)
        }

        dstRect.set(0, 0, target.width, target.height)
        drawCanvas.setBitmap(target)
        drawCanvas.drawBitmap(bitmap, srcRect, dstRect, drawPaint)
        return target
    }

    override fun release() {
        if (!bitmap.isRecycled) bitmap.recycle()
    }
}

private object PixelCopyDispatcher {
    private val thread = HandlerThread("BackdropPixelCopy").apply { start() }
    val handler = Handler(thread.looper)
}
