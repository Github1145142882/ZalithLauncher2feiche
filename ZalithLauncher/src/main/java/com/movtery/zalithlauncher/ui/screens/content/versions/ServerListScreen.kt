package com.movtery.zalithlauncher.ui.screens.content.versions

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.multiplayer.ServerData
import com.movtery.zalithlauncher.game.version.multiplayer.ServerPingResult
import com.movtery.zalithlauncher.game.version.multiplayer.description.ComponentDescription
import com.movtery.zalithlauncher.game.version.multiplayer.description.ComponentDescriptionRoot
import com.movtery.zalithlauncher.game.version.multiplayer.description.ServerDescription
import com.movtery.zalithlauncher.game.version.multiplayer.description.StringDescription
import com.movtery.zalithlauncher.game.version.multiplayer.parseServerData
import com.movtery.zalithlauncher.game.version.multiplayer.pingServer
import com.movtery.zalithlauncher.game.version.multiplayer.resolve
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.CardTitleLayout
import com.movtery.zalithlauncher.ui.components.EdgeDirection
import com.movtery.zalithlauncher.ui.components.ScalingLabel
import com.movtery.zalithlauncher.ui.components.ShimmerBox
import com.movtery.zalithlauncher.ui.components.SimpleTextInputField
import com.movtery.zalithlauncher.ui.components.fadeEdge
import com.movtery.zalithlauncher.ui.components.itemLayoutColor
import com.movtery.zalithlauncher.ui.components.itemLayoutShadowElevation
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.ComponentText
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.MinecraftColorText
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.MinecraftColorTextNormal
import com.movtery.zalithlauncher.ui.screens.content.versions.layouts.VersionChunkBackground
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.LaunchGameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.LinkedList

sealed interface ServerListOperation {
    /** 服务器列表刷新中 */
    data object Loading : ServerListOperation
    /** 已加载服务器数据 */
    data object LoadedData : ServerListOperation
}

/**
 * 保存单个服务器的状态
 */
private class ServerState(
    val data: ServerData
) {
    sealed interface Operation {
        data object Loading : Operation
        /** 服务器加载成功 */
        data class Loaded(val result: ServerPingResult) : Operation
        /** 无法连接至服务器 */
        data object Failed : Operation
    }

    var icon by mutableStateOf<Any?>(data.icon)
        private set

    var operation by mutableStateOf<Operation>(Operation.Loading)
        private set

    suspend fun load() {
        withContext(Dispatchers.Main) {
            operation = Operation.Loading
        }

        runCatching {
            val resolvedAddress = data.ip.resolve()
            val result = pingServer(resolvedAddress)

            withContext(Dispatchers.Main) {
                icon = result.status.favicon ?: data.icon
                operation = Operation.Loaded(result)
            }
        }.onFailure {
            lWarning("Unable to load/connect to server: ${data.ip}", it)
            withContext(Dispatchers.Main) {
                operation = Operation.Failed
            }
        }

    }

    fun cancel() {

    }
}

