package com.movtery.zalithlauncher.ui.screens.content

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.google.gson.JsonSyntaxException
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.download.DownloadFailedException
import com.movtery.zalithlauncher.game.version.export.ExportInfo
import com.movtery.zalithlauncher.game.version.export.PackExporter
import com.movtery.zalithlauncher.game.version.export.PackType
import com.movtery.zalithlauncher.game.version.export.data.FileSelectionData
import com.movtery.zalithlauncher.game.version.export.data.Selected
import com.movtery.zalithlauncher.game.version.export.data.getSelectedFiles
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.info.InfoDistributor
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.fadeEdge
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.clearKeys
import com.movtery.zalithlauncher.ui.screens.content.elements.TitleTaskFlowDialog
import com.movtery.zalithlauncher.ui.screens.content.versions.export.ExportInfoScreen
import com.movtery.zalithlauncher.ui.screens.content.versions.export.ExportSelectFilesScreen
import com.movtery.zalithlauncher.ui.screens.content.versions.export.ExportTypeSelectScreen
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.ui.screens.onBack
import com.movtery.zalithlauncher.ui.screens.rememberTransitionSpec
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import com.movtery.zalithlauncher.viewmodel.sendKeepScreen
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

private sealed interface PackExportOperation {
    data object None : PackExportOperation
    /** 正在导出整合包中 */
    data object Exporting : PackExportOperation
    /** 成功导出整合包 */
    data object Finished : PackExportOperation
    /** 导出过程出现异常 */
    data class Error(val throwable: Throwable) : PackExportOperation
}

/**
 * 默认不选择的文件/文件夹
 */
private val selectBlackList = listOf(
    //Fabric的一些运行库，不需要打包
    ".fabric",
    //游戏配置文件默认不选择
    "options.txt",
    //包含natives的文件夹，一般是某些驱动文件
    //没有打包的必要
    "natives",
    "downloads",
    //各启动器的配置文件
    "PCL", InfoDistributor.LAUNCHER_IDENTIFIER, "fclversion.cfg",
    //一般来说不需要打包游戏存档
    "saves",
    //Realms配置
    "realms_persistence.json",
    //登录过的账号uuid缓存
    "usercache.json",
    //XXX日志
    ".log", "logs"
)

/**
 * @param versionName 当前游戏版本的自定义名称
 */
