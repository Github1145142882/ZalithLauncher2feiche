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

package com.movtery.zalithlauncher.ui.components

import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.movtery.layer_controller.layout.BackdropBlurConfig
import com.movtery.layer_controller.layout.BackdropBlurLayer
import com.movtery.layer_controller.layout.mapSourceRect

val LocalLauncherBackdropBlurConfig = staticCompositionLocalOf<BackdropBlurConfig?> {
    null
}

val LocalLauncherBackdropDepth = staticCompositionLocalOf { 0 }

val LocalLauncherBackdropWhiteOutline = staticCompositionLocalOf { false }
val LocalLauncherBackdropActive = staticCompositionLocalOf { false }

/**
 * 启动器组件毛玻璃层（同层仅保留一层，避免双重模糊）
 */
@Composable
fun LauncherBackdropLayer(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    enabled: Boolean = true,
    drawWhiteOutline: Boolean = LocalLauncherBackdropWhiteOutline.current,
    content: @Composable BoxScope.() -> Unit
) {
    val blurConfig = LocalLauncherBackdropBlurConfig.current
    val currentDepth = LocalLauncherBackdropDepth.current
    val canBlur = enabled &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        currentDepth <= 0 &&
        blurConfig != null

    var sourceOffset by remember { mutableStateOf(Offset.Zero) }
    var sourceSize by remember { mutableStateOf(IntSize.Zero) }

    val sourceRect = remember(
        canBlur,
        blurConfig?.frame,
        blurConfig?.sourceWidth,
        blurConfig?.sourceHeight,
        blurConfig?.captureWidth,
        blurConfig?.captureHeight,
        sourceOffset,
        sourceSize
    ) {
        if (!canBlur) {
            null
        } else {
            blurConfig.mapSourceRect(
                sourceOffset = sourceOffset,
                sourceSize = sourceSize
            )
        }
    }

    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            sourceSize = coordinates.size
            val pos = coordinates.positionInWindow()
            sourceOffset = Offset(pos.x, pos.y)
        }
    ) {
        if (sourceRect != null && canBlur) {
            val config = blurConfig
            Box(
                modifier = Modifier
                    .matchParentOverlaySize()
                    .clip(shape)
            ) {
                BackdropBlurLayer(
                    modifier = Modifier.matchParentOverlaySize(),
                    config = config,
                    srcOffset = sourceRect.first,
                    srcSize = sourceRect.second
                )
            }
        }

        CompositionLocalProvider(
            LocalLauncherBackdropDepth provides (currentDepth + 1)
        ) {
            content()
        }

        if (enabled && drawWhiteOutline) {
            Box(
                modifier = Modifier
                    .matchParentOverlaySize()
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.95f),
                        shape = shape
                    )
            )
        }
    }
}

private fun Modifier.matchParentOverlaySize(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = 0,
            minHeight = 0
        )
    )
    val width = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
    val height = if (constraints.hasBoundedHeight) constraints.maxHeight else placeable.height
    layout(width, height) {
        placeable.place(0, 0)
    }
}
