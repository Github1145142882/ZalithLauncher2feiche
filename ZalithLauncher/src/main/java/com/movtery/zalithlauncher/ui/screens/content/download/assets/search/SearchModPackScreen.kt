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

package com.movtery.zalithlauncher.ui.screens.content.download.assets.search

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.google.gson.JsonSyntaxException
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeModpackCategory
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.curseForgeModLoaderFilters
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthFeatures
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthModpackCategory
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.modrinthModLoaderFilters
import com.movtery.zalithlauncher.game.download.jvm_server.JvmCrashException
import com.movtery.zalithlauncher.game.download.modpack.install.ModpackImporter
import com.movtery.zalithlauncher.game.download.modpack.install.PackNotSupportedException
import com.movtery.zalithlauncher.game.download.modpack.install.UnsupportedPackReason
import com.movtery.zalithlauncher.game.download.modpack.platform.PackPlatform
import com.movtery.zalithlauncher.game.version.download.DownloadFailedException
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.fadeEdge
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.download.ModpackVersionNameDialog
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.BaseFilterLayout
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.PackIdentifier
import com.movtery.zalithlauncher.ui.screens.content.elements.TitleTaskFlowDialog
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerializationException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

/** 导入整合包相关操作 */
private sealed interface ModpackImportOperation {
    data object None : ModpackImportOperation
    /** 警告用户整合包兼容性问题 */
    data object Warning : ModpackImportOperation
    /** 开始导入整合包 */
    data object Import : ModpackImportOperation
    /** 不支持的整合包或格式无效 */
    data class NotSupport(val reason: UnsupportedPackReason) : ModpackImportOperation
    /** 整合包导入完成 */
    data object Finished : ModpackImportOperation
    /** 导入整合包时出现异常 */
    data class Error(val th: Throwable) : ModpackImportOperation
}

/** 整合包版本名称自定义状态操作 */
private sealed interface VersionNameOperation {
    data object None : VersionNameOperation
    /** 等待用户输入版本名称 */
    data class Waiting(val name: String) : VersionNameOperation
}

private class ModpackViewModel : ViewModel() {
    var importOperation by mutableStateOf<ModpackImportOperation>(ModpackImportOperation.None)
    var versionNameOperation by mutableStateOf<VersionNameOperation>(VersionNameOperation.None)

    //等待用户输入版本名称相关
    private var versionNameContinuation: (Continuation<String>)? = null
    suspend fun waitForVersionName(name: String): String {
        return suspendCancellableCoroutine { cont ->
            versionNameContinuation = cont
            versionNameOperation = VersionNameOperation.Waiting(name)
        }
    }

    /**
     * 用户确认输入版本名称
     */
    fun confirmVersionName(name: String) {
        //恢复continuation
        versionNameContinuation?.resume(name)
        versionNameContinuation = null
        versionNameOperation = VersionNameOperation.None
    }

    /**
     * 整合包导入器
     */
    var importer by mutableStateOf<ModpackImporter?>(null)

    /**
     * 开始导入整合包
     */
    fun import(
        context: Context,
        uri: Uri
    ) {
        importOperation = ModpackImportOperation.Import
        importer = ModpackImporter(
            context = context,
            uri = uri,
            scope = viewModelScope,
            waitForVersionName = ::waitForVersionName
        ).also {
            it.startImport(
                onFinished = {
                    importer = null
                    VersionsManager.refresh()
                    importOperation = ModpackImportOperation.Finished
                },
                onError = { th ->
                    importer = null
                    importOperation = if (th is PackNotSupportedException) {
                        //整合包不受支持，无法导入
                        ModpackImportOperation.NotSupport(th.reason)
                    } else {
                        ModpackImportOperation.Error(th)
                    }
                }
            )
        }
    }

    fun cancel() {
        importer?.cancel()
        importer = null
        importOperation = ModpackImportOperation.None
    }

    override fun onCleared() {
        cancel()
    }
}

