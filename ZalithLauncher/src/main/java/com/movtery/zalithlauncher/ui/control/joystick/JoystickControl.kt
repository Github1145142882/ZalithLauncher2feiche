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

package com.movtery.zalithlauncher.ui.control.joystick

import androidx.annotation.FloatRange
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.movtery.layer_controller.data.DefaultJoystickStyle
import com.movtery.layer_controller.data.loadFromFile
import com.movtery.layer_controller.data.saveToFile
import com.movtery.layer_controller.observable.ObservableJoystickStyle
import com.movtery.zalithlauncher.setting.enums.isLauncherInDarkTheme
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.atan2
import kotlin.onFailure

private const val JSON_FILE_NAME = "joystick.json"

/**
 * 加载启动器默认的摇杆样式
 */
suspend fun loadJoystickStyle(
    path: File
): ObservableJoystickStyle {
    val styleFile = File(path, JSON_FILE_NAME)
    val style = if (styleFile.exists()) {
        runCatching {
            withContext(Dispatchers.IO) {
                loadFromFile(styleFile)
            }
        }.onFailure {
            lWarning("Failed to load joystick from file: $styleFile", it)
        }.getOrNull()
    } else null

    return ObservableJoystickStyle(style ?: DefaultJoystickStyle)
}

/**
 * 保存启动器默认的摇杆样式
 */
suspend fun saveJoystickStyle(
    path: File,
    style: ObservableJoystickStyle,
    onFailed: (Throwable) -> Unit,
    onSuccess: suspend () -> Unit = {}
) {
    val styleFile = File(path, JSON_FILE_NAME)
    runCatching {
        withContext(Dispatchers.IO) {
            saveToFile(style.pack(), styleFile)
        }
    }.onFailure {
        onFailed(it)
    }.onSuccess {
        onSuccess()
    }
}

/**
 * 根据可观察的摇杆样式对象一键设置样式的移动摇杆控件
 * @param isDarkTheme 是否处于暗色模式中，用于选择样式
 * @param style 可观察的摇杆样式
 * @param size 组件整体大小
 * @param deadZoneRatio 死区范围，作为半分比，根据组件整体大小计算
 * @param lockThreshold 前进锁判定范围（在组件的外部，正上方），作为百分比，根据组件整体大小计算
 * @param onDirectionChanged 当摇杆的方向变更时，使用这个函数回调
 * @param canLock 是否可以进行前进锁
 * @param onLock 当摇杆触发前进锁，或者离开锁定状态时，使用这个函数回调
 */
@Composable
fun StyleableJoystick(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isLauncherInDarkTheme(),
    style: ObservableJoystickStyle,
    size: Dp = 120.dp,
    @FloatRange(from = 0.0, to = 1.0)
    deadZoneRatio: Float = 0.5f,
    @FloatRange(from = 0.0, to = 1.0)
    lockThreshold: Float = 0.3f,
    onDirectionChanged: (JoystickDirection) -> Unit = {},
    canLock: Boolean = true,
    onLock: (Boolean) -> Unit = {}
) {
    val theme = if (isDarkTheme) style.darkStyle else style.lightStyle

    val backgroundShape = remember(theme.backgroundShape) {
        RoundedCornerShape(percent = theme.backgroundShape)
    }

    val joystickShape = remember(theme.joystickShape) {
        RoundedCornerShape(percent = theme.joystickShape)
    }

    val borderWidthRatio = remember(theme.borderWidthRatio) {
        (theme.borderWidthRatio.toFloat() / 100f).coerceIn(0.0f, 0.5f)
    }

    Joystick(
        modifier = modifier,
        alpha = theme.alpha,
        backgroundColor = theme.backgroundColor,
        joystickColor = theme.joystickColor,
        joystickCanLockColor = theme.joystickCanLockColor,
        joystickLockedColor = theme.joystickLockedColor,
        lockMarkColor = theme.lockMarkColor,
        borderColor = theme.borderColor,
        borderWidthRatio = borderWidthRatio,
        backgroundShape = backgroundShape,
        joystickShape = joystickShape,
        size = size,
        joystickSize = theme.joystickSize,
        deadZoneRatio = deadZoneRatio,
        lockThreshold = lockThreshold,
        onDirectionChanged = onDirectionChanged,
        canLock = canLock,
        onLock = onLock
    )
}

