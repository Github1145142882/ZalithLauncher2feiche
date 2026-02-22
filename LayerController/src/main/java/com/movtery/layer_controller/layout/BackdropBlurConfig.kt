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

package com.movtery.layer_controller.layout

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * 控件毛玻璃渲染配置
 */
data class BackdropBlurConfig(
    val frame: ImageBitmap,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val captureWidth: Int,
    val captureHeight: Int,
    val radiusDp: Dp
)

/**
 * 一次性毛玻璃遮罩区域
 */
/**
 * 将控件在原始坐标中的位置映射为采样帧区域
 */
fun BackdropBlurConfig.mapSourceRect(
    sourceOffset: Offset,
    sourceSize: IntSize
): Pair<IntOffset, IntSize>? {
    if (
        sourceWidth <= 0 || sourceHeight <= 0 ||
        captureWidth <= 0 || captureHeight <= 0 ||
        sourceSize.width <= 0 || sourceSize.height <= 0
    ) {
        return null
    }

    val scaleX = captureWidth.toFloat() / sourceWidth.toFloat()
    val scaleY = captureHeight.toFloat() / sourceHeight.toFloat()

    val rawLeft = (sourceOffset.x * scaleX).roundToInt()
    val rawTop = (sourceOffset.y * scaleY).roundToInt()
    val rawWidth = (sourceSize.width * scaleX).roundToInt().coerceAtLeast(1)
    val rawHeight = (sourceSize.height * scaleY).roundToInt().coerceAtLeast(1)

    val left = rawLeft.coerceIn(0, captureWidth - 1)
    val top = rawTop.coerceIn(0, captureHeight - 1)
    val maxWidth = (captureWidth - left).coerceAtLeast(1)
    val maxHeight = (captureHeight - top).coerceAtLeast(1)
    val width = rawWidth.coerceIn(1, maxWidth)
    val height = rawHeight.coerceIn(1, maxHeight)

    return IntOffset(left, top) to IntSize(width, height)
}

/**
 * 渲染裁剪后的毛玻璃图层
 */
@Composable
fun BackdropBlurLayer(
    modifier: Modifier = Modifier,
    config: BackdropBlurConfig,
    srcOffset: IntOffset,
    srcSize: IntSize
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .blur(config.radiusDp)
    ) {
        drawImage(
            image = config.frame,
            srcOffset = srcOffset,
            srcSize = srcSize,
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
        )
    }
}