@Composable
private fun rememberModpackViewModel(): ModpackViewModel {
    val screenKey = NormalNavKey.SearchModPack.toString()
    return viewModel(
        key = "${screenKey}_import"
    ) {
        ModpackViewModel()
    }
}

@Composable
fun SearchModPackScreen(
    mainScreenKey: NavKey?,
    downloadScreenKey: NavKey?,
    downloadModPackScreenKey: NavKey,
    downloadModPackScreenCurrentKey: NavKey?,
    swapToDownload: (Platform, projectId: String, iconUrl: String?) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val viewModel = rememberModpackViewModel()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { uri ->
            viewModel.import(context, uri)
        }
    }

    ModpackImportOperation(
        operation = viewModel.importOperation,
        changeOperation = { viewModel.importOperation = it },
        selectedUri = {
            //允许导入任意文件，在导入整合包的流程中会对文件进行判断
            filePicker.launch(arrayOf("*/*"))
        },
        importer = viewModel.importer,
        onCancel = {
            viewModel.cancel()
        }
    )

    //用户确认版本名称 操作流程
    VersionNameOperation(
        operation = viewModel.versionNameOperation,
        onConfirmVersionName = { name ->
            viewModel.confirmVersionName(name)
        }
    )

    SearchAssetsScreen(
        mainScreenKey = mainScreenKey,
        parentScreenKey = downloadModPackScreenKey,
        parentCurrentKey = downloadScreenKey,
        screenKey = NormalNavKey.SearchModPack,
        currentKey = downloadModPackScreenCurrentKey,
        platformClasses = PlatformClasses.MOD_PACK,
        initialPlatform = Platform.MODRINTH,
        getCategories = { platform ->
            when (platform) {
                Platform.CURSEFORGE -> CurseForgeModpackCategory.entries
                Platform.MODRINTH -> ModrinthModpackCategory.entries
            }
        },
        enableModLoader = true,
        getModloaders = { platform ->
            when (platform) {
                Platform.CURSEFORGE -> curseForgeModLoaderFilters
                Platform.MODRINTH -> modrinthModLoaderFilters
            }
        },
        mapCategories = { platform, string ->
            when (platform) {
                Platform.MODRINTH -> {
                    ModrinthModpackCategory.entries.find { it.facetValue() == string }
                        ?: ModrinthFeatures.entries.find { it.facetValue() == string }
                }
                Platform.CURSEFORGE -> {
                    CurseForgeModpackCategory.entries.find { it.describe() == string }
                }
            }
        },
        swapToDownload = swapToDownload,
        extraFilter = {
            //新增导入整合包按钮
            item {
                BaseFilterLayout(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { role = Role.Button },
                    onClick = {
                        if (viewModel.importOperation == ModpackImportOperation.None) {
                            //先警告用户关于整合包的兼容性问题
                            viewModel.importOperation = ModpackImportOperation.Warning
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MarqueeText(
                            text = stringResource(R.string.import_modpack),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun ModpackImportOperation(
    operation: ModpackImportOperation,
    changeOperation: (ModpackImportOperation) -> Unit,
    selectedUri: () -> Unit,
    importer: ModpackImporter?,
    onCancel: () -> Unit
) {
    when (operation) {
        is ModpackImportOperation.None -> {}
        is ModpackImportOperation.Warning -> {
            //警告整合包的兼容性（免责声明）
            SimpleAlertDialog(
                title = stringResource(R.string.generic_tip),
                text = {
                    Text(text = stringResource(R.string.import_modpack_tip))

                    Spacer(Modifier.height(8.dp))
                    AllSupportPackDisplay(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))

                    Text(text = stringResource(R.string.download_modpack_warning1))
                    Text(text = stringResource(R.string.download_modpack_warning2))

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.download_modpack_warning3),
                        fontWeight = FontWeight.Bold
                    )
                },
                confirmText = stringResource(R.string.generic_import),
                onCancel = {
                    changeOperation(ModpackImportOperation.None)
                },
                onConfirm = {
                    changeOperation(ModpackImportOperation.None)
                    selectedUri()
                }
            )
        }
        is ModpackImportOperation.Import -> {
            if (importer != null) {
                val tasks by importer.taskFlow.collectAsState()
                if (tasks.isNotEmpty()) {
                    TitleTaskFlowDialog(
                        title = stringResource(R.string.import_modpack),
                        tasks = tasks,
                        onCancel = {
                            onCancel()
                            changeOperation(ModpackImportOperation.None)
                        }
                    )
                }
            }
        }
        is ModpackImportOperation.NotSupport -> {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(text = stringResource(R.string.import_modpack_not_supported_title))
                },
                text = {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fadeEdge(state = scrollState)
                            .verticalScroll(state = scrollState),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        when (operation.reason) {
                            UnsupportedPackReason.CorruptedArchive -> {
                                //因文件无法解压导致的无法导入
                                Text(text = stringResource(R.string.import_modpack_not_supported_text1))

                                Text(text = stringResource(R.string.import_modpack_not_supported_text2))
                                Text(text = stringResource(R.string.import_modpack_not_supported_text3))

                                Text(text = stringResource(R.string.import_modpack_not_supported_text4))
                            }
                            UnsupportedPackReason.UnsupportedFormat -> {
                                //启动器确实不支持这个格式
                                Text(text = stringResource(R.string.import_modpack_not_supported_formats))
                                AllSupportPackDisplay(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            changeOperation(ModpackImportOperation.None)
                        }
                    ) {
                        MarqueeText(text = stringResource(R.string.generic_confirm))
                    }
                }
            )
        }
        is ModpackImportOperation.Finished -> {
            SimpleAlertDialog(
                title = stringResource(R.string.import_modpack_finished_title),
                text = stringResource(R.string.import_modpack_finished_text)
            ) {
                changeOperation(ModpackImportOperation.None)
            }
        }
        is ModpackImportOperation.Error -> {
            val th = operation.th
            lError("Failed to download the game!", th)
            val message = when (th) {
                is HttpRequestTimeoutException, is SocketTimeoutException -> stringResource(R.string.error_timeout)
                is UnknownHostException, is UnresolvedAddressException -> stringResource(R.string.error_network_unreachable)
                is ConnectException -> stringResource(R.string.error_connection_failed)
                is SerializationException, is JsonSyntaxException -> stringResource(R.string.error_parse_failed)
                is JvmCrashException -> stringResource(R.string.download_install_error_jvm_crash, th.code)
                is DownloadFailedException -> stringResource(R.string.download_install_error_download_failed)
                else -> {
                    val errorMessage = th.localizedMessage ?: th.message ?: th::class.qualifiedName ?: "Unknown error"
                    stringResource(R.string.error_unknown, errorMessage)
                }
            }
            val dismiss = {
                changeOperation(ModpackImportOperation.None)
            }
            AlertDialog(
                onDismissRequest = dismiss,
                title = {
                    Text(text = stringResource(R.string.import_modpack_failed_title))
                },
                text = {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fadeEdge(state = scrollState)
                            .verticalScroll(state = scrollState),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = stringResource(R.string.import_modpack_failed_text))
                        Text(text = message)
                    }
                },
                confirmButton = {
                    Button(onClick = dismiss) {
                        MarqueeText(text = stringResource(R.string.generic_confirm))
                    }
                }
            )
        }
    }
}

/**
 * 所有支持的整合包格式展示
 */
@Composable
private fun AllSupportPackDisplay(
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PackPlatform.entries.fastForEach { platform ->
            PackIdentifier(
                platform = platform
            )
        }
    }
}

@Composable
private fun VersionNameOperation(
    operation: VersionNameOperation,
    onConfirmVersionName: (String) -> Unit
) {
    when (operation) {
        is VersionNameOperation.None -> {}
        is VersionNameOperation.Waiting -> {
            ModpackVersionNameDialog(
                name = operation.name,
                onConfirmVersionName = onConfirmVersionName
            )
        }
    }
}