/**
 * 移动摇杆控件
 * @param alpha 整体不透明度
 * @param backgroundColor 控件的背景层颜色
 * @param joystickColor 摇杆的颜色
 * @param joystickCanLockColor 摇杆移动到可以锁定的位置时，摇杆的颜色
 * @param joystickLockedColor 摇杆锁定时的颜色
 * @param lockMarkColor 锁定标记的颜色
 * @param backgroundShape 背景层的形状
 * @param joystickShape 摇杆的形状
 * @param size 组件整体大小
 * @param joystickSize 摇杆大小，作为百分比，根据组件整体大小计算
 * @param deadZoneRatio 死区范围，作为半分比，根据组件整体大小计算
 * @param lockThreshold 前进锁判定范围（在组件的外部，正上方），作为百分比，根据组件整体大小计算
 * @param onDirectionChanged 当摇杆的方向变更时，使用这个函数回调
 * @param canLock 是否可以进行前进锁
 * @param onLock 当摇杆触发前进锁，或者离开锁定状态时，使用这个函数回调
 */
@Composable
fun Joystick(
    modifier: Modifier = Modifier,
    @FloatRange(from = 0.0, to = 1.0)
    alpha: Float = 1.0f,
    backgroundColor: Color = Color.Black.copy(alpha = 0.5f),
    joystickColor: Color = Color.White.copy(alpha = 0.5f),
    joystickCanLockColor: Color = Color.Yellow.copy(alpha = 0.5f),
    joystickLockedColor: Color = Color.Green.copy(alpha = 0.5f),
    lockMarkColor: Color = Color.White,
    borderColor: Color = Color.White,
    @FloatRange(from = 0.0, to = 0.5)
    borderWidthRatio: Float = 0f,
    backgroundShape: Shape = CircleShape,
    joystickShape: Shape = CircleShape,
    size: Dp = 120.dp,
    @FloatRange(from = 0.0, to = 1.0)
    joystickSize: Float = 0.5f,
    @FloatRange(from = 0.0, to = 1.0)
    deadZoneRatio: Float = 0.5f,
    @FloatRange(from = 0.0, to = 1.0)
    lockThreshold: Float = 0.3f,
    onDirectionChanged: (JoystickDirection) -> Unit = {},
    canLock: Boolean = true,
    onLock: (Boolean) -> Unit = {}
) {
    //使用这个标记来判断是否渲染摇杆组件，未完全初始化时，可能导致组件闪烁
    var initialized by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    var joystickPosition by remember { mutableStateOf(Offset.Zero) }
    var isLocked by remember { mutableStateOf(false) }
    var lastDragPosition by remember { mutableStateOf(Offset.Zero) }

    //已经经过验证，如果使用Modifier.alpha设置不透明度，会导致摇杆强制裁切超出范围的内容
    //graphicsLayer(alpha = alpha, clip = false)也一样
    //这里暂时只能统一修改颜色的alpha
    val currentBackgroundColor = remember(backgroundColor, alpha) {
        backgroundColor.applyAlpha(alpha)
    }
    val currentJoystickColor = remember(joystickColor, alpha) {
        joystickColor.applyAlpha(alpha)
    }
    val currentJoystickCanLockColor = remember(joystickCanLockColor, alpha) {
        joystickCanLockColor.applyAlpha(alpha)
    }
    val currentJoystickLockedColor = remember(joystickLockedColor, alpha) {
        joystickLockedColor.applyAlpha(alpha)
    }
    val currentLockMarkColor = remember(lockMarkColor, alpha) {
        lockMarkColor.applyAlpha(alpha)
    }
    val currentBorderColor = remember(borderColor, alpha) {
        borderColor.applyAlpha(alpha)
    }
    val currentCanLock by rememberUpdatedState(canLock)

    //计算尺寸
    val backgroundSizePx = remember(size) {
        with(density) { size.toPx() }
    }
    val currentBackgroundSizePx by rememberUpdatedState(backgroundSizePx)

    //计算摇杆大小 Px
    val joystickSizePx = remember(backgroundSizePx, joystickSize) {
        backgroundSizePx * joystickSize.coerceIn(0.0f, 1.0f)
    }
    val currentJoystickSizePx by rememberUpdatedState(joystickSizePx)

    //组件中心点
    val centerPoint = remember(backgroundSizePx) {
        Offset(backgroundSizePx / 2, backgroundSizePx / 2)
    }
    val currentCenterPoint by rememberUpdatedState(centerPoint)

    //死区半径
    val deadZoneRadius = remember(backgroundSizePx, deadZoneRatio) {
        backgroundSizePx * deadZoneRatio / 2
    }
    val currentDeadZoneRadius by rememberUpdatedState(deadZoneRadius)

    //前进锁触发距离
    val lockThresholdPx = remember(backgroundSizePx, lockThreshold) {
        backgroundSizePx * lockThreshold
    }
    val currentLockThresholdPx by rememberUpdatedState(lockThresholdPx)

    //前进锁定时，摇杆锁定的位置
    val lockPosition = remember(centerPoint) {
        Offset(centerPoint.x, 0f)
    }
    val currentLockPosition by rememberUpdatedState(lockPosition)

    var internalCanLock by remember { mutableStateOf(false) }
    var direction by remember { mutableStateOf(JoystickDirection.None) }

    fun updateJoystickState(position: Offset = currentCenterPoint) {
        val clampedPosition = updateJoystickPosition(
            newPosition = position,
            minX = 0f,
            maxX = currentBackgroundSizePx,
            minY = 0f,
            maxY = currentBackgroundSizePx
        )

        joystickPosition = clampedPosition

        direction = calculateDirection(
            joystickPosition = clampedPosition,
            backgroundCenter = currentCenterPoint,
            deadZoneRadius = currentDeadZoneRadius
        )

        internalCanLock =
            currentCanLock &&
            direction == JoystickDirection.North &&
            lastDragPosition.y < -currentLockThresholdPx
    }

    //进行初始化

    LaunchedEffect(size) {
        initialized = false
        updateJoystickState()
        initialized = true
    }

    //状态更新回调

    LaunchedEffect(direction) {
        onDirectionChanged(direction)
    }
    LaunchedEffect(isLocked) {
        onLock(isLocked)
    }
    DisposableEffect(Unit) {
        onDispose {
            onDirectionChanged(JoystickDirection.None)
            initialized = false
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        lastDragPosition = offset
                        if (isLocked) isLocked = false
                        updateJoystickState(offset)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        lastDragPosition = change.position
                        if (isLocked) isLocked = false
                        updateJoystickState(change.position)
                    },
                    onDragEnd = {
                        if (internalCanLock) {
                            isLocked = true
                            updateJoystickState(currentLockPosition)
                        } else {
                            isLocked = false
                            updateJoystickState(currentCenterPoint)
                        }
                    }
                )
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
        ) {
            if (initialized) {
                val minSide = minOf(this@Canvas.size.width, this@Canvas.size.height)

                //背景层
                drawBackgroundLayer(
                    layoutDirection = layoutDirection,
                    size = this@Canvas.size,
                    shape = backgroundShape,
                    backgroundColor = currentBackgroundColor,
                    borderColor = currentBorderColor,
                    borderWidthPx = (minSide * borderWidthRatio).coerceAtLeast(0f)
                )

                //摇杆层
                drawJoystick(
                    layoutDirection = layoutDirection,
                    color = when {
                        isLocked -> currentJoystickLockedColor
                        internalCanLock -> currentJoystickCanLockColor
                        else -> currentJoystickColor
                    },
                    center = joystickPosition,
                    size = currentJoystickSizePx,
                    shape = joystickShape
                )

                //锁定标记
                if (isLocked) {
                    drawCircle(
                        color = currentLockMarkColor,
                        center = currentLockPosition,
                        radius = 4f
                    )
                }
            }
        }
    }
}

