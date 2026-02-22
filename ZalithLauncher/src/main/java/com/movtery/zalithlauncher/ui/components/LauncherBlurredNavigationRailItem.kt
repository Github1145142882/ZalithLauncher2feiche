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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

@Composable
fun LauncherBlurredNavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val selectedColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
    val unselectedColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.46f)

    LauncherBackdropSurface(
        modifier = modifier,
        influencedByBackground = true,
        shape = RoundedCornerShape(20.dp),
        color = influencedByBackgroundColor(
            color = if (selected) selectedColor else unselectedColor,
            enabled = true
        ),
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    ) {
        val currentContentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
        CompositionLocalProvider(LocalContentColor provides currentContentColor) {
            Column(
                modifier = Modifier
                    .sizeIn(minWidth = 92.dp, minHeight = 92.dp)
                    .fillMaxSize()
                    .clickable(enabled = enabled, onClick = onClick)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    icon()
                }
                label?.let {
                    Box(
                        modifier = Modifier.padding(top = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        it()
                    }
                }
            }
        }
    }
}