private class ExportModpackViewModel(
    val mcVersion: String,
    val versionName: String,
    val gamePath: File,
    val loader: ExportInfo.LoaderVersion?
): ViewModel() {
    private val _allFiles = MutableStateFlow<List<FileSelectionData>>(emptyList())
    /** 当前可供选择的全部文件/目录 */
    val allFiles = _allFiles.asStateFlow()

    private val _selectedFiles = MutableStateFlow(false)
    /** 当前是否选则了文件 */
    val selectedFiles = _selectedFiles.asStateFlow()

    private val _exportInfo = MutableStateFlow(defaultInfo())
    val exportInfo = _exportInfo.asStateFlow()

    fun editInfo(info: ExportInfo) {
        _exportInfo.update { info }
    }

    fun selectType(packType: PackType) {
        _exportInfo.update { defaultInfo(packType) }
    }

    private val _selectingFolder = MutableStateFlow(false)
    /** 用户是否正在选择导出目录 */
    val selectingFolder = _selectingFolder.asStateFlow()
    fun updateSelecting(value: Boolean) {
        _selectingFolder.update { value }
    }

    private fun defaultInfo(
        packType: PackType = PackType.Modrinth
    ): ExportInfo = ExportInfo(
        gamePath = gamePath,
        name = versionName,
        version = "1.0",
        mcVersion = mcVersion,
        loader = loader,
        packType = packType
    )

    private val _packExportOperation = MutableStateFlow<PackExportOperation>(PackExportOperation.None)
    val packExportOperation = _packExportOperation.asStateFlow()

    fun updateOperation(operation: PackExportOperation) {
        _packExportOperation.update { operation }
    }

    private val _packExporter = MutableStateFlow<PackExporter?>(null)
    /** 整合包导出器 */
    val packExporter = _packExporter.asStateFlow()

    private val startMutex = Mutex()
    fun startExport(
        version: Version,
        context: Context,
        outputUri: Uri,
        onStart: () -> Unit = {},
        onStop: () -> Unit = {},
        onFinished: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            val info = startMutex.withLock {
                val files = withContext(Dispatchers.Default) {
                    allFiles.value.getSelectedFiles()
                }
                _exportInfo.value.copy(
                    selectedFiles = files
                )
            }

            _packExportOperation.update { PackExportOperation.Exporting }
            _packExporter.update {
                PackExporter(
                    context = context,
                    exportInfo = info,
                    scope = viewModelScope
                ).also {
                    it.startExport(
                        outputUri = outputUri,
                        version = version,
                        onFinished = {
                            _packExporter.update { null }
                            _packExportOperation.update { PackExportOperation.Finished }
                            onStop()
                            onFinished()
                        },
                        onError = { throwable ->
                            _packExporter.update { null }
                            _packExportOperation.update { PackExportOperation.Error(throwable) }
                            onStop()
                        }
                    )
                    onStart()
                }
            }
        }
    }

    fun cancelExport() {
        _packExporter.value?.cancel()
        _packExporter.update { null }
        _packExportOperation.update { PackExportOperation.None }
    }


    private var currentRefreshJob: Job? = null

    /**
     * 刷新当前版本目录下可选择的文件列表
     */
    fun refreshFiles(
        isInit: Boolean = true
    ) {
        currentRefreshJob?.cancel()
        currentRefreshJob = viewModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) {
                _allFiles.update { emptyList() }
            }
            val temp = packFiles(gamePath, 1)

            if (isInit) {
                //如果是初始化，则预先选择部分文件
                temp.forEach { data ->
                    if (selectBlackList.any { data.file.name.contains(it) }) {
                        data.updateSelectState(Selected.Unselected)
                        return@forEach
                    }
                    data.updateSelectState(Selected.Selected)
                }
            }

            withContext(Dispatchers.Main) {
                _allFiles.update { temp }
            }

            refreshRootSelectSuspend()
        }
    }


    fun refreshRootSelect() {
        viewModelScope.launch(Dispatchers.Default) {
            refreshRootSelectSuspend()
        }
    }

    private val refreshSelectMutex = Mutex()
    private suspend fun refreshRootSelectSuspend() {
        refreshSelectMutex.withLock {
            val count = refreshTreeSelect(_allFiles.value)
            //根据选中的文件数量来判断是否选择了文件
            _selectedFiles.update { count > 0 }
        }
    }

    /**
     * @return 选中了多少个文件
     */
    private suspend fun refreshTreeSelect(list: List<FileSelectionData>): Int {
        var count = 0
        list.forEach { node ->
            count += node.refreshRootState()
            node.child?.let {
                count += refreshTreeSelect(it)
            }
        }
        return count
    }

    private val packBlackList = listOf(
        "$versionName.json",
        "$versionName.jar"
    )

    /**
     * 递归收集所有可选择的文件
     */
    private fun packFiles(root: File, depth: Int): List<FileSelectionData> {
        if (!root.isDirectory) return emptyList()

        val files = root.listFiles() ?: return emptyList()

        return files.asSequence()
            .filterNot { file ->
                packBlackList.any { pattern -> file.name.contains(pattern) }
            }
            .mapNotNull { file ->
                //只有根目录的文件才能设置别名
                val alias = if (depth == 1) getAlias(file.name) else null

                when {
                    file.isDirectory -> {
                        val childFiles = packFiles(file, depth + 1)
                        FileSelectionData(
                            file = file,
                            alias = alias,
                            child = childFiles.sorted()
                        )
                    }
                    else -> {
                        FileSelectionData(
                            file = file,
                            alias = alias,
                            child = null
                        )
                    }
                }
            }
            .sorted()
            .toList()
    }

    /**
     * 获取文件的别称（玩家看得懂的名称）
     */
    private fun getAlias(fileName: String): Int? {
        return when (fileName) {
            "options.txt" -> R.string.versions_export_alias_options
            "resourcepacks" -> R.string.versions_export_alias_resource_packs
            "mods" -> R.string.versions_export_alias_mods
            "saves" -> R.string.versions_export_alias_saves
            "shaderpacks" -> R.string.versions_export_alias_shaderpacks
            "config" -> R.string.versions_export_alias_config
            InfoDistributor.LAUNCHER_IDENTIFIER -> R.string.versions_export_alias_launcher
            else -> null
        }
    }

    override fun onCleared() {
        currentRefreshJob?.cancel()
        currentRefreshJob = null
    }
}

