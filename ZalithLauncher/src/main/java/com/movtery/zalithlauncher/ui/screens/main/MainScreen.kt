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

package com.movtery.zalithlauncher.ui.screens.main

import android.os.Build
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardBackspace
import androidx.compose.material.icons.automirrored.rounded.ArrowLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.movtery.layer_controller.layout.BackdropBlurConfig
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.info.InfoDistributor
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.components.CardTitleLayout
import com.movtery.zalithlauncher.ui.components.LauncherBackdropLayer
import com.movtery.zalithlauncher.ui.components.LocalLauncherBackdropActive
import com.movtery.zalithlauncher.ui.components.LocalLauncherBackdropBlurConfig
import com.movtery.zalithlauncher.ui.components.LocalLauncherBackdropWhiteOutline
import com.movtery.zalithlauncher.ui.components.LocalLauncherSafeAreaPadding
import com.movtery.zalithlauncher.ui.components.TextRailItem
import com.movtery.zalithlauncher.ui.components.itemLayoutColor
import com.movtery.zalithlauncher.ui.components.itemLayoutShadowElevation
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.AccountManageScreen
import com.movtery.zalithlauncher.ui.screens.content.DownloadScreen
import com.movtery.zalithlauncher.ui.screens.content.FileSelectorScreen
import com.movtery.zalithlauncher.ui.screens.content.LauncherScreen
import com.movtery.zalithlauncher.ui.screens.content.LicenseScreen
import com.movtery.zalithlauncher.ui.screens.content.MultiplayerScreen
import com.movtery.zalithlauncher.ui.screens.content.SettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.VersionExportScreen
import com.movtery.zalithlauncher.ui.screens.content.VersionSettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.VersionsManageScreen
import com.movtery.zalithlauncher.ui.screens.content.WebViewScreen
import com.movtery.zalithlauncher.ui.screens.content.navigateToDownload
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.ui.screens.onBack
import com.movtery.zalithlauncher.ui.screens.rememberTransitionSpec
import com.movtery.zalithlauncher.ui.theme.readableShadowFor
import com.movtery.zalithlauncher.ui.theme.withReadableShadow
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.LaunchGameViewModel
import com.movtery.zalithlauncher.viewmodel.LocalBackgroundViewModel
import com.movtery.zalithlauncher.viewmodel.ModpackImportViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import com.movtery.zalithlauncher.viewmodel.sendKeepScreen

