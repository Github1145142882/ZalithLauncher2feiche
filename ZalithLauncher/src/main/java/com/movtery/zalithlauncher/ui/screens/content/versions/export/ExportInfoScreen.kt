package com.movtery.zalithlauncher.ui.screens.content.versions.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.export.ExportInfo
import com.movtery.zalithlauncher.game.version.export.PackType
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedLazyColumn
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.main.control_editor.InfoLayoutSliderItem
import com.movtery.zalithlauncher.ui.screens.main.control_editor.InfoLayoutSwitchItem
import com.movtery.zalithlauncher.utils.platform.getMaxMemoryForSettings
import com.movtery.zalithlauncher.utils.string.isEmptyOrBlank
import com.movtery.zalithlauncher.utils.string.isNotEmptyOrBlank
import com.movtery.zalithlauncher.utils.string.toSingleLine

/**
 * 填写整合包配置信息
 * @param onFinishClick 结束编辑
 */
@Composable
fun ExportInfoScreen(
    info: ExportInfo,
    onInfoEdited: (ExportInfo) -> Unit,
    onFinishClick: () -> Unit,
    mainScreenKey: NavKey?,
    exportScreenKey: NavKey?,
) {
    BaseScreen(
        levels1 = listOf(
            Pair(NestedNavKey.VersionExport::class.java, mainScreenKey)
        ),
        Triple(NormalNavKey.VersionExports.EditInfo, exportScreenKey, false)
    ) { isVisible ->
        val isNameEmpty = remember(info) { info.name.isEmptyOrBlank() }
        val isVersionEmpty = remember(info) { info.version.isEmptyOrBlank() }
        val isAuthorEmpty = remember(info) { info.author.isEmptyOrBlank() }

        AnimatedLazyColumn(
            isVisible = isVisible,
            contentPadding = PaddingValues(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) { scope ->
            //整合包名称/版本编辑
            animatedItem(scope) { yOffset ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    OutlinedTextField(
                        modifier = Modifier.weight(0.7f),
                        value = info.name,
                        onValueChange = {
                            onInfoEdited(info.copy(name = it.toSingleLine()))
                        },
                        isError = isNameEmpty,
                        label = {
                            Text(text = stringResource(R.string.versions_export_pack_name))
                        },
                        supportingText = {
                            if (isNameEmpty) {
                                Text(text = stringResource(R.string.generic_cannot_empty))
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large
                    )

                    OutlinedTextField(
                        modifier = Modifier.weight(0.7f),
                        value = info.version,
                        onValueChange = {
                            onInfoEdited(info.copy(version = it.toSingleLine()))
                        },
                        isError = isVersionEmpty,
                        label = {
                            Text(text = stringResource(R.string.versions_export_pack_version))
                        },
                        supportingText = {
                            if (isVersionEmpty) {
                                Text(text = stringResource(R.string.generic_cannot_empty))
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large
                    )
                }
            }

            animatedItem(scope) { yOffset ->
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                    value = info.author,
                    onValueChange = {
                        onInfoEdited(info.copy(author = it.toSingleLine()))
                    },
                    isError = isAuthorEmpty,
                    label = {
                        Text(text = stringResource(R.string.versions_export_pack_author))
                    },
                    supportingText = {
                        if (isAuthorEmpty) {
                            Text(text = stringResource(R.string.generic_cannot_empty))
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )
            }

            animatedItem(scope) { yOffset ->
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                    value = info.summary ?: "",
                    onValueChange = { new ->
                        onInfoEdited(
                            info.copy(summary = new.takeIf { it.isNotEmptyOrBlank() }?.toSingleLine())
                        )
                    },
                    label = {
                        Text(text = stringResource(R.string.versions_export_pack_summary))
                    },
                    supportingText = {
                        Text(text = stringResource(R.string.versions_export_pack_summary_hint))
                    },
                    minLines = 3,
                    shape = MaterialTheme.shapes.large
                )
            }

            when (info.packType) {
                PackType.MCBBS -> {
                    //游戏参数
                    animatedItem(scope) { yOffset ->
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                            value = info.jvmArgs,
                            onValueChange = { new ->
                                onInfoEdited(
                                    info.copy(jvmArgs = new.toSingleLine())
                                )
                            },
                            label = {
                                Text(text = stringResource(R.string.versions_export_pack_jvm_args))
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large
                        )
                    }

                    //Java虚拟机参数
                    animatedItem(scope) { yOffset ->
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                            value = info.javaArgs,
                            onValueChange = { new ->
                                onInfoEdited(
                                    info.copy(javaArgs = new.toSingleLine())
                                )
                            },
                            label = {
                                Text(text = stringResource(R.string.versions_export_pack_java_args))
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large
                        )
                    }

                    //整合包官方网站
                    animatedItem(scope) { yOffset ->
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                            value = info.url,
                            onValueChange = { new ->
                                onInfoEdited(
                                    info.copy(url = new.toSingleLine())
                                )
                            },
                            label = {
                                Text(text = stringResource(R.string.versions_export_pack_website))
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large
                        )
                    }

                    //最小内存
                    animatedItem(scope) { yOffset ->
                        var memory by remember(info) { mutableFloatStateOf(info.minMemory.toFloat()) }

                        InfoLayoutSliderItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                            title = stringResource(R.string.versions_export_pack_min_memory),
                            value = memory,
                            onValueChange = {
                                memory = it
                            },
                            onValueChangeFinished = {
                                onInfoEdited(info.copy(minMemory = memory.toInt()))
                            },
                            valueRange = 0f..getMaxMemoryForSettings(LocalContext.current).toFloat(),
                            decimalFormat = "#0",
                            fineTuningStep = 1.0f,
                            suffix = "MB"
                        )
                    }
                }
                PackType.Modrinth -> {
                    //是否打包远程资源
                    animatedItem(scope) { yOffset ->
                        InfoLayoutSwitchItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                            title = stringResource(R.string.versions_export_pack_pack_remote),
                            value = info.packRemote,
                            onValueChange = {
                                onInfoEdited(info.copy(packRemote = it))
                            }
                        )
                    }

                    //是否打包 CurseForge 的远程资源
                    animatedItem(scope) { yOffset ->
                        InfoLayoutSwitchItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                            title = stringResource(R.string.versions_export_pack_pack_curseforge),
                            value = info.packCurseForge,
                            onValueChange = {
                                onInfoEdited(info.copy(packCurseForge = it))
                            },
                            enabled = info.packRemote
                        )
                    }
                }

                PackType.MultiMC -> {
                    //最小内存
                    animatedItem(scope) { yOffset ->
                        var memory by remember(info) { mutableFloatStateOf(info.minMemory.toFloat()) }

                        InfoLayoutSliderItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                            title = stringResource(R.string.versions_export_pack_min_memory),
                            value = memory,
                            onValueChange = {
                                memory = it
                            },
                            onValueChangeFinished = {
                                onInfoEdited(info.copy(minMemory = memory.toInt()))
                            },
                            valueRange = 0f..getMaxMemoryForSettings(LocalContext.current).toFloat(),
                            decimalFormat = "#0",
                            fineTuningStep = 1.0f,
                            suffix = "MB"
                        )
                    }

                    //最大内存
                    animatedItem(scope) { yOffset ->
                        var memory by remember(info) { mutableFloatStateOf(info.maxMemory.toFloat()) }

                        InfoLayoutSliderItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                            title = stringResource(R.string.versions_export_pack_max_memory),
                            value = memory,
                            onValueChange = {
                                memory = it
                            },
                            onValueChangeFinished = {
                                onInfoEdited(info.copy(maxMemory = memory.toInt()))
                            },
                            valueRange = 0f..getMaxMemoryForSettings(LocalContext.current).toFloat(),
                            decimalFormat = "#0",
                            fineTuningStep = 1.0f,
                            suffix = "MB"
                        )
                    }
                }
            }

            //导出按钮
            animatedItem(scope) { yOffset ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = onFinishClick,
                        enabled = !isNameEmpty && !isVersionEmpty && !isAuthorEmpty
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val text = stringResource(R.string.versions_export_pack_select_files)
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = text
                            )

                            Text(text = text)
                        }
                    }
                }
            }
        }
    }
}