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

package com.movtery.zalithlauncher.ui.screens.content.versions

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.control.ControlManager
import com.movtery.zalithlauncher.game.multirt.RuntimesManager
import com.movtery.zalithlauncher.game.plugin.driver.DriverPluginManager
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionConfig
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.unit.floatRange
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedColumn
import com.movtery.zalithlauncher.ui.components.IDItem
import com.movtery.zalithlauncher.ui.components.SimpleIDListLayout
import com.movtery.zalithlauncher.ui.components.SimpleIntSliderLayout
import com.movtery.zalithlauncher.ui.components.SimpleListLayout
import com.movtery.zalithlauncher.ui.components.TextInputLayout
import com.movtery.zalithlauncher.ui.components.TitleAndSummary
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.MemoryPreview
import com.movtery.zalithlauncher.ui.screens.content.elements.MicrophoneCheckOperation
import com.movtery.zalithlauncher.ui.screens.content.elements.MicrophoneCheckState
import com.movtery.zalithlauncher.ui.screens.content.settings.DriverSummaryLayout
import com.movtery.zalithlauncher.ui.screens.content.settings.RendererSummaryLayout
import com.movtery.zalithlauncher.ui.screens.content.versions.layouts.VersionSettingsBackground
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import com.movtery.zalithlauncher.utils.platform.getMaxMemoryForSettings
import com.movtery.zalithlauncher.utils.string.getMessageOrToString
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel

@Composable
fun VersionConfigScreen(
    mainScreenKey: NavKey?,
    versionsScreenKey: NavKey?,
    version: Version,
    backToMainScreen: () -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    if (!version.isValid()) {
        backToMainScreen()
        return
    }

    BaseScreen(
        levels1 = listOf(
            Pair(NestedNavKey.VersionSettings::class.java, mainScreenKey)
        ),
        Triple(NormalNavKey.Versions.Config, versionsScreenKey, false)
    ) { isVisible ->
        val config = version.getVersionConfig()

        AnimatedColumn(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(state = rememberScrollState())
                .padding(all = 12.dp),
            isVisible = isVisible
        ) { scope ->
            AnimatedItem(scope) { yOffset ->
                VersionConfigs(
                    config = config,
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                    submitError = submitError
                )
            }

            AnimatedItem(scope) { yOffset ->
                GameConfigs(
                    config = config,
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                    submitError = submitError
                )
            }

            AnimatedItem(scope) { yOffset ->
                SupportConfigs(
                    config = config,
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                    submitError = submitError
                )
            }
        }
    }
}

