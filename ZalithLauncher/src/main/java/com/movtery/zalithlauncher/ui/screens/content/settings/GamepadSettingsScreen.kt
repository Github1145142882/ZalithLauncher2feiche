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

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowRight
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.unit.floatRange
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedLazyColumn
import com.movtery.zalithlauncher.ui.components.CheckChip
import com.movtery.zalithlauncher.ui.components.LittleTextLabel
import com.movtery.zalithlauncher.ui.control.GamepadBindingKeyboard
import com.movtery.zalithlauncher.ui.control.gamepad.GamepadMap
import com.movtery.zalithlauncher.ui.control.gamepad.JoystickMode
import com.movtery.zalithlauncher.ui.control.gamepad.getNameByGamepadEvent
import com.movtery.zalithlauncher.ui.control.gamepad.remapperMMKV
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsBackground
import com.movtery.zalithlauncher.viewmodel.GamepadViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface BindKeyOperation {
    data object None : BindKeyOperation
    /** 展示键值绑定对话框，开始绑定键值 */
    data class OnBind(val map: GamepadMap) : BindKeyOperation
}

@Composable
fun GamepadSettingsScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: NavKey?,
    mainScreenKey: NavKey?
) {
    val viewModel: GamepadViewModel = viewModel()

    var operation by remember { mutableStateOf<BindKeyOperation>(BindKeyOperation.None) }

    /**
     * 编辑手柄按键绑定：true为游戏内，false为菜单内
     */
    var editKeyInGame by remember { mutableStateOf(true) }

    /**
     * 用于更新列表
     */
    var refreshed by remember { mutableStateOf(false) }

    BindKeyOperation(
        operation = operation,
        changeOperation = { operation = it },
        viewModel = viewModel,
        editKeyInGame = editKeyInGame,
        onRefresh = { refreshed = refreshed.not() }
    )

    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.Gamepad, settingsScreenKey, false)
    ) { isVisible ->
        AnimatedLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            isVisible = isVisible
        ) { scope ->
            animatedItem(scope) { yOffset ->
                SettingsBackground(
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
                ) {
                    val scope = rememberCoroutineScope()
                    val context = LocalContext.current

                    ClickableSettingsLayout(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.settings_gamepad_remapping_reset_title),
                        summary = stringResource(R.string.settings_gamepad_remapping_reset_summary),
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                val mmkv = remapperMMKV()
                                mmkv.clearAll()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.settings_gamepad_remapping_reset_finished),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )

                    SwitchSettingsLayout(
                        modifier = Modifier.fillMaxWidth(),
                        unit = AllSettings.gamepadControl,
                        title = stringResource(R.string.settings_gamepad_title),
                        summary = stringResource(R.string.settings_gamepad_summary)
                    )

                    SliderSettingsLayout(
                        modifier = Modifier.fillMaxWidth(),
                        unit = AllSettings.gamepadDeadZoneScale,
                        title = stringResource(R.string.settings_gamepad_deadzone_title),
                        summary = stringResource(R.string.settings_gamepad_deadzone_summary),
                        valueRange = AllSettings.gamepadDeadZoneScale.floatRange,
                        suffix = "%",
                        enabled = AllSettings.gamepadControl.state,
                        fineTuningControl = true
                    )

                    SliderSettingsLayout(
                        modifier = Modifier.fillMaxWidth(),
                        unit = AllSettings.gamepadCursorSensitivity,
                        title = stringResource(R.string.settings_gamepad_cursor_sensitivity_title),
                        summary = stringResource(R.string.settings_gamepad_cursor_sensitivity_summary),
                        valueRange = AllSettings.gamepadCursorSensitivity.floatRange,
                        suffix = "%",
                        enabled = AllSettings.gamepadControl.state,
                        fineTuningControl = true
                    )

                    SliderSettingsLayout(
                        modifier = Modifier.fillMaxWidth(),
                        unit = AllSettings.gamepadCameraSensitivity,
                        title = stringResource(R.string.settings_gamepad_camera_sensitivity_title),
                        summary = stringResource(R.string.settings_gamepad_camera_sensitivity_summary),
                        valueRange = AllSettings.gamepadCameraSensitivity.floatRange,
                        suffix = "%",
                        enabled = AllSettings.gamepadControl.state,
                        fineTuningControl = true
                    )

                    ListSettingsLayout(
                        unit = AllSettings.joystickControlMode,
                        items = JoystickMode.entries,
                        title = stringResource(R.string.settings_gamepad_joystick_mode_title),
                        summary = stringResource(R.string.settings_gamepad_joystick_mode_summary),
                        getItemText = { mode ->
                            stringResource(mode.titleRes)
                        },
                        getItemSummary = { mode ->
                            Text(
                                modifier = Modifier.alpha(0.7f),
                                text = stringResource(mode.summaryRes),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        enabled = AllSettings.gamepadControl.state
                    )
                }
            }

            animatedItem(scope) { yOffset ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    //游戏内
                    CheckChip(
                        selected = editKeyInGame,
                        label = {
                            Text(text = stringResource(R.string.settings_gamepad_mapping_in_game))
                        },
                        onClick = {
                            editKeyInGame = true
                        }
                    )

                    //菜单内
                    CheckChip(
                        selected = editKeyInGame.not(),
                        label = {
                            Text(text = stringResource(R.string.settings_gamepad_mapping_in_menu))
                        },
                        onClick = {
                            editKeyInGame = false
                        }
                    )
                }
            }

            animatedItems(
                lazyListScope = scope,
                items = GamepadMap.entries,
                key = { it.identifier }
            ) { index, item, yOffset ->
                SettingsBackground(
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                    onClick = {
                        operation = BindKeyOperation.OnBind(item)
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(all = 8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Image(
                            modifier = Modifier
                                .padding(all = 6.dp)
                                .size(32.dp),
                            painter = painterResource(item.getIconRes()),
                            contentDescription = null,
                            contentScale = ContentScale.Fit
                        )

                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val codes = remember(item, editKeyInGame, refreshed) {
                                viewModel.findByMap(item, inGame = editKeyInGame)?.toList() ?: emptyList()
                            }

                            Text(
                                text = if (codes.isNotEmpty()) {
                                    stringResource(R.string.settings_gamepad_mapping_bound)
                                } else {
                                    stringResource(R.string.settings_gamepad_mapping_unbound)
                                },
                                style = MaterialTheme.typography.titleSmall
                            )

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .basicMarquee(Int.MAX_VALUE),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                codes.fastForEach { code ->
                                    LittleTextLabel(
                                        text = getNameByGamepadEvent(code)
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.resetMapping(item, editKeyInGame)
                                    refreshed = refreshed.not()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = stringResource(R.string.generic_reset)
                                )
                            }

                            Icon(
                                modifier = Modifier.size(28.dp),
                                imageVector = Icons.AutoMirrored.Rounded.ArrowRight,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BindKeyOperation(
    operation: BindKeyOperation,
    changeOperation: (BindKeyOperation) -> Unit,
    viewModel: GamepadViewModel,
    editKeyInGame: Boolean,
    onRefresh: () -> Unit
) {
    when (operation) {
        is BindKeyOperation.None -> {}
        is BindKeyOperation.OnBind -> {
            val gamepad = operation.map

            val selectedKeys = remember(gamepad, editKeyInGame) {
                val mapList = viewModel.findByMap(gamepad, inGame = editKeyInGame)?.toList() ?: emptyList()
                mapList.toMutableList()
            }

            GamepadBindingKeyboard(
                selectedKeys = selectedKeys,
                onKeyAdd = { key ->
                    selectedKeys.add(key)
                    viewModel.saveMapping(gamepad, selectedKeys.toSet(), editKeyInGame)
                    onRefresh()
                },
                onKeyRemove = { key ->
                    selectedKeys.remove(key)
                    viewModel.saveMapping(gamepad, selectedKeys.toSet(), editKeyInGame)
                    onRefresh()
                },
                onDismissRequest = {
                    changeOperation(BindKeyOperation.None)
                }
            )
        }
    }
}