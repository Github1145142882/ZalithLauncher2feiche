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

package com.movtery.zalithlauncher.ui.screens.content.settings.layouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.components.TitleAndSummary

/**
 * 根据卡片在UI组中的位置，选择不同的圆角形状
 */
enum class CardPosition {
    /**
     * 位于 UI 组的顶部
     * 左上角和右上角为外部形状，左下角和右下角为内部形状
     */
    Top,

    /**
     * 位于 UI 组的中部
     * 四个角都为内部形状
     */
    Middle,

    /**
     * 位于 UI 组的底部
     * 左上角和右上角为内部形状，左下角和右下角为外部形状
     */
    Bottom,

    /**
     * 单个 UI 组件
     * 四个角都为外部形状
     */
    Single
}

/**
 * 根据 UI 组件在组中的位置决定的形状
 */
@Composable
fun rememberSettingsCardShape(
    position: CardPosition,
    outerShape: Dp = 28.dp,
    innerShape: Dp = 4.dp
): Shape {
    return remember(position, outerShape, innerShape) {
        when (position) {
            CardPosition.Top -> RoundedCornerShape(
                topStart = outerShape,
                topEnd = outerShape,
                bottomStart = innerShape,
                bottomEnd = innerShape
            )
            CardPosition.Middle -> RoundedCornerShape(innerShape)
            CardPosition.Bottom -> RoundedCornerShape(
                topStart = innerShape,
                topEnd = innerShape,
                bottomStart = outerShape,
                bottomEnd = outerShape
            )
            CardPosition.Single -> RoundedCornerShape(outerShape)
        }
    }
}

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    position: CardPosition,
    influencedByBackground: Boolean = true,
    outerShape: Dp = 28.dp,
    innerShape: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = rememberSettingsCardShape(position, outerShape, innerShape)

    BackgroundCard(
        modifier = modifier,
        influencedByBackground = influencedByBackground,
        shape = shape,
        content = content
    )
}

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    position: CardPosition,
    influencedByBackground: Boolean = true,
    outerShape: Dp = 28.dp,
    innerShape: Dp = 4.dp,
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = rememberSettingsCardShape(position, outerShape, innerShape)

    BackgroundCard(
        modifier = modifier,
        influencedByBackground = influencedByBackground,
        shape = shape,
        onClick = onClick,
        enabled = enabled,
        content = content
    )
}

@Composable
fun SettingsCard(
    position: CardPosition,
    title: String,
    modifier: Modifier = Modifier,
    influencedByBackground: Boolean = true,
    summary: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.titleSmall,
    summaryStyle: TextStyle = MaterialTheme.typography.labelSmall,
    outerShape: Dp = 28.dp,
    innerShape: Dp = 4.dp,
    innerPadding: PaddingValues = PaddingValues(all = 16.dp),
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val shape = rememberSettingsCardShape(position, outerShape, innerShape)

    BackgroundCard(
        modifier = modifier,
        influencedByBackground = influencedByBackground,
        shape = shape,
        onClick = onClick,
        enabled = enabled
    ) {
        TitleAndSummary(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding),
            title = title,
            summary = summary,
            titleStyle = titleStyle,
            summaryStyle = summaryStyle
        )
    }
}

@Composable
fun SettingsCardColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content
    )
}
