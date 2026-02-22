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

package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.VideoSettings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.components.LauncherBackdropSurface
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.ui.components.ScalingActionButton
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.AccountAvatar
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionIconImage
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.viewmodel.LaunchGameViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel

@Composable
fun LauncherScreen(
    backStackViewModel: ScreenBackStackViewModel,
    navigateToVersions: (Version) -> Unit,
    launchGameViewModel: LaunchGameViewModel
) {
    val showHomeLeftFeatureCard = AllSettings.launcherShowHomeLeftFeatureCard.state

    BaseScreen(
        screenKey = NormalNavKey.LauncherMain,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) { isVisible ->
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            ContentMenu(
                isVisible = isVisible,
                modifier = Modifier
                    .weight(7f)
                    .fillMaxHeight()
                    .padding(start = 12.dp, top = 12.dp, end = 6.dp, bottom = 12.dp),
                show = showHomeLeftFeatureCard,
                backStackViewModel = backStackViewModel,
                navigateToVersions = navigateToVersions
            )

            RightMenu(
                isVisible = isVisible,
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight()
                    .padding(start = 6.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
                launchGameViewModel = launchGameViewModel,
                toAccountManageScreen = {
                    backStackViewModel.mainScreen.navigateTo(NormalNavKey.AccountManager)
                },
                toVersionManageScreen = {
                    backStackViewModel.mainScreen.removeAndNavigateTo(
                        remove = NestedNavKey.VersionSettings::class,
                        screenKey = NormalNavKey.VersionsManager
                    )
                },
                toVersionSettingsScreen = {
                    VersionsManager.currentVersion.value?.let { version ->
                        navigateToVersions(version)
                    }
                },
                toGameSettingsScreen = {
                    backStackViewModel.settingsScreen.clearWith(NormalNavKey.Settings.Game)
                    backStackViewModel.mainScreen.removeAndNavigateTo(
                        removes = backStackViewModel.clearBeforeNavKeys,
                        screenKey = backStackViewModel.settingsScreen
                    )
                },
                toJavaManageScreen = {
                    backStackViewModel.settingsScreen.clearWith(NormalNavKey.Settings.JavaManager)
                    backStackViewModel.mainScreen.removeAndNavigateTo(
                        removes = backStackViewModel.clearBeforeNavKeys,
                        screenKey = backStackViewModel.settingsScreen
                    )
                }
            )
        }
    }
}