@Composable
private fun VersionConfigs(
    config: VersionConfig,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    VersionSettingsBackground(modifier = modifier) {
        Text(
            modifier = Modifier.padding(all = 8.dp),
            text = stringResource(R.string.versions_config_version_settings),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge
        )

        StatefulDropdownMenuFollowGlobal(
            currentValue = config.isolationType,
            onValueChange = { type ->
                if (config.isolationType != type) {
                    config.isolationType = type
                    config.saveOrShowError(context, submitError)
                }
            },
            title = stringResource(R.string.versions_config_isolation_title),
            summary = stringResource(R.string.versions_config_isolation_summary)
        )

        StatefulDropdownMenuFollowGlobal(
            currentValue = config.skipGameIntegrityCheck,
            onValueChange = { type ->
                if (config.skipGameIntegrityCheck != type) {
                    config.skipGameIntegrityCheck = type
                    config.saveOrShowError(context, submitError)
                }
            },
            title = stringResource(R.string.settings_game_skip_game_integrity_check_title),
            summary = stringResource(R.string.settings_game_skip_game_integrity_check_summary)
        )

        val renderers = Renderers.getCompatibleRenderers(context).second
        val renderersIdList = getIDList(renderers) { IDItem(it.getUniqueIdentifier(), it.getRendererName()) }
        SimpleListLayout(
            items = renderersIdList,
            currentId = config.renderer,
            defaultId = "",
            title = stringResource(R.string.versions_config_renderer),
            getItemText = { it.title },
            getItemId = { it.id },
            getItemSummary = { item ->
                renderers.find { it.getUniqueIdentifier() == item.id }?.let { renderer ->
                    RendererSummaryLayout(renderer)
                }
            },
            onValueChange = { item ->
                if (config.renderer != item.id) {
                    config.renderer = item.id
                    config.saveOrShowError(context, submitError)
                }
            }
        )

        val drivers = DriverPluginManager.getDriverList()
        val driversIdList = getIDList(drivers) { IDItem(it.id, it.name) }
        SimpleListLayout(
            items = driversIdList,
            currentId = config.driver,
            defaultId = "",
            title = stringResource(R.string.versions_config_vulkan_driver),
            getItemText = { it.title },
            getItemId = { it.id },
            getItemSummary = { item ->
                drivers.find { it.id == item.id }?.let { driver ->
                    DriverSummaryLayout(driver)
                }
            },
            onValueChange = { item ->
                if (config.driver != item.id) {
                    config.driver = item.id
                    config.saveOrShowError(context, submitError)
                }
            }
        )

        val controls by ControlManager.dataList.collectAsState()
        val controlsIdList = getIDList(controls.filter { it.isSupport }) {
            IDItem(it.file.name, it.controlLayout.info.name.translate())
        }
        SimpleListLayout(
            items = controlsIdList,
            currentId = config.control,
            defaultId = "",
            title = stringResource(R.string.versions_config_control),
            getItemText = { it.title },
            getItemId = { it.id },
            onValueChange = {
                if (config.control != it.id) {
                    config.control = it.id
                    config.saveOrShowError(context, submitError)
                }
            }
        )
    }
}