@Composable
private fun rememberExportModpackViewModel(
    version: Version
) = viewModel(
    key = version.toString() + "_ExportModpack"
) {
    val info = version.getVersionInfo()!!

    ExportModpackViewModel(
        mcVersion = info.minecraftVersion,
        versionName = version.getVersionName(),
        gamePath = version.getGameDir(),
        loader = info.loaderInfo?.let { loader ->
            ExportInfo.LoaderVersion(loader.loader, loader.version)
        }
    )
}

@Composable
fun VersionExportScreen(
    key: NestedNavKey.VersionExport,
    backScreenViewModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel,
    backToMainScreen: () -> Unit,
) {
    if (!key.version.isValid()) {
        backToMainScreen()
        return
    }

    val cBackToMainScreen by rememberUpdatedState(backToMainScreen)
    DisposableEffect(key) {
        val listener = object : suspend (List<Version>) -> Unit {
            override suspend fun invoke(versions: List<Version>) {
                cBackToMainScreen()
            }
        }
        VersionsManager.registerListener(listener)
        onDispose {
            VersionsManager.unregisterListener(listener)
        }
    }

    val viewModel = rememberExportModpackViewModel(version = key.version)

    val packExporter by viewModel.packExporter.collectAsStateWithLifecycle()
    val exportOperation by viewModel.packExportOperation.collectAsStateWithLifecycle()
    PackExportOperation(
        operation = exportOperation,
        onChange = { new ->
            viewModel.updateOperation(new)
        },
        packExporter = packExporter,
        onCancel = {
            viewModel.cancelExport()
        }
    )

    BaseScreen(
        screenKey = key,
        currentKey = backScreenViewModel.mainScreen.currentKey
    ) {
        NavigationUI(
            modifier = Modifier.fillMaxSize(),
            viewModel = viewModel,
            key = key,
            backScreenViewModel = backScreenViewModel,
            eventViewModel = eventViewModel,
            exportScreenKey = key.currentKey,
            onCurrentKeyChange = { key.currentKey = it },
            backToMainScreen = backToMainScreen,
            version = key.version
        )
    }
}