private class ServerListViewModel(
    val serverData: File
) : ViewModel() {
    var operation by mutableStateOf<ServerListOperation>(ServerListOperation.Loading)

    private val internalServers = mutableListOf<ServerState>()
    private val _servers = MutableStateFlow<List<ServerState>?>(emptyList<ServerState>())
    val servers = _servers.asStateFlow()

    /**
     * 搜索服务器的名称
     */
    var searchName by mutableStateOf("")

    /** 作为标记，记录哪些服务器已被加载 */
    private val serversToLoad = mutableListOf<ServerState>()
    private val loadQueue = LinkedList<ServerState>()
    private val semaphore = Semaphore(8) //一次最多允许同时加载8个服务器
    private var initialQueueSize = 0
    private val queueMutex = Mutex()

    private var refreshJob: Job? = null
    private var searchJob: Job? = null

    /**
     * 开始加载服务器列表数据
     */
    fun loadServer() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            internalServers.clear()

            withContext(Dispatchers.Main) {
                operation = ServerListOperation.Loading
                _servers.update { emptyList() }
            }

            val dataList = parseServerData(serverData)
            internalServers.addAll(
                dataList.map { data ->
                    ServerState(data)
                }
            )

            withContext(Dispatchers.Main) {
                operation = ServerListOperation.LoadedData
                _servers.update {
                    if (internalServers.isEmpty()) {
                        null
                    } else {
                        filteredServers()
                    }
                }
            }
        }
    }

    private fun filteredServers(): List<ServerState> {
        return internalServers.filter { server ->
            server.data.name.contains(searchName)
        }
    }

    /**
     * 根据现有的搜索名称，刷新显示服务器列表
     */
    fun filterServers() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val servers = filteredServers()
            _servers.update { servers }
        }
    }

    private fun startQueueProcessor() {
        viewModelScope.launch {
            while (true) {
                try {
                    ensureActive()
                } catch (_: Exception) {
                    break //取消
                }

                val server = queueMutex.withLock {
                    loadQueue.poll()
                } ?: run {
                    delay(100)
                    continue
                }

                semaphore.acquire()

                launch {
                    try {
                        server.load()
                    } finally {
                        semaphore.release()
                    }
                }
            }
        }
    }

    /**
     * 尝试 Ping 服务器
     */
    fun loadServer(
        server: ServerState,
        isRefresh: Boolean = false
    ) {
        if (isRefresh) {
            viewModelScope.launch {
                queueMutex.withLock {
                    loadQueue.removeAll { it == server }
                    loadQueue.addFirst(server) //加入队头优先执行
                }
            }
            if (serversToLoad.contains(server)) return //已在加载列表
            serversToLoad.add(server)
            return
        }

        if (serversToLoad.contains(server)) return

        serversToLoad.add(server)
        viewModelScope.launch {
            queueMutex.withLock {
                val canJoin = loadQueue.size <= (initialQueueSize / 2)
                if (canJoin || loadQueue.none { it == server }) {
                    loadQueue.add(server)
                    serversToLoad.add(server)
                    //若当前是新一轮任务，更新初始队列总数
                    if (initialQueueSize == 0 || canJoin) {
                        initialQueueSize = loadQueue.size
                    }
                }
            }
        }
    }

    init {
        loadServer()
        startQueueProcessor()
    }

    override fun onCleared() {
        refreshJob?.cancel()
        refreshJob = null
    }
}

@Composable
private fun rememberServerListViewModel(
    serverData: File,
    version: Version
): ServerListViewModel {
    return viewModel(
        key = version.toString() + "_" + "ServerList"
    ) {
        ServerListViewModel(
            serverData = serverData
        )
    }
}