@Composable
private fun ContentMenu(
    isVisible: Boolean,
    show: Boolean,
    modifier: Modifier = Modifier,
    backStackViewModel: ScreenBackStackViewModel,
    navigateToVersions: (Version) -> Unit
) {
    val yOffset by swapAnimateDpAsState(
        targetValue = (-40).dp,
        swapIn = isVisible
    )
    val context = LocalContext.current
    val selectedRendererId = AllSettings.renderer.state
    val rendererDisplayName = remember(context, selectedRendererId) {
        runCatching {
            Renderers.init()
            val compatibleRenderers = Renderers.getCompatibleRenderers(context).second
            compatibleRenderers
                .firstOrNull { renderer -> renderer.getUniqueIdentifier() == selectedRendererId }
                ?.getRendererName()
                ?: compatibleRenderers.firstOrNull()?.getRendererName()
                ?: selectedRendererId
        }.getOrElse {
            selectedRendererId
        }.ifBlank {
            context.getString(R.string.generic_unknown)
        }
    }
    val lastPlayedVersionName = AllSettings.launcherLastPlayedVersionName.state
    val recentVersionSummary = lastPlayedVersionName.ifBlank {
        stringResource(R.string.launcher_home_recent_played_empty)
    }
    val totalLaunchCount = AllSettings.finishedGame.state
    val totalPlayMinutes = (AllSettings.launcherTotalPlaySeconds.state / 60L).coerceAtLeast(0L)
    val sustainedPerformanceEnabled = AllSettings.sustainedPerformance.state
    val sustainedPerformanceStatus = if (sustainedPerformanceEnabled) {
        stringResource(R.string.generic_enabled)
    } else {
        stringResource(R.string.generic_disabled)
    }
    val miniCardColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
    val drawWhiteOutline = false

    if (!show) {
        Box(
            modifier = modifier
                .offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
                .fillMaxSize()
        )
        return
    }

    BackgroundCard(
        modifier = modifier
            .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeQuickActionCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                icon = Icons.Outlined.VideoSettings,
                title = stringResource(R.string.launcher_home_renderer_title),
                summary = rendererDisplayName,
                color = miniCardColor,
                drawWhiteOutline = drawWhiteOutline,
                onClick = {
                    backStackViewModel.settingsScreen.clearWith(NormalNavKey.Settings.Renderer)
                    backStackViewModel.mainScreen.removeAndNavigateTo(
                        removes = backStackViewModel.clearBeforeNavKeys,
                        screenKey = backStackViewModel.settingsScreen
                    )
                }
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeQuickActionCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    icon = Icons.Outlined.RocketLaunch,
                    title = stringResource(R.string.launcher_home_recent_played_title),
                    summary = recentVersionSummary,
                    color = miniCardColor,
                    drawWhiteOutline = drawWhiteOutline,
                    onClick = {
                        val recentVersion = lastPlayedVersionName
                            .takeIf { it.isNotBlank() }
                            ?.let { name ->
                                VersionsManager.versions.firstOrNull { version ->
                                    version.getVersionName() == name && version.isValid()
                                }
                            }

                        if (recentVersion != null) {
                            navigateToVersions(recentVersion)
                        } else {
                            backStackViewModel.mainScreen.removeAndNavigateTo(
                                removes = backStackViewModel.clearBeforeNavKeys,
                                screenKey = NormalNavKey.VersionsManager
                            )
                        }
                    }
                )
                HomeQuickActionCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    icon = Icons.Filled.Settings,
                    title = stringResource(R.string.launcher_home_sustained_mode_title),
                    summary = stringResource(
                        R.string.launcher_home_sustained_mode_summary,
                        sustainedPerformanceStatus
                    ),
                    color = miniCardColor,
                    drawWhiteOutline = drawWhiteOutline,
                    onClick = {
                        AllSettings.sustainedPerformance.save(!sustainedPerformanceEnabled)
                    }
                )
            }

            HomeSummaryStrip(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.72f),
                totalLaunchCount = totalLaunchCount,
                totalPlayMinutes = totalPlayMinutes,
                color = miniCardColor,
                drawWhiteOutline = drawWhiteOutline
            )

            if (BuildConfig.DEBUG) {
                Text(
                    modifier = Modifier.alpha(0.7f),
                    text = stringResource(R.string.launcher_version_debug_warning_cant_close),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun HomeQuickActionCard(
    icon: ImageVector,
    title: String,
    summary: String,
    color: androidx.compose.ui.graphics.Color,
    drawWhiteOutline: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LauncherBackdropSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        influencedByBackground = true,
        shape = MaterialTheme.shapes.large,
        color = color,
        drawWhiteOutline = drawWhiteOutline
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(28.dp),
                imageVector = icon,
                contentDescription = title
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun HomeStatsCard(
    totalLaunchCount: Int,
    totalPlayMinutes: Long,
    color: androidx.compose.ui.graphics.Color,
    drawWhiteOutline: Boolean,
    modifier: Modifier = Modifier
) {
    LauncherBackdropSurface(
        modifier = modifier,
        influencedByBackground = true,
        shape = MaterialTheme.shapes.large,
        color = color,
        drawWhiteOutline = drawWhiteOutline
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HomeStatItem(
                title = stringResource(R.string.launcher_home_total_launches_title),
                value = totalLaunchCount.toString()
            )
            HomeStatItem(
                title = stringResource(R.string.launcher_home_total_play_minutes_title),
                value = totalPlayMinutes.toString()
            )
        }
    }
}

@Composable
private fun HomeSummaryStrip(
    totalLaunchCount: Int,
    totalPlayMinutes: Long,
    color: androidx.compose.ui.graphics.Color,
    drawWhiteOutline: Boolean,
    modifier: Modifier = Modifier
) {
    LauncherBackdropSurface(
        modifier = modifier,
        influencedByBackground = true,
        shape = MaterialTheme.shapes.large,
        color = color,
        drawWhiteOutline = drawWhiteOutline
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeSummaryItem(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.launcher_home_total_launches_title),
                value = totalLaunchCount.toString()
            )
            HomeSummaryItem(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.launcher_home_total_play_minutes_title),
                value = totalPlayMinutes.toString()
            )
        }
    }
}

@Composable
private fun HomeSummaryItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
            text = title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1
        )
        Text(
            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
            text = value,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1
        )
    }
}

