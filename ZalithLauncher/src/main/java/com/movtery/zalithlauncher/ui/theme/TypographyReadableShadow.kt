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

package com.movtery.zalithlauncher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle

private fun TextStyle.withShadow(shadow: Shadow): TextStyle {
    return merge(TextStyle(shadow = shadow))
}

fun readableShadowFor(onSurface: Color): Shadow {
    val luminance = onSurface.luminance()
    val useDarkShadow = luminance >= 0.55f
    return Shadow(
        color = if (useDarkShadow) {
            Color.Black.copy(alpha = 0.36f)
        } else {
            Color.White.copy(alpha = 0.22f)
        },
        offset = Offset(0f, 1.4f),
        blurRadius = 3.8f
    )
}

fun Typography.withReadableShadow(shadow: Shadow): Typography {
    return copy(
        displayLarge = displayLarge.withShadow(shadow),
        displayMedium = displayMedium.withShadow(shadow),
        displaySmall = displaySmall.withShadow(shadow),
        headlineLarge = headlineLarge.withShadow(shadow),
        headlineMedium = headlineMedium.withShadow(shadow),
        headlineSmall = headlineSmall.withShadow(shadow),
        titleLarge = titleLarge.withShadow(shadow),
        titleMedium = titleMedium.withShadow(shadow),
        titleSmall = titleSmall.withShadow(shadow),
        bodyLarge = bodyLarge.withShadow(shadow),
        bodyMedium = bodyMedium.withShadow(shadow),
        bodySmall = bodySmall.withShadow(shadow),
        labelLarge = labelLarge.withShadow(shadow),
        labelMedium = labelMedium.withShadow(shadow),
        labelSmall = labelSmall.withShadow(shadow)
    )
}
