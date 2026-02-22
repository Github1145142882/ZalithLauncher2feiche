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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.LauncherSafeAreaMode

data class LauncherSafeAreaPadding(
    val start: Dp,
    val top: Dp,
    val end: Dp,
    val bottom: Dp
) {
    companion object {
        val Zero = LauncherSafeAreaPadding(0.dp, 0.dp, 0.dp, 0.dp)
    }
}

val LocalLauncherSafeAreaPadding = staticCompositionLocalOf {
    LauncherSafeAreaPadding.Zero
}

/**
 * 启动器页面安全区容器
 */
@Composable
fun LauncherSafeArea(
    applyContentPadding: Boolean = true,
    drawBackground: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    val fullScreen = AllSettings.launcherFullScreen.state
    val safeAreaMode = AllSettings.launcherSafeAreaMode.state
    val layoutDirection = LocalLayoutDirection.current

    val basePadding = when {
        !fullScreen || safeAreaMode == LauncherSafeAreaMode.IGNORE -> PaddingValues(0.dp)
        else -> WindowInsets.safeDrawing.asPaddingValues()
    }

    val horizontalPadding = when {
        fullScreen && safeAreaMode == LauncherSafeAreaMode.CUSTOM -> AllSettings.launcherSafeAreaHorizontal.state.dp
        else -> 0.dp
    }

    val verticalPadding = when {
        fullScreen && safeAreaMode == LauncherSafeAreaMode.CUSTOM -> AllSettings.launcherSafeAreaVertical.state.dp
        else -> 0.dp
    }

    val launcherSafeAreaPadding = LauncherSafeAreaPadding(
        start = basePadding.calculateStartPadding(layoutDirection) + horizontalPadding,
        end = basePadding.calculateEndPadding(layoutDirection) + horizontalPadding,
        top = basePadding.calculateTopPadding() + verticalPadding,
        bottom = basePadding.calculateBottomPadding() + verticalPadding
    )

    CompositionLocalProvider(
        LocalLauncherSafeAreaPadding provides launcherSafeAreaPadding
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .let {
                    if (drawBackground) {
                        it.background(backgroundColor)
                    } else {
                        it
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .let {
                        if (applyContentPadding) {
                            it.padding(
                                start = launcherSafeAreaPadding.start,
                                end = launcherSafeAreaPadding.end,
                                bottom = launcherSafeAreaPadding.bottom
                            )
                        } else {
                            it
                        }
                    }
            ) {
                content()
            }
        }
    }
}