@Composable
private fun HomeStatItem(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun RightMenu(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    launchGameViewModel: LaunchGameViewModel,
    toAccountManageScreen: () -> Unit = {},
    toVersionManageScreen: () -> Unit = {},
    toVersionSettingsScreen: () -> Unit = {},
    toGameSettingsScreen: () -> Unit = {},
    toJavaManageScreen: () -> Unit = {}
) {
    val xOffset by swapAnimateDpAsState(
        targetValue = 40.dp,
        swapIn = isVisible,
        isHorizontal = true
    )

    BackgroundCard(
        modifier = modifier.offset { IntOffset(x = xOffset.roundToPx(), y = 0) },
        shape = MaterialTheme.shapes.extraLarge
    ) {
        val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()
        val version by VersionsManager.currentVersion.collectAsStateWithLifecycle()
        val isRefreshing by VersionsManager.isRefreshing.collectAsStateWithLifecycle()

        val memoryValue = AllSettings.ramAllocation.state?.let { "${it}M" }
            ?: stringResource(R.string.launcher_home_status_auto)
        val resolutionValue = "${AllSettings.resolutionRatio.state}%"
        val javaValue = AllSettings.javaRuntime.state.ifBlank {
            stringResource(R.string.launcher_home_status_auto)
        }

        ConstraintLayout(
            modifier = Modifier.fillMaxSize()
        ) {
            val (accountAvatar, versionManagerLayout, statusLayout, launchButton) = createRefs()

            AccountAvatar(
                modifier = Modifier
                    .constrainAs(accountAvatar) {
                        top.linkTo(parent.top, margin = 8.dp)
                        bottom.linkTo(versionManagerLayout.top, margin = 12.dp)
                        verticalBias = 0f
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    },
                account = account,
                onClick = toAccountManageScreen
            )

            Row(
                modifier = Modifier
                    .constrainAs(versionManagerLayout) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(statusLayout.top, margin = 12.dp)
                    }
                    .offset(y = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VersionManagerLayout(
                    isRefreshing = isRefreshing,
                    version = version,
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp),
                    swapToVersionManage = toVersionManageScreen
                )
                version?.takeIf { !isRefreshing && it.isValid() }?.let {
                    IconButton(
                        modifier = Modifier.padding(end = 8.dp),
                        onClick = toVersionSettingsScreen
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.versions_manage_settings)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .constrainAs(statusLayout) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(launchButton.top, margin = 12.dp)
                    }
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HomeStatusChip(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.launcher_home_status_memory),
                    value = memoryValue,
                    onClick = toGameSettingsScreen
                )
                HomeStatusChip(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.launcher_home_status_resolution),
                    value = resolutionValue,
                    onClick = toGameSettingsScreen
                )
                HomeStatusChip(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.launcher_home_status_java),
                    value = javaValue,
                    onClick = toJavaManageScreen
                )
            }

            ScalingActionButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .constrainAs(launchButton) {
                        bottom.linkTo(parent.bottom, margin = 12.dp)
                    }
                    .padding(PaddingValues(horizontal = 12.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                border = null,
                onClick = {
                    launchGameViewModel.tryLaunch(
                        VersionsManager.currentVersion.value
                    )
                },
            ) {
                MarqueeText(text = stringResource(R.string.main_launch_game))
            }
        }
    }
}

@Composable
private fun HomeStatusChip(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LauncherBackdropSurface(
        modifier = modifier,
        onClick = onClick,
        influencedByBackground = true,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
        drawWhiteOutline = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                text = title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
            Text(
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                text = value,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun VersionManagerLayout(
    isRefreshing: Boolean,
    version: Version?,
    modifier: Modifier = Modifier,
    swapToVersionManage: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .clip(shape = MaterialTheme.shapes.large)
            .clickable(onClick = swapToVersionManage)
            .padding(PaddingValues(all = 8.dp))
    ) {
        if (isRefreshing) {
            Box(modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator(modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.Center))
            }
        } else {
            VersionIconImage(
                version = version,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.CenterVertically)
            )
            Spacer(modifier = Modifier.width(8.dp))

            if (version == null) {
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .basicMarquee(iterations = Int.MAX_VALUE),
                    text = stringResource(R.string.versions_manage_no_versions),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                ) {
                    Text(
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                        text = version.getVersionName(),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                    if (version.isValid()) {
                        Text(
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                            text = version.getVersionSummary(),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