@Composable
fun ServerListScreen(
    mainScreenKey: NavKey?,
    versionsScreenKey: NavKey?,
    launchGameViewModel: LaunchGameViewModel,
    version: Version,
    backToMainScreen: () -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    if (!version.isValid()) {
        backToMainScreen()
        return
    }

    val dataFile = remember(version) {
        File(version.getGameDir(), "servers.dat")
    }

    BaseScreen(
        levels1 = listOf(
            Pair(NestedNavKey.VersionSettings::class.java, mainScreenKey)
        ),
        Triple(NormalNavKey.Versions.ServerList, versionsScreenKey, false),
    ) { isVisible ->
        val viewModel = rememberServerListViewModel(
            serverData = dataFile,
            version = version
        )

        val yOffset by swapAnimateDpAsState(
            targetValue = (-40).dp,
            swapIn = isVisible
        )

        VersionChunkBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 12.dp)
                .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
            paddingValues = PaddingValues()
        ) {
            when (viewModel.operation) {
                is ServerListOperation.Loading -> {
                    Box(Modifier.fillMaxSize()) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                }
                is ServerListOperation.LoadedData -> {
                    val servers by viewModel.servers.collectAsStateWithLifecycle()

                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        ServerListHeader(
                            searchName = viewModel.searchName,
                            onSearchNameChange = {
                                viewModel.searchName = it
                                viewModel.filterServers()
                            },
                            refreshServers = {
                                viewModel.loadServer()
                            }
                        )

                        ServerListBody(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            servers = servers,
                            onLoad = { viewModel.loadServer(it) },
                            onRefresh = { viewModel.loadServer(it, true) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerListHeader(
    searchName: String,
    onSearchNameChange: (String) -> Unit,
    refreshServers: () -> Unit,
    modifier: Modifier = Modifier,
    inputFieldColor: Color = itemLayoutColor(),
    inputFieldContentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    CardTitleLayout(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SimpleTextInputField(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                    value = searchName,
                    onValueChange = { onSearchNameChange(it) },
                    hint = {
                        Text(
                            text = stringResource(R.string.generic_search),
                            style = TextStyle(color = LocalContentColor.current).copy(fontSize = 12.sp)
                        )
                    },
                    color = inputFieldColor,
                    contentColor = inputFieldContentColor,
                    singleLine = true
                )

                val scrollState = rememberScrollState()
                LaunchedEffect(Unit) {
                    scrollState.scrollTo(scrollState.maxValue)
                }
                Row(
                    modifier = Modifier
                        .fadeEdge(
                            state = scrollState,
                            length = 32.dp,
                            direction = EdgeDirection.Horizontal
                        )
                        .widthIn(max = this@BoxWithConstraints.maxWidth / 2)
                        .horizontalScroll(scrollState),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = refreshServers
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.generic_refresh)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerListBody(
    servers: List<ServerState>?,
    onLoad: (ServerState) -> Unit,
    onRefresh: (ServerState) -> Unit,
    modifier: Modifier = Modifier,
) {
    servers?.let { list ->
        if (list.isNotEmpty()) {
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(all = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(list) { server ->
                    ServerItem(
                        modifier = Modifier.fillMaxWidth(),
                        item = server,
                        onLoad = { onLoad(server) },
                        onRefresh = { onRefresh(server) }
                    )
                }
            }
        } else {
            //如果列表是空的，则是由搜索导致的
            //展示“无匹配项”文本
            Box(modifier = Modifier.fillMaxSize()) {
                ScalingLabel(
                    modifier = Modifier.align(Alignment.Center),
                    text = stringResource(R.string.generic_no_matching_items)
                )
            }
        }
    } ?: run {
        //如果为null，则代表本身就没有存档可以展示
        Box(modifier = Modifier.fillMaxSize()) {
            ScalingLabel(
                modifier = Modifier.align(Alignment.Center),
                text = stringResource(R.string.servers_list_no_servers)
            )
        }
    }
}

@Composable
private fun ServerItem(
    item: ServerState,
    onLoad: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    shape: Shape = MaterialTheme.shapes.large,
    itemColor: Color = itemLayoutColor(),
    itemContentColor: Color = MaterialTheme.colorScheme.onSurface,
    shadowElevation: Dp = itemLayoutShadowElevation()
) {
    val ot = item.operation

    val scale = remember { Animatable(initialValue = 0.95f) }
    LaunchedEffect(Unit) {
        scale.animateTo(targetValue = 1f, animationSpec = getAnimateTween())
    }

    LaunchedEffect(item) {
        onLoad()
    }

    Surface(
        modifier = modifier.graphicsLayer(scaleY = scale.value, scaleX = scale.value),
        onClick = onClick,
        shape = shape,
        color = itemColor,
        contentColor = itemContentColor,
        shadowElevation = shadowElevation
    ) {
        val alphaModifier = Modifier.alpha(0.7f)

        Row(
            modifier = Modifier
                .padding(all = 8.dp)
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            //服务器的图标
            ServerIcon(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(10.dp)),
                server = item,
                size = 64.dp,
            )

            Column(
                modifier = Modifier.weight(1f),
            ) {
                //服务器名称、状态、描述
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        //服务器名称
                        MinecraftColorTextNormal(
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                            inputText = item.data.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1
                        )

                        //显示服务器的延迟、在线人数
                        if (ot is ServerState.Operation.Loaded) {
                            val undefined = stringResource(R.string.servers_list_undefined)

                            //服务器延迟显示部分
                            val signalStrength = remember(ot) {
                                val pingMs = ot.result.pingMs
                                if (pingMs < 150L) 5
                                else if (pingMs < 300L) 4
                                else if (pingMs < 600L) 3
                                else if (pingMs < 1000L) 2
                                else 1
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ServerSignalIcon(
                                    modifier = Modifier.size(16.dp),
                                    signalStrength = signalStrength
                                )
                                Text(
                                    modifier = alphaModifier,
                                    text = "${ot.result.pingMs} ms",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            //在线人数显示部分
                            val playerFull = stringResource(R.string.servers_list_players_full)

                            val onlineStatus = remember(ot) {
                                val players = ot.result.status.players

                                buildString {
                                    val max = players.max
                                    val online = players.online

                                    //在线玩家数小于0，则认为服务器未定义
                                    if (online < 0) {
                                        append(undefined)
                                    } else {
                                        if (max > 0) {
                                            if (online in 0..players.max) {
                                                append(online)
                                            } else {
                                                append(playerFull)
                                            }
                                            append('/')
                                            append(players.max)
                                        } else {
                                            //服务器未定义最大玩家数
                                            //仅显示当前在线玩家数
                                            append(online)
                                        }
                                    }
                                }
                                "${players.online}/${players.max}"
                            }
                            //在线人数信息
                            Row(
                                modifier = alphaModifier,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    modifier = Modifier.size(16.dp),
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null
                                )
                                Text(
                                    text = onlineStatus,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    when (ot) {
                        is ServerState.Operation.Loading -> {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        is ServerState.Operation.Loaded -> {
                            ot.result.status.description?.let { des ->
                                DescriptionTextRender(
                                    description = des,
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                    maxLines = 2
                                )
                            }
                        }

                        is ServerState.Operation.Failed -> {
                            Text(
                                text = stringResource(R.string.servers_list_failed_to_connect),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                //服务器ip地址
                Text(
                    modifier = alphaModifier,
                    text = item.data.originIp,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            //刷新服务器按钮
            IconButton(
                onClick = onRefresh,
                enabled = ot !is ServerState.Operation.Loading
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.generic_refresh)
                )
            }
        }
    }
}

/**
 * 服务器信号动态绘制图标
 */
@Composable
fun ServerSignalIcon(
    modifier: Modifier = Modifier,
    signalStrength: Int,
    mainColor: Color = MaterialTheme.colorScheme.primary,
    otherColor: Color = MaterialTheme.colorScheme.background,
    contentPadding: PaddingValues = PaddingValues(vertical = 2.dp)
) {
    require(signalStrength in 1..5) {
        "signalStrength must be between 1 and 5"
    }

    Canvas(modifier = modifier.padding(contentPadding)) {
        val barCount = 5
        val barWidth = size.width / (barCount * 2 - 1)
        val spacing = barWidth

        for (i in 0 until barCount) {
            val barHeight = size.height * (i + 1) / barCount
            val xOffset = i * (barWidth + spacing)
            val yOffset = size.height - barHeight

            drawRect(
                color = if (i < signalStrength) mainColor else otherColor,
                topLeft = Offset(xOffset, yOffset),
                size = Size(barWidth, barHeight)
            )
        }
    }
}

@Composable
private fun ServerIcon(
    server: ServerState,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val density = LocalDensity.current
    val pxSize = with(density) { size.roundToPx() }

    val imageRequest = remember(server.icon, pxSize) {
        val builder = ImageRequest.Builder(context)

        if (server.icon == null) {
            //如果服务器没有提供可用的图标，则使用本地缓存的图标
            builder.data(server.data.icon)
                .size(pxSize) //固定大小
                .crossfade(true)
        } else {
            builder.data(server.icon)
                .size(pxSize)
                .crossfade(true)
        }

        builder.build()
    }

    val painter = rememberAsyncImagePainter(
        model = imageRequest,
        placeholder = null,
        error = painterResource(R.drawable.ic_unknown_icon)
    )

    val state by painter.state.collectAsStateWithLifecycle()
    val sizeModifier = modifier.size(size)

    when (state) {
        AsyncImagePainter.State.Empty -> {
            Box(modifier = sizeModifier)
        }
        is AsyncImagePainter.State.Loading -> {
            ShimmerBox(
                modifier = sizeModifier
            )
        }
        is AsyncImagePainter.State.Error,
        is AsyncImagePainter.State.Success -> {
            Image(
                painter = painter,
                contentDescription = null,
                alignment = Alignment.Center,
                contentScale = ContentScale.Fit,
                modifier = sizeModifier,
            )
        }
    }
}

/**
 * 服务器描述文本渲染，尝试模仿 Minecraft 原版对于 Component 文本组件的渲染
 */
@Composable
private fun DescriptionTextRender(
    description: ServerDescription,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) {
    when(description) {
        is ComponentDescriptionRoot -> {
            ComponentText(
                modifier = modifier,
                descriptions = description.values,
                fontSize = fontSize,
                maxLines = maxLines
            )
        }
        is ComponentDescription -> {
            ComponentText(
                modifier = modifier,
                descriptions = listOf(description),
                fontSize = fontSize,
                maxLines = maxLines
            )
        }
        is StringDescription -> {
            val value = remember(description) { description.value }
            MinecraftColorText(
                modifier = modifier,
                inputText = value,
                fontSize = fontSize,
                maxLines = maxLines
            )
        }
    }
}