/**
 * 绘制背景层
 */
private fun DrawScope.drawBackgroundLayer(
    layoutDirection: LayoutDirection,
    size: Size,
    shape: Shape,
    backgroundColor: Color,
    borderColor: Color,
    borderWidthPx: Float
) {
    val outline = shape.createOutline(
        size = size,
        layoutDirection = layoutDirection,
        density = this
    )

    val clipPath = when (outline) {
        is Outline.Generic -> outline.path
        is Outline.Rounded -> Path().apply {
            addRoundRect(outline.roundRect)
        }
        is Outline.Rectangle -> Path().apply {
            addRect(outline.rect)
        }
    }

    clipPath(clipPath) {
        drawOutline(
            outline = outline,
            color = backgroundColor
        )

        if (borderWidthPx > 0f) {
            drawOutline(
                outline = outline,
                color = borderColor,
                style = Stroke(width = borderWidthPx)
            )
        }
    }
}

/**
 * 绘制摇杆层
 */
private fun DrawScope.drawJoystick(
    layoutDirection: LayoutDirection,
    color: Color,
    center: Offset,
    size: Float,
    shape: Shape
) {
    val halfSize = size / 2
    val topLeftX = center.x - halfSize
    val topLeftY = center.y - halfSize

    val outline = shape.createOutline(
        size = Size(size, size),
        layoutDirection = layoutDirection,
        density = this
    )

    translate(
        left = topLeftX,
        top = topLeftY
    ) {
        drawOutline(
            outline = outline,
            color = color
        )
    }
}

