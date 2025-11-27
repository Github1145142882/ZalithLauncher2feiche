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

package com.movtery.zalithlauncher.ui.screens.content.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.plugin.driver.Driver
import com.movtery.zalithlauncher.game.plugin.driver.DriverPluginManager
import com.movtery.zalithlauncher.game.renderer.RendererInterface
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.unit.floatRange
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedColumn
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.SwitchLayout
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsBackground
import com.movtery.zalithlauncher.utils.device.checkVulkanSupport
import com.movtery.zalithlauncher.utils.isAdrenoGPU

@Composable
fun RendererSettingsScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: NavKey?,
    mainScreenKey: NavKey?
) {
    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.Renderer, settingsScreenKey, false)
    ) { isVisible ->
        val context = LocalContext.current

        AnimatedColumn(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(state = rememberScrollState())
                .padding(all = 12.dp),
            isVisible = isVisible
        ) { scope ->
            AnimatedItem(scope) { yOffset ->
                SettingsBackground(
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
                ) {
                    ListSettingsLayout(
                        modifier = Modifier.fillMaxWidth(),
                        unit = AllSettings.renderer,
                        items = Renderers.getCompatibleRenderers(context).second,
                        title = stringResource(R.string.settings_renderer_global_renderer_title),
                        summary = stringResource(R.string.settings_renderer_global_renderer_summary),
                        getItemText = { it.getRendererName() },
                        getItemId = { it.getUniqueIdentifier() },
                        getItemSummary = {
                            RendererSummaryLayout(it)
                        }
                    )

                    ListSettingsLayout(
                        modifier = Modifier.fillMaxWidth(),
                        unit = AllSettings.vulkanDriver,
                        items = DriverPluginManager.getDriverList(),
                        title = stringResource(R.string.settings_renderer_global_vulkan_driver_title),
                        getItemText = { it.name },
                        getItemId = { it.id },
                        getItemSummary = {
                            DriverSummaryLayout(it)
                        }
                    )

                    SliderSettingsLayout(
                        modifier = Modifier.fillMaxWidth(),
                        unit = AllSettings.resolutionRatio,
                        title = stringResource(R.string.settings_renderer_resolution_scale_title),
                        summary = stringResource(R.string.settings_renderer_resolution_scale_summary),
                        valueRange = AllSettings.resolutionRatio.floatRange,
                        suffix = "%",
                        fineTuningControl = true
                    )

                    SwitchSettingsLayout(
                        modifier = Modifier.fillMaxWidth(),
                        unit = AllSettings.gameFullScreen,
                        title = stringResource(R.string.settings_renderer_full_screen_title),
                        summary = stringResource(R.string.settings_renderer_full_screen_summary)
                    )
                }
            }

            AnimatedItem(scope) { yOffset ->
                SettingsBackground(
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
                ) {
                    SwitchSettingsLayout(
                        modifier = Modifier.fillMaxWidth(),
                        unit = AllSettings.sustainedPerformance,
                        title = stringResource(R.string.settings_renderer_sustained_performance_title),
                        summary = stringResource(R.string.settings_renderer_sustained_performance_summary)
                    )

                    if (checkVulkanSupport(LocalContext.current.packageManager)) {
                        var adrenoGPUAlert by remember { mutableStateOf(false) }

                        var value by remember { mutableStateOf(AllSettings.zinkPreferSystemDriver.getValue()) }

                        fun change(value1: Boolean) {
                            value = value1
                            AllSettings.zinkPreferSystemDriver.save(value)
                        }

                        SwitchLayout(
                            modifier = Modifier.fillMaxWidth(),
                            title = stringResource(R.string.settings_renderer_vulkan_driver_system_title),
                            summary = stringResource(R.string.settings_renderer_vulkan_driver_system_summary),
                            checked = value,
                            onCheckedChange = { checked ->
                                if (checked && isAdrenoGPU()) adrenoGPUAlert = true
                                else change(checked)
                            }
                        )

                        if (adrenoGPUAlert) {
                            SimpleAlertDialog(
                                title = stringResource(R.string.generic_warning),
                                text = stringResource(R.string.settings_renderer_zink_driver_adreno),
                                onConfirm = {
                                    change(true)
                                    adrenoGPUAlert = false
                                },
                                onDismiss = {
                                    change(false)
                                    adrenoGPUAlert = false
                                }
                            )
                        }
                    }

                    SwitchSettingsLayout(
                        modifier = Modifier.fillMaxWidth(),
                        unit = AllSettings.vsyncInZink,
                        title = stringResource(R.string.settings_renderer_vsync_in_zink_title),
                        summary = stringResource(R.string.settings_renderer_vsync_in_zink_summary)
                    )

                    SwitchSettingsLayout(
                        modifier = Modifier.fillMaxWidth(),
                        unit = AllSettings.bigCoreAffinity,
                        title = stringResource(R.string.settings_renderer_force_big_core_title),
                        summary = stringResource(R.string.settings_renderer_force_big_core_summary)
                    )

                    SwitchSettingsLayout(
                        modifier = Modifier.fillMaxWidth(),
                        unit = AllSettings.dumpShaders,
                        title = stringResource(R.string.settings_renderer_shader_dump_title),
                        summary = stringResource(R.string.settings_renderer_shader_dump_summary)
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun RendererSummaryLayout(renderer: RendererInterface) {
    FlowRow(
        modifier = Modifier.alpha(0.7f),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        with(renderer) {
            getRendererSummary()?.let { summary ->
                Text(text = summary, style = MaterialTheme.typography.labelSmall)
            }

            val minVer = getMinMCVersion()
            val maxVer = getMaxMCVersion()

            if (minVer != null || maxVer != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = stringResource(R.string.renderer_version_support), style = MaterialTheme.typography.labelSmall)

                    minVer?.let {
                        Text(text = ">= $it", style = MaterialTheme.typography.labelSmall)
                    }

                    maxVer?.let {
                        Text(text = "<= $it", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun DriverSummaryLayout(driver: Driver) {
    with(driver) {
        summary?.let { text ->
            Text(
                modifier = Modifier.alpha(0.7f),
                text = text, style = MaterialTheme.typography.labelSmall
            )
        }
    }
}