@Composable
fun MainScreen(
    screenBackStackModel: ScreenBackStackViewModel,
    launchGameViewModel: LaunchGameViewModel,
    eventViewModel: EventViewModel,
    modpackImportViewModel: ModpackImportViewModel,
    launcherBackdropConfig: BackdropBlurConfig?,
    launcherBackdropEnabled: Boolean,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val tasks by TaskSystem.tasksFlow.collectAsStateWithLifecycle()

    //监控当前是否有任务正在进行
    LaunchedEffect(tasks) {
        if (tasks.isEmpty()) {
            eventViewModel.sendKeepScreen(false)
        } else {
            //有任务正在进行，避免熄屏
            eventViewModel.sendKeepScreen(true)
        }
    }

    val isTaskMenuExpanded = AllSettings.launcherTaskMenuExpanded.state

    fun changeTasksExpandedState() {
        AllSettings.launcherTaskMenuExpanded.save(!isTaskMenuExpanded)
    }

    /** 回到主页面通用函数 */
    val toMainScreen: () -> Unit = {
        screenBackStackModel.mainScreen.clearWith(NormalNavKey.LauncherMain)
    }

    val mainScreenKey = screenBackStackModel.mainScreen.currentKey
    val inLauncherScreen = mainScreenKey == null || mainScreenKey is NormalNavKey.LauncherMain
    val isBackgroundValid = LocalBackgroundViewModel.current?.isValid == true
    val configuredBackgroundOpacityFactor = AllSettings.launcherBackgroundOpacity.state.toFloat() / 100f
    val effectiveBackdropConfig = launcherBackdropConfig.takeIf {
        launcherBackdropEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
    val homeBackdropActive = inLauncherScreen && effectiveBackdropConfig != null
    val nonHomeBackgroundBlurActive = !inLauncherScreen && effectiveBackdropConfig != null
    val topBarColor = MaterialTheme.colorScheme.surfaceContainer
    val backgroundColor = MaterialTheme.colorScheme.surface
    val safeAreaPadding = LocalLauncherSafeAreaPadding.current
    val topSafeAreaPadding = safeAreaPadding.top

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (isBackgroundValid) {
            backgroundColor.copy(
                alpha = if (homeBackdropActive) 0f else configuredBackgroundOpacityFactor
            )
        } else {
            backgroundColor
        },
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScopedLauncherEffects(
                blurConfig = effectiveBackdropConfig,
                whiteOutline = false,
                forceBackgroundOpacityZero = false,
                applyReadableTextShadow = effectiveBackdropConfig != null
            ) {
                TopBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp + topSafeAreaPadding)
                        .zIndex(10f),
                    mainScreenKey = mainScreenKey,
                    taskRunning = tasks.isEmpty(),
                    isTasksExpanded = isTaskMenuExpanded,
                    topSafeAreaPadding = topSafeAreaPadding,
                    startSafeAreaPadding = safeAreaPadding.start,
                    endSafeAreaPadding = safeAreaPadding.end,
                    color = if (isBackgroundValid) {
                        topBarColor.copy(alpha = configuredBackgroundOpacityFactor)
                    } else {
                        topBarColor
                    },
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onScreenBack = {
                        screenBackStackModel.mainScreen.backStack.removeFirstOrNull()
                    },
                    toMainScreen = toMainScreen,
                    toSettingsScreen = {
                        screenBackStackModel.mainScreen.removeAndNavigateTo(
                            removes = screenBackStackModel.clearBeforeNavKeys,
                            screenKey = screenBackStackModel.settingsScreen
                        )
                    },
                    toDownloadScreen = {
                        screenBackStackModel.navigateToDownload()
                    },
                    toMultiplayerScreen = {
                        screenBackStackModel.mainScreen.removeAndNavigateTo(
                            removes = screenBackStackModel.clearBeforeNavKeys,
                            screenKey = NormalNavKey.Multiplayer
                        )
                    }
                ) {
                    changeTasksExpandedState()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (nonHomeBackgroundBlurActive) {
                    ScopedLauncherEffects(
                        blurConfig = effectiveBackdropConfig,
                        whiteOutline = false,
                        forceBackgroundOpacityZero = false,
                        applyReadableTextShadow = false
                    ) {
                        LauncherBackdropLayer(
                            modifier = Modifier.fillMaxSize(),
                            shape = RectangleShape,
                            drawWhiteOutline = false
                        ) {}
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = safeAreaPadding.start,
                            end = safeAreaPadding.end,
                            bottom = safeAreaPadding.bottom
                        )
                ) {
                    ScopedLauncherEffects(
                        blurConfig = if (inLauncherScreen) effectiveBackdropConfig else null,
                        whiteOutline = false,
                        forceBackgroundOpacityZero = homeBackdropActive,
                        applyReadableTextShadow = homeBackdropActive
                    ) {
                        NavigationUI(
                            modifier = Modifier.fillMaxSize(),
                            screenBackStackModel = screenBackStackModel,
                            toMainScreen = toMainScreen,
                            launchGameViewModel = launchGameViewModel,
                            eventViewModel = eventViewModel,
                            modpackImportViewModel = modpackImportViewModel,
                            submitError = submitError
                        )
                    }

                    TaskMenu(
                        tasks = tasks,
                        isExpanded = isTaskMenuExpanded,
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.3f)
                            .align(Alignment.CenterStart)
                            .padding(all = 6.dp)
                    ) {
                        changeTasksExpandedState()
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    mainScreenKey: NavKey?,
    taskRunning: Boolean,
    isTasksExpanded: Boolean,
    topSafeAreaPadding: Dp,
    startSafeAreaPadding: Dp,
    endSafeAreaPadding: Dp,
    modifier: Modifier = Modifier,
    color: Color,
    contentColor: Color,
    onScreenBack: () -> Unit,
    toMainScreen: () -> Unit,
    toSettingsScreen: () -> Unit,
    toDownloadScreen: () -> Unit,
    toMultiplayerScreen: () -> Unit,
    changeExpandedState: () -> Unit = {}
) {
    val inLauncherScreen = mainScreenKey == null || mainScreenKey is NormalNavKey.LauncherMain
    val inMultiplayerScreen = mainScreenKey is NormalNavKey.Multiplayer
    val inDownloadScreen = mainScreenKey is NestedNavKey.Download
    val inSettingsScreen = mainScreenKey is NestedNavKey.Settings

    val drawWhiteOutline = LocalLauncherBackdropWhiteOutline.current
    val logoTextVisible = AllSettings.launcherTopBarLogoTextVisible.state
    val logoColor = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier,
        color = color,
        contentColor = contentColor,
        tonalElevation = 3.dp
    ) {
        LauncherBackdropLayer(
            modifier = Modifier.fillMaxSize(),
            shape = RectangleShape,
            drawWhiteOutline = false
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                ConstraintLayout(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = startSafeAreaPadding,
                            top = topSafeAreaPadding,
                            end = endSafeAreaPadding
                        )
                ) {
                    val (backCenter, title, endButtons) = createRefs()

                    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

                    Row(
                        modifier = Modifier
                            .constrainAs(backCenter) {
                                start.linkTo(parent.start)
                                top.linkTo(parent.top)
                                bottom.linkTo(parent.bottom)
                            }
                            .fillMaxHeight()
                    ) {
                        AnimatedVisibility(
                            visible = !inLauncherScreen
                        ) {
                            Row(modifier = Modifier.fillMaxHeight()) {
                                Spacer(Modifier.width(12.dp))

                                IconButton(
                                    modifier = Modifier.fillMaxHeight(),
                                    onClick = {
                                        if (!inLauncherScreen) {
                                            //不在主屏幕时才允许返回
                                            backDispatcher?.onBackPressed() ?: run {
                                                onScreenBack()
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        modifier = Modifier.size(24.dp),
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardBackspace,
                                        contentDescription = stringResource(R.string.generic_back)
                                    )
                                }

                                IconButton(
                                    modifier = Modifier.fillMaxHeight(),
                                    onClick = {
                                        if (!inLauncherScreen) {
                                            //不在主屏幕时才允许回到主页面
                                            toMainScreen()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Home,
                                        contentDescription = stringResource(R.string.generic_main_menu)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .constrainAs(endButtons) {
                                top.linkTo(parent.top)
                                bottom.linkTo(parent.bottom)
                                end.linkTo(parent.end, margin = 12.dp)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimatedVisibility(
                            visible = !(isTasksExpanded || taskRunning),
                            enter = slideInVertically(
                                initialOffsetY = { -50 }
                            ) + fadeIn(),
                            exit = slideOutVertically(
                                targetOffsetY = { -50 }
                            ) + fadeOut()
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(shape = MaterialTheme.shapes.large)
                                    .clickable { changeExpandedState() }
                                    .padding(all = 8.dp)
                                    .width(120.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LinearProgressIndicator(modifier = Modifier.weight(1f))
                                Icon(
                                    modifier = Modifier.size(22.dp),
                                    imageVector = Icons.Filled.Task,
                                    contentDescription = stringResource(R.string.main_task_menu)
                                )
                            }
                        }

                        TopBarRailItem(
                            selected = inMultiplayerScreen,
                            icon = Icons.Filled.Group,
                            text = stringResource(R.string.terracotta),
                            onClick = {
                                if (!inMultiplayerScreen) toMultiplayerScreen()
                            },
                            color = contentColor
                        )

                        TopBarRailItem(
                            selected = inDownloadScreen,
                            icon = Icons.Filled.Download,
                            text = stringResource(R.string.generic_download),
                            onClick = {
                                if (!inDownloadScreen) toDownloadScreen()
                            },
                            color = contentColor
                        )

                        TopBarRailItem(
                            selected = inSettingsScreen,
                            icon = Icons.Filled.Settings,
                            text = stringResource(R.string.generic_setting),
                            onClick = {
                                if (!inSettingsScreen) toSettingsScreen()
                            },
                            color = contentColor
                        )
                    }

                    Row(
                        modifier = Modifier
                            .constrainAs(title) {
                                centerVerticallyTo(parent)
                                start.linkTo(backCenter.end, margin = 16.dp)
                                end.linkTo(endButtons.start, margin = 12.dp)
                                width = Dimension.fillToConstraints
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(shape = MaterialTheme.shapes.large)
                                .clickable {
                                    AllSettings.launcherTopBarLogoTextVisible.save(!logoTextVisible)
                                },
                            painter = painterResource(R.drawable.img_launcher),
                            contentDescription = InfoDistributor.LAUNCHER_IDENTIFIER,
                            tint = Color.Unspecified
                        )
                        AnimatedVisibility(
                            visible = logoTextVisible,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .clip(shape = MaterialTheme.shapes.large)
                                    .clickable {
                                        AllSettings.launcherTopBarLogoTextVisible.save(!logoTextVisible)
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = InfoDistributor.LAUNCHER_IDENTIFIER,
                                    color = logoColor
                                )

                            }
                        }
                    }
                }

                if (drawWhiteOutline) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.95f))
                    )
                }
            }
        }
    }
}

@Composable
private fun ScopedLauncherEffects(
    blurConfig: BackdropBlurConfig?,
    whiteOutline: Boolean,
    forceBackgroundOpacityZero: Boolean,
    applyReadableTextShadow: Boolean,
    content: @Composable () -> Unit
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val textShadow = remember(applyReadableTextShadow, onSurface) {
        if (!applyReadableTextShadow || blurConfig == null) {
            null
        } else {
            readableShadowFor(onSurface)
        }
    }
    val baseTypography = MaterialTheme.typography
    val readableTypography = remember(baseTypography, textShadow) {
        if (textShadow == null) {
            baseTypography
        } else {
            baseTypography.withReadableShadow(textShadow)
        }
    }
    val baseTextStyle = LocalTextStyle.current
    val scopedTextStyle = remember(baseTextStyle, textShadow) {
        if (textShadow == null) {
            baseTextStyle
        } else {
            baseTextStyle.merge(TextStyle(shadow = textShadow))
        }
    }

    CompositionLocalProvider(
        LocalLauncherBackdropBlurConfig provides blurConfig,
        LocalLauncherBackdropWhiteOutline provides whiteOutline,
        LocalLauncherBackdropActive provides forceBackgroundOpacityZero
    ) {
        if (textShadow == null) {
            content()
        } else {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme,
                typography = readableTypography,
                shapes = MaterialTheme.shapes
            ) {
                CompositionLocalProvider(LocalTextStyle provides scopedTextStyle) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun TopBarRailItem(
    selected: Boolean,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    color: Color = MaterialTheme.colorScheme.onSurface,
    textStyle: TextStyle = MaterialTheme.typography.labelMedium
) {
    TextRailItem(
        modifier = modifier,
        onClick = onClick,
        text = {
            AnimatedVisibility(visible = selected) {
                Row {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = text,
                        style = textStyle
                    )
                }
            }
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = text
            )
        },
        selected = selected,
        selectedPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        unSelectedPadding = PaddingValues(all = 8.dp),
        unselectedContentColor = color
    )
}

@Composable
private fun NavigationUI(
    modifier: Modifier = Modifier,
    screenBackStackModel: ScreenBackStackViewModel,
    toMainScreen: () -> Unit,
    launchGameViewModel: LaunchGameViewModel,
    eventViewModel: EventViewModel,
    modpackImportViewModel: ModpackImportViewModel,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val backStack = screenBackStackModel.mainScreen.backStack
    val currentKey = backStack.lastOrNull()

    LaunchedEffect(currentKey) {
        screenBackStackModel.mainScreen.currentKey = currentKey
    }

    if (backStack.isNotEmpty()) {
        /** 导航至版本详细信息屏幕 */
        val navigateToVersions: (Version) -> Unit = { version ->
            screenBackStackModel.mainScreen.navigateTo(
                screenKey = NestedNavKey.VersionSettings(version),
                useClassEquality = true
            )
        }
        /** 导航至整合包导出屏幕 */
        val navigateToExport: (Version) -> Unit = { version ->
            screenBackStackModel.mainScreen.removeAndNavigateTo(
                remove = NestedNavKey.VersionSettings::class,
                screenKey = NestedNavKey.VersionExport(version),
                useClassEquality = true
            )
        }

        NavDisplay(
            backStack = backStack,
            modifier = modifier,
            onBack = {
                onBack(backStack)
            },
            transitionSpec = rememberTransitionSpec(),
            popTransitionSpec = rememberTransitionSpec(),
            entryProvider = entryProvider {
                entry<NormalNavKey.LauncherMain> {
                    LauncherScreen(
                        backStackViewModel = screenBackStackModel,
                        navigateToVersions = navigateToVersions,
                        launchGameViewModel = launchGameViewModel
                    )
                }
                entry<NestedNavKey.Settings> { key ->
                    SettingsScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        openLicenseScreen = { raw ->
                            backStack.navigateTo(NormalNavKey.License(raw))
                        },
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.License> { key ->
                    LicenseScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel
                    )
                }
                entry<NormalNavKey.AccountManager> {
                    AccountManageScreen(
                        backStackViewModel = screenBackStackModel,
                        backToMainScreen = toMainScreen,
                        openLink = { url ->
                            eventViewModel.sendEvent(EventViewModel.Event.OpenLink(url))
                        },
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.WebScreen> { key ->
                    WebViewScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel
                    )
                }
                entry<NormalNavKey.VersionsManager> {
                    VersionsManageScreen(
                        backScreenViewModel = screenBackStackModel,
                        navigateToVersions = navigateToVersions,
                        navigateToExport = navigateToExport,
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.FileSelector> { key ->
                    FileSelectorScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel
                    ) {
                        backStack.removeLastOrNull()
                    }
                }
                entry<NestedNavKey.VersionSettings> { key ->
                    VersionSettingsScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel,
                        backToMainScreen = toMainScreen,
                        onExportModpack = {
                            navigateToExport(key.version)
                        },
                        launchGameViewModel = launchGameViewModel,
                        eventViewModel = eventViewModel,
                        submitError = submitError
                    )
                }
                entry<NestedNavKey.VersionExport> { key ->
                    VersionExportScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel,
                        backToMainScreen = toMainScreen
                    )
                }
                entry<NestedNavKey.Download> { key ->
                    DownloadScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel,
                        modpackImportViewModel = modpackImportViewModel,
                        submitError = submitError
                    )
                }
                entry<NormalNavKey.Multiplayer> {
                    MultiplayerScreen(
                        backScreenViewModel = screenBackStackModel,
                        eventViewModel = eventViewModel
                    )
                }
            }
        )
    } else {
        Box(modifier)
    }
}

@Composable
private fun TaskMenu(
    tasks: List<Task>,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    changeExpandedState: () -> Unit = {}
) {
    val show = isExpanded && tasks.isNotEmpty()

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    AnimatedVisibility(
        modifier = modifier,
        enter = slideInHorizontally(
            initialOffsetX = { if (isRtl) it else -it },
            animationSpec = getAnimateTween()
        ) + fadeIn(),
        exit = slideOutHorizontally(
            targetOffsetX = { if (isRtl) it else -it },
            animationSpec = getAnimateTween()
        ) + fadeOut(),
        visible = show
    ) {
        BackgroundCard(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 6.dp),
            influencedByBackground = false,
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
        ) {
            Column {
                CardTitleLayout {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(top = 8.dp, bottom = 4.dp)
                    ) {
                        IconButton(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.CenterStart),
                            onClick = changeExpandedState
                        ) {
                            Icon(
                                modifier = Modifier.size(28.dp),
                                imageVector = Icons.AutoMirrored.Rounded.ArrowLeft,
                                contentDescription = stringResource(R.string.generic_collapse)
                            )
                        }

                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = stringResource(R.string.main_task_menu)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    items(tasks) { task ->
                        TaskItem(
                            taskProgress = task.currentProgress,
                            taskMessageRes = task.currentMessageRes,
                            taskMessageArgs = task.currentMessageArgs,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            //取消任务
                            TaskSystem.cancelTask(task.id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItem(
    taskProgress: Float,
    taskMessageRes: Int?,
    taskMessageArgs: Array<out Any>?,
    modifier: Modifier = Modifier,
    influencedByBackground: Boolean = false,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = itemLayoutColor(influencedByBackground = influencedByBackground),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shadowElevation: Dp = itemLayoutShadowElevation(influencedByBackground = influencedByBackground),
    onCancelClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
        shadowElevation = shadowElevation
    ) {
        Row(
            modifier = Modifier.padding(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterVertically),
                onClick = onCancelClick
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.generic_cancel)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
                    .animateContentSize(animationSpec = getAnimateTween())
            ) {
                taskMessageRes?.let { messageRes ->
                    Text(
                        text = if (taskMessageArgs != null) {
                            stringResource(messageRes, *taskMessageArgs)
                        } else {
                            stringResource(messageRes)
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                if (taskProgress < 0) { //负数则代表不确定
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { taskProgress },
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterVertically)
                        )
                        Text(
                            text = "${(taskProgress * 100).toInt()}%",
                            modifier = Modifier.align(Alignment.CenterVertically),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