@Composable
private fun GameConfigs(
    config: VersionConfig,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    VersionSettingsBackground(modifier = modifier) {
        Text(
            modifier = Modifier.padding(all = 8.dp),
            text = stringResource(R.string.versions_config_game_settings),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge
        )

        SimpleIDListLayout(
            items = getIDList(RuntimesManager.getRuntimes().filter { it.isCompatible() }) { IDItem(it.name, it.name) },
            currentId = config.javaRuntime,
            defaultId = "",
            title = stringResource(R.string.settings_game_java_runtime_title),
            summary = stringResource(R.string.versions_config_java_runtime_summary),
            onValueChange = { item ->
                if (config.javaRuntime != item.id) {
                    config.javaRuntime = item.id
                    config.saveOrShowError(context, submitError)
                }
            }
        )

        /**
         * 临时已分配内存，用于UI状态更新
         */
        var ramAllocation by remember { mutableIntStateOf(config.ramAllocation) }
        ToggleableSliderSetting(
            currentValue = config.ramAllocation,
            valueRange = AllSettings.ramAllocation.floatRange.start..getMaxMemoryForSettings(LocalContext.current).toFloat(),
            defaultValue = AllSettings.ramAllocation.getValue(),
            title = stringResource(R.string.settings_game_java_memory_title),
            summary = stringResource(R.string.settings_game_java_memory_summary),
            suffix = "MB",
            onValueChange = {
                config.ramAllocation = it
                ramAllocation = it
            },
            onValueChangeFinished = { config.saveOrShowError(context, submitError) }
        )

        AnimatedVisibility(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(start = 2.dp),
            visible = ramAllocation >= 256
        ) {
            Column {
                MemoryPreview(
                    modifier = Modifier.fillMaxWidth(),
                    preview = ramAllocation.takeIf { it >= 256 }?.toDouble(),
                    usedText = { usedMemory, totalMemory ->
                        stringResource(R.string.settings_game_java_memory_used_text, usedMemory.toInt(), totalMemory.toInt())
                    },
                    previewText = { preview ->
                        stringResource(R.string.settings_game_java_memory_allocation_text, preview.toInt())
                    }
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        TextInputLayout(
            currentValue = config.customInfo,
            title = stringResource(R.string.settings_game_version_custom_info_title),
            summary = stringResource(R.string.settings_game_version_custom_info_summary),
            onValueChange = { value ->
                if (config.customInfo != value) {
                    config.customInfo = value
                    config.saveOrShowError(context, submitError)
                }
            },
            label = {
                Text(text = stringResource(R.string.versions_config_follow_global_if_blank))
            }
        )

        TextInputLayout(
            currentValue = config.jvmArgs,
            title = stringResource(R.string.settings_game_jvm_args_title),
            summary = stringResource(R.string.settings_game_jvm_args_summary),
            onValueChange = { value ->
                if (config.jvmArgs != value) {
                    config.jvmArgs = value
                    config.saveOrShowError(context, submitError)
                }
            },
            label = {
                Text(text = stringResource(R.string.versions_config_follow_global_if_blank))
            }
        )

        TextInputLayout(
            modifier = Modifier.padding(bottom = 4.dp),
            currentValue = config.serverIp,
            title = stringResource(R.string.versions_config_auto_join_server_ip_title),
            summary = stringResource(R.string.versions_config_auto_join_server_ip_summary),
            onValueChange = { value ->
                if (config.serverIp != value) {
                    config.serverIp = value
                    config.saveOrShowError(context, submitError)
                }
            },
            label = {
                Text(text = stringResource(R.string.versions_config_disable_if_blank))
            }
        )
    }
}

@Composable
private fun SupportConfigs(
    config: VersionConfig,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    VersionSettingsBackground(modifier = modifier) {
        Text(
            modifier = Modifier.padding(all = 8.dp),
            text = stringResource(R.string.versions_config_support_settings),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge
        )

        var enableTouchProxy by remember { mutableStateOf(config.enableTouchProxy) }

        SwitchConfigLayout(
            currentValue = enableTouchProxy,
            onCheckedChange = { value ->
                enableTouchProxy = value
                if (config.enableTouchProxy != value) {
                    config.enableTouchProxy = value
                    config.saveOrShowError(context, submitError)
                }
            },
            title = stringResource(R.string.versions_config_enable_touch_proxy_title),
            summary = stringResource(R.string.versions_config_enable_touch_proxy_summary)
        )

        var touchVibrateDuration by remember { mutableIntStateOf(config.touchVibrateDuration) }

        SimpleIntSliderLayout(
            value = touchVibrateDuration,
            title = stringResource(R.string.versions_config_vibrate_duration_title),
            summary = stringResource(R.string.versions_config_vibrate_duration_summary),
            valueRange = 80f..500f,
            onValueChange = {
                touchVibrateDuration = it
                config.touchVibrateDuration = touchVibrateDuration
            },
            onValueChangeFinished = {
                config.saveOrShowError(context, submitError)
            },
            suffix = "ms",
            fineTuningControl = true
        )

        CheckMicrophoneLayout(
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CheckMicrophoneLayout(
    modifier: Modifier = Modifier
) {
    var state by remember { mutableStateOf<MicrophoneCheckState>(MicrophoneCheckState.None) }

    MicrophoneCheckOperation(
        state = state,
        changeState = { state = it }
    )

    Column(
        modifier = modifier
            .clip(shape = RoundedCornerShape(22.0.dp))
            .clickable {
                state = MicrophoneCheckState.Start
            }
            .padding(all = 8.dp)
            .padding(bottom = 4.dp)
    ) {
        TitleAndSummary(
            title = stringResource(R.string.versions_config_microphone_check_title),
            summary = stringResource(R.string.versions_config_microphone_check_summary)
        )
    }
}

@Composable
private fun <E> getIDList(list: List<E>, toIDItem: (E) -> IDItem): List<IDItem> {
    return list.map {
        toIDItem(it)
    }.toMutableList().apply {
        add(0, IDItem("", stringResource(R.string.generic_follow_global)))
    }
}

private fun VersionConfig.saveOrShowError(
    context: Context,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    runCatching {
        saveWithThrowable()
    }.onFailure { e ->
        lError("Failed to save version config!", e)
        submitError(
            ErrorViewModel.ThrowableMessage(
                title = context.getString(R.string.versions_config_failed_to_save),
                message = e.getMessageOrToString()
            )
        )
    }
}