@Composable
private fun NavigationUI(
    viewModel: ExportModpackViewModel,
    key: NestedNavKey.VersionExport,
    backScreenViewModel: ScreenBackStackViewModel,
    eventViewModel: EventViewModel,
    exportScreenKey: NavKey?,
    onCurrentKeyChange: (NavKey?) -> Unit,
    backToMainScreen: () -> Unit,
    version: Version,
    modifier: Modifier = Modifier,
) {
    val mainScreenKey = backScreenViewModel.mainScreen.currentKey

    val backStack = key.backStack
    val stackTopKey = backStack.lastOrNull()
    LaunchedEffect(stackTopKey) {
        onCurrentKeyChange(stackTopKey)
    }

    if (backStack.isNotEmpty()) {
        NavDisplay(
            backStack = backStack,
            modifier = modifier,
            onBack = {
                onBack(backStack)
            },
            transitionSpec = rememberTransitionSpec(),
            popTransitionSpec = rememberTransitionSpec(),
            entryProvider = entryProvider {
                entry<NormalNavKey.VersionExports.SelectType> {
                    ExportTypeSelectScreen(
                        mainScreenKey = mainScreenKey,
                        exportScreenKey = exportScreenKey,
                        onTypeSelect = { type ->
                            viewModel.selectType(type)
                            backStack.navigateTo(NormalNavKey.VersionExports.EditInfo)
                        }
                    )
                }
                entry<NormalNavKey.VersionExports.EditInfo> {
                    val exportInfo by viewModel.exportInfo.collectAsStateWithLifecycle()
                    ExportInfoScreen(
                        info = exportInfo,
                        onInfoEdited = { viewModel.editInfo(it) },
                        onFinishClick = {
                            viewModel.refreshFiles()
                            viewModel.updateSelecting(false)
                            backStack.navigateTo(NormalNavKey.VersionExports.SelectFiles)
                        },
                        mainScreenKey = mainScreenKey,
                        exportScreenKey = exportScreenKey,
                    )
                }
                entry<NormalNavKey.VersionExports.SelectFiles> {
                    val allFiles by viewModel.allFiles.collectAsStateWithLifecycle()
                    val selectedFiles by viewModel.selectedFiles.collectAsStateWithLifecycle()
                    val isSelectingFolder by viewModel.selectingFolder.collectAsStateWithLifecycle()

                    val context = LocalContext.current

                    val safLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        val uri = result.data?.data?.let { uri ->
                            //获取永久读写uri
                            context.contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )
                            uri
                        }
                        if (uri != null) {
                            viewModel.startExport(
                                context = context,
                                outputUri = uri,
                                version = version,
                                onStart = {
                                    eventViewModel.sendKeepScreen(true)
                                },
                                onStop = {
                                    eventViewModel.sendKeepScreen(false)
                                },
                                onFinished = {
                                    backStack.clearKeys(
                                        NormalNavKey.VersionExports.EditInfo,
                                        NormalNavKey.VersionExports.SelectFiles
                                    )
                                }
                            )
                        }
                        viewModel.updateSelecting(false)
                    }

                    var showSelectTip by remember { mutableStateOf(false) }
                    if (showSelectTip) {
                        SimpleAlertDialog(
                            title = stringResource(R.string.versions_export_pack_select_folder_title),
                            text = stringResource(R.string.versions_export_pack_select_folder_message),
                            onDismiss = {
                                showSelectTip = false
                            },
                            onConfirm = {
                                showSelectTip = false
                                //开始选择目录
                                viewModel.updateSelecting(true)
                                safLauncher.launch(
                                    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                                )
                            }
                        )
                    }

                    ExportSelectFilesScreen(
                        allFiles = allFiles,
                        selectedFiles = selectedFiles,
                        onRefreshRootSelect = { viewModel.refreshRootSelect() },
                        isSelectingFolder = isSelectingFolder,
                        mainScreenKey = mainScreenKey,
                        exportScreenKey = exportScreenKey,
                        version = version,
                        backToMainScreen = backToMainScreen,
                        onFinish = {
                            showSelectTip = true
                        }
                    )
                }
            }
        )
    } else {
        Box(modifier)
    }
}

@Composable
private fun PackExportOperation(
    operation: PackExportOperation,
    onChange: (PackExportOperation) -> Unit,
    packExporter: PackExporter?,
    onCancel: () -> Unit,
) {
    when (operation) {
        is PackExportOperation.None -> {}
        is PackExportOperation.Exporting -> {
            if (packExporter != null) {
                val tasks by packExporter.taskFlow.collectAsStateWithLifecycle()
                if (tasks.isNotEmpty()) {
                    TitleTaskFlowDialog(
                        title = stringResource(R.string.versions_export),
                        tasks = tasks,
                        onCancel = {
                            onCancel()
                            onChange(PackExportOperation.None)
                        }
                    )
                }
            }
        }
        is PackExportOperation.Finished -> {
            SimpleAlertDialog(
                title = stringResource(R.string.versions_export),
                text = stringResource(R.string.versions_export_task_finished),
                onDismiss = {
                    onChange(PackExportOperation.None)
                }
            )
        }
        is PackExportOperation.Error -> {
            val th = operation.throwable
            lError("Failed to download the game!", th)
            val message = when (th) {
                is HttpRequestTimeoutException, is SocketTimeoutException -> stringResource(R.string.error_timeout)
                is UnknownHostException, is UnresolvedAddressException -> stringResource(R.string.error_network_unreachable)
                is ConnectException -> stringResource(R.string.error_connection_failed)
                is SerializationException, is JsonSyntaxException -> stringResource(R.string.error_parse_failed)
                is DownloadFailedException -> stringResource(R.string.download_install_error_download_failed)
                else -> {
                    val errorMessage = th.localizedMessage ?: th.message ?: th::class.qualifiedName ?: "Unknown error"
                    stringResource(R.string.error_unknown, errorMessage)
                }
            }
            val dismiss = {
                onChange(PackExportOperation.None)
            }
            AlertDialog(
                onDismissRequest = dismiss,
                title = {
                    Text(text = stringResource(R.string.versions_export_task_error_title))
                },
                text = {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fadeEdge(state = scrollState)
                            .verticalScroll(state = scrollState),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = stringResource(R.string.versions_export_task_error_message))
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