private fun updateJoystickPosition(
    newPosition: Offset,
    minX: Float,
    maxX: Float,
    minY: Float,
    maxY: Float
): Offset {
    //限制中心点在边界内
    val clampedX = newPosition.x.coerceIn(minX, maxX)
    val clampedY = newPosition.y.coerceIn(minY, maxY)

    return Offset(clampedX, clampedY)
}

/**
 * 计算摇杆方向
 */
private fun calculateDirection(
    joystickPosition: Offset,
    backgroundCenter: Offset,
    deadZoneRadius: Float
): JoystickDirection {
    if (joystickPosition == backgroundCenter) {
        return JoystickDirection.None
    }

    val vector = joystickPosition - backgroundCenter
    val distance = kotlin.math.sqrt(vector.x * vector.x + vector.y * vector.y)

    //如果距离小于死区半径，认为是无方向
    if (distance < deadZoneRadius) {
        return JoystickDirection.None
    }

    val angle = Math.toDegrees(atan2(vector.y.toDouble(), vector.x.toDouble())).toFloat()

    return when {
        angle >= -22.5f && angle < 22.5f -> JoystickDirection.East
        angle in 22.5f..<67.5f -> JoystickDirection.SouthEast
        angle in 67.5f..<112.5f -> JoystickDirection.South
        angle in 112.5f..<157.5f -> JoystickDirection.SouthWest
        angle >= 157.5f || angle < -157.5f -> JoystickDirection.West
        angle >= -157.5f && angle < -112.5f -> JoystickDirection.NorthWest
        angle >= -112.5f && angle < -67.5f -> JoystickDirection.North
        angle >= -67.5f && angle < -22.5f -> JoystickDirection.NorthEast
        else -> JoystickDirection.None
    }
}

private fun Color.applyAlpha(multiplier: Float): Color {
    return copy(alpha = this.alpha * multiplier)
}