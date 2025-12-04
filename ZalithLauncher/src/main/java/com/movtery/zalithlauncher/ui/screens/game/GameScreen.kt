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

package com.movtery.zalithlauncher.ui.screens.game

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movtery.layer_controller.ControlBoxLayout
import com.movtery.layer_controller.data.HideLayerWhen
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.layer_controller.event.EventHandler
import com.movtery.layer_controller.layout.ControlLayout
import com.movtery.layer_controller.layout.EmptyControlLayout
import com.movtery.layer_controller.layout.loadLayoutFromFile
import com.movtery.layer_controller.observable.ObservableControlLayout
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.bridge.CURSOR_DISABLED
import com.movtery.zalithlauncher.bridge.ZLBridgeStates
import com.movtery.zalithlauncher.game.input.LWJGLCharSender
import com.movtery.zalithlauncher.game.keycodes.ControlEventKeycode
import com.movtery.zalithlauncher.game.keycodes.LwjglGlfwKeycode
import com.movtery.zalithlauncher.game.keycodes.OPEN_CHAT
import com.movtery.zalithlauncher.game.keycodes.mapToKeycode
import com.movtery.zalithlauncher.game.support.touch_controller.touchControllerInputModifier
import com.movtery.zalithlauncher.game.support.touch_controller.touchControllerTouchModifier
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.isLauncherInDarkTheme
import com.movtery.zalithlauncher.setting.enums.toAction
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.components.MenuState
import com.movtery.zalithlauncher.ui.control.MinecraftHotbar
import com.movtery.zalithlauncher.ui.control.event.KeyEventHandler
import com.movtery.zalithlauncher.ui.control.event.launcherEvent
import com.movtery.zalithlauncher.ui.control.event.lwjglEvent
import com.movtery.zalithlauncher.ui.control.gamepad.GamepadKeyListener
import com.movtery.zalithlauncher.ui.control.gamepad.GamepadStickMovementListener
import com.movtery.zalithlauncher.ui.control.gamepad.SimpleGamepadCapture
import com.movtery.zalithlauncher.ui.control.gyroscope.GyroscopeReader
import com.movtery.zalithlauncher.ui.control.gyroscope.isGyroscopeAvailable
import com.movtery.zalithlauncher.ui.control.hotbarPercentage
import com.movtery.zalithlauncher.ui.control.input.TextInputMode
import com.movtery.zalithlauncher.ui.control.input.TopOverlayAboveIme
import com.movtery.zalithlauncher.ui.control.input.textInputHandler
import com.movtery.zalithlauncher.ui.control.mouse.SwitchableMouseLayout
import com.movtery.zalithlauncher.ui.screens.game.elements.DraggableGameBall
import com.movtery.zalithlauncher.ui.screens.game.elements.ForceCloseOperation
import com.movtery.zalithlauncher.ui.screens.game.elements.GameMenuSubscreen
import com.movtery.zalithlauncher.ui.screens.game.elements.LogBox
import com.movtery.zalithlauncher.ui.screens.game.elements.LogState
import com.movtery.zalithlauncher.ui.screens.game.elements.ReplacementControlOperation
import com.movtery.zalithlauncher.ui.screens.game.elements.ReplacementControlState
import com.movtery.zalithlauncher.ui.screens.game.elements.SendKeycodeOperation
import com.movtery.zalithlauncher.ui.screens.game.elements.SendKeycodeState
import com.movtery.zalithlauncher.ui.screens.game.multiplayer.TerracottaOperation
import com.movtery.zalithlauncher.ui.screens.game.multiplayer.rememberTerracottaViewModel
import com.movtery.zalithlauncher.ui.screens.main.control_editor.ControlEditor
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import com.movtery.zalithlauncher.viewmodel.EditorViewModel
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.GamepadViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lwjgl.glfw.CallbackBridge
import java.io.File

private class GameViewModel(private val version: Version) : ViewModel() {
    /** 游戏菜单操作状态 */
    var gameMenuState by mutableStateOf(MenuState.NONE)
    /** 游戏菜单悬浮球当前的位置 */
    var gameBallPosition by mutableStateOf(Offset.Zero)
    /** 游戏菜单-控制设置区域Tab选择的索引 */
    var controlMenuTabIndex by mutableIntStateOf(0)
    /** 强制关闭弹窗操作状态 */
    var forceCloseState by mutableStateOf<ForceCloseOperation>(ForceCloseOperation.None)
    /** 发送键值操作状态 */
    var sendKeycodeState by mutableStateOf<SendKeycodeState>(SendKeycodeState.None)
    /** 更换控制布局操作状态 */
    var replacementControlState by mutableStateOf<ReplacementControlState>(ReplacementControlState.None)
    /** 输入法状态 */
    var textInputMode by mutableStateOf(TextInputMode.DISABLE)
    /** 被控制布局层标记为仅滑动的指针列表 */
    var moveOnlyPointers = mutableSetOf<PointerId>()
    /** 鼠标触摸指针处理层占用指针列表 */
    var occupiedPointers = mutableSetOf<PointerId>()

    /** 可观察的控制布局 */
    var observableLayout by mutableStateOf<ObservableControlLayout?>(null)
        private set
    /** 当前控制布局文件 */
    var currentControlFile by mutableStateOf<File?>(null)
        private set
    /** 控制布局：控件层隐藏状态 */
    var controlLayerHideState by mutableStateOf(HideLayerWhen.None)
        private set

    /** 是否正在编辑布局 */
    var isEditingLayout by mutableStateOf(false)
        private set

    fun switchControlLayer(hideWhen: HideLayerWhen) {
        if (controlLayerHideState != hideWhen) controlLayerHideState = hideWhen
    }

    /** 虚拟鼠标滚动事件处理 */
    val mouseScrollUpEvent = MouseScrollEvent(viewModelScope, 1.0)
    val mouseScrollDownEvent = MouseScrollEvent(viewModelScope, -1.0)

    /** 游戏内消息发送器 */
    val gameTextSender = GameTextSender(viewModelScope)

    /** 所有已按下的按键 */
    val pressedKeyEvents = KeyEventHandler { key, pressed ->
        lwjglEvent(eventKey = key, isMouse = key.startsWith("GLFW_MOUSE_", false), isPressed = pressed)
    }
    /** 所有已按下的启动器事件 */
    val pressedLauncherEvents = KeyEventHandler { key, pressed ->
        launcherEvent(
            eventKey = key,
            isPressed = pressed,
            onSwitchIME = { switchIME() },
            onSwitchMenu = { switchMenu() },
            onSingleScrollUp = { mouseScrollUpEvent.scrollSingle() },
            onSingleScrollDown = { mouseScrollDownEvent.scrollSingle() },
            onLongScrollUp = { mouseScrollUpEvent.scrollLongPress() },
            onLongScrollUpCancel = { mouseScrollUpEvent.cancel() },
            onLongScrollDown = { mouseScrollDownEvent.scrollLongPress() },
            onLongScrollDownCancel = { mouseScrollDownEvent.cancel() }
        )
    }
    /** 控制布局控件点击事件处理器 */
    val eventHandler = EventHandler { event, pressed ->
        onKeyEvent(event, pressed)
    }

    /** 处理控制布局类点击事件 */
    fun onKeyEvent(event: ClickEvent, pressed: Boolean) {
        val events = when (event.type) {
            ClickEvent.Type.Key -> pressedKeyEvents
            ClickEvent.Type.LauncherEvent -> pressedLauncherEvents
            ClickEvent.Type.SendText -> {
                //游戏内文本发送事件
                if (pressed) {
                    val text = event.key
                    val inGame = ZLBridgeStates.cursorMode == CURSOR_DISABLED
                    gameTextSender.send(GameTextSender.Data(text, inGame))
                }
                return
            }
            else -> return
        }
        if (pressed) {
            events.pressKey(event.key)
        } else {
            events.releaseKey(event.key)
        }
    }

    fun loadControlLayout(layoutFile: File? = version.getControlPath()) {
        observableLayout = null
        currentControlFile = layoutFile
        val layout = getLayout(layoutFile)
        //将控制布局加载为可供Compose加载的形式
        observableLayout = ObservableControlLayout(layout)
    }

    private fun getLayout(layoutFile: File? = currentControlFile): ControlLayout {
        return layoutFile?.let {
            try {
                loadLayoutFromFile(it)
            } catch (e: Exception) {
                lWarning("Failed to load control layout: $it", e)
                null
            }
        } ?: EmptyControlLayout
    }

    /**
     * 开始编辑控制布局模式
     */
    fun startControlEditor(editorVM: EditorViewModel) {
        if (!isEditingLayout) {
            clearState()
            editorVM.forceChangeLayout(getLayout())
            isEditingLayout = true
        }
    }

    /**
     * 退出编辑控制布局模式（如果当前确实正在编辑控制布局）
     */
    fun exitControlEditor() {
        if (isEditingLayout) {
            isEditingLayout = false
            loadControlLayout(currentControlFile)
        }
    }

    /**
     * 切换输入法
     */
    fun switchIME() {
        this.textInputMode = this.textInputMode.switch()
    }

    /**
     * 切换游戏菜单
     */
    fun switchMenu() {
        this.gameMenuState = this.gameMenuState.next()
    }

    /**
     * 清除所有游戏状态
     */
    fun clearState() {
        mouseScrollUpEvent.cancel()
        mouseScrollDownEvent.cancel()
        gameTextSender.cancel()
        pressedKeyEvents.clearEvent()
        pressedLauncherEvents.clearEvent()
        textInputMode = TextInputMode.DISABLE
    }

    init {
        loadControlLayout()
    }

    override fun onCleared() {
        clearState()
    }
}

/**
 * 鼠标滚轮事件管理
 * @param offset 滚轮滚动距离
 */
private class MouseScrollEvent(
    private val scope: CoroutineScope,
    private val offset: Double
) {
    private var mouseScrollJob: Job? = null

    /**
     * 取消滚动事件，并重置状态
     */
    fun cancel() {
        mouseScrollJob?.cancel()
        mouseScrollJob = null
    }

    /**
     * 单击响应一次滚轮滚动事件
     */
    fun scrollSingle() {
        CallbackBridge.sendScroll(0.0, offset)
    }

    /**
     * 长按不间断触发滚轮滚动事件
     */
    fun scrollLongPress() {
        mouseScrollJob?.cancel()
        mouseScrollJob = scope.launch {
            while (true) {
                try {
                    ensureActive()
                    CallbackBridge.sendScroll(0.0, offset)
                    delay(50)
                } catch (_: Exception) {
                    break
                }
            }
            mouseScrollJob = null
        }
    }
}

/**
 * 游戏内消息发送器
 */
private class GameTextSender(private val scope: CoroutineScope) {
    /**
     * @param text 要发送的文本
     * @param inGame 当前是否处于游戏内，如果在游戏中，则会尝试打开聊天栏
     */
    data class Data(
        val text: String,
        val inGame: Boolean
    )

    private var messageChannel: Channel<Data>? = null
    private var job: Job? = null

    fun cancel() {
        job?.cancel()
        messageChannel?.close()
        messageChannel = null
        job = null
    }

    /**
     * 尝试向游戏发送文本（排队发送）
     */
    fun send(data: Data) {
        if (job?.isActive != true || messageChannel == null) {
            job?.cancel()
            messageChannel?.close()

            messageChannel = Channel(Channel.UNLIMITED)
            job = scope.launch {
                messageChannel?.let { channel ->
                    for ((text, inGame) in channel) {
                        sendMessage(text, inGame)
                    }
                }
            }
        }

        messageChannel?.trySend(data)
    }

    private suspend fun sendMessage(text: String, inGame: Boolean) {
        withContext(Dispatchers.Main) {
            fun sendText() {
                for (ch in text) {
                    LWJGLCharSender.sendChar(ch)
                }
            }

            if (inGame) {
                //根据options.txt中的配置，找到打开聊天栏的键
                //如果找不到，则忽略这次事件
                mapToKeycode(OPEN_CHAT)?.let { openChat ->
                    CallbackBridge.sendKeyPress(openChat)
                    delay(50)
                    sendText()
                    delay(50)
                    LWJGLCharSender.sendEnter()
                }
            } else {
                //如果当前不在游戏内，则直接发送文本
                sendText()
            }
        }
    }
}

@Composable
private fun rememberGameViewModel(
    version: Version
) = viewModel(
    key = version.toString()
) {
    GameViewModel(version)
}

@Composable
fun GameScreen(
    version: Version,
    isGameRendering: Boolean,
    logState: LogState,
    onLogStateChange: (LogState) -> Unit,
    isTouchProxyEnabled: Boolean,
    onInputAreaRectUpdated: (IntRect?) -> Unit,
    surfaceOffset: Offset,
    incrementScreenOffset: (Offset) -> Unit,
    resetScreenOffset: () -> Unit,
    getAccountName: () -> String,
    eventViewModel: EventViewModel,
    gamepadViewModel: GamepadViewModel
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val viewModel = rememberGameViewModel(version)
    val editorViewModel: EditorViewModel = viewModel()
    val isGrabbing = remember(ZLBridgeStates.cursorMode) {
        ZLBridgeStates.cursorMode == CURSOR_DISABLED
    }
    val terracottaViewModel = rememberTerracottaViewModel(
        keyTag = version.toString() + "_Terracotta",
        eventViewModel = eventViewModel,
        getUserName = getAccountName
    )

    SendKeycodeOperation(
        operation = viewModel.sendKeycodeState,
        onChange = { viewModel.sendKeycodeState = it },
        lifecycleScope = viewModel.viewModelScope
    )

    ForceCloseOperation(
        operation = viewModel.forceCloseState,
        onChange = { viewModel.forceCloseState = it },
        text = stringResource(R.string.game_menu_option_force_close_text)
    )

    ReplacementControlOperation(
        operation = viewModel.replacementControlState,
        onChange = { viewModel.replacementControlState = it },
        currentLayout = viewModel.currentControlFile,
        replacementControl = { viewModel.loadControlLayout(it) }
    )

    TerracottaOperation(
        viewModel = terracottaViewModel
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
        val screenSize = remember(maxWidth, maxHeight) {
            with(density) {
                IntSize(
                    width = maxWidth.roundToPx(),
                    height = maxHeight.roundToPx()
                )
            }
        }

        GameInfoBox(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(all = 16.dp),
            version = version,
            isGameRendering = isGameRendering
        )

        if (!viewModel.isEditingLayout) {
            if (AllSettings.gamepadControl.state && gamepadViewModel.gamepadEngaged) {
                //手柄事件监听
                GamepadKeyListener(
                    gamepadViewModel = gamepadViewModel,
                    isGrabbing = ZLBridgeStates.cursorMode == CURSOR_DISABLED,
                    onKeyEvent = { events, pressed ->
                        events.fastForEach { event ->
                            viewModel.onKeyEvent(event, pressed)
                        }
                    },
                    onAction = {
                        viewModel.switchControlLayer(HideLayerWhen.WhenGamepad)
                    }
                )

                //手柄摇杆控制移动事件监听
                GamepadStickMovementListener(
                    gamepadViewModel = gamepadViewModel,
                    isGrabbing = ZLBridgeStates.cursorMode == CURSOR_DISABLED,
                    onKeyEvent = { event, pressed ->
                        viewModel.onKeyEvent(event, pressed)
                    }
                )
            }

            ControlBoxLayout(
                modifier = Modifier.fillMaxSize(),
                observedLayout = viewModel.observableLayout,
                eventHandler = viewModel.eventHandler,
                checkOccupiedPointers = { viewModel.occupiedPointers.contains(it) },
                opacity = (AllSettings.controlsOpacity.state.toFloat() / 100f).coerceIn(0f, 1f),
                markPointerAsMoveOnly = { viewModel.moveOnlyPointers.add(it) },
                isCursorGrabbing = ZLBridgeStates.cursorMode == CURSOR_DISABLED,
                hideLayerWhen = viewModel.controlLayerHideState,
                isDark = isLauncherInDarkTheme()
            ) {
                val transformableState = rememberTransformableState { _, offsetChange, _ ->
                    incrementScreenOffset(offsetChange.copy(x = 0f)) //固定X坐标，只允许移动Y坐标
                }

                TopOverlayAboveIme(
                    content = {
                        MouseControlLayout(
                            isTouchProxyEnabled = isTouchProxyEnabled,
                            modifier = Modifier
                                .fillMaxSize()
                                .absoluteOffset(x = 0.dp, y = surfaceOffset.y.dp),
                            screenSize = screenSize,
                            onInputAreaRectUpdated = onInputAreaRectUpdated,
                            textInputMode = viewModel.textInputMode,
                            onCloseInputMethod = { viewModel.textInputMode = TextInputMode.DISABLE },
                            isMoveOnlyPointer = { viewModel.moveOnlyPointers.contains(it) },
                            onOccupiedPointer = { viewModel.occupiedPointers.add(it) },
                            onReleasePointer = {
                                viewModel.occupiedPointers.remove(it)
                                viewModel.moveOnlyPointers.remove(it)
                            },
                            onMouseMoved = { viewModel.switchControlLayer(HideLayerWhen.WhenMouse) },
                            onTouch = { viewModel.switchControlLayer(HideLayerWhen.None) },
                            gamepadViewModel = gamepadViewModel.takeIf { AllSettings.gamepadControl.state }
                        )

                        MinecraftHotbar(
                            screenSize = screenSize,
                            rule = AllSettings.hotbarRule.state,
                            widthPercentage = AllSettings.hotbarWidth.state.hotbarPercentage(),
                            heightPercentage = AllSettings.hotbarHeight.state.hotbarPercentage(),
                            onClickSlot = { keycode ->
                                CallbackBridge.sendKeyPress(keycode)
                            },
                            isGrabbing = isGrabbing,
                            resolutionRatio = AllSettings.resolutionRatio.state,
                            onOccupiedPointer = { viewModel.occupiedPointers.add(it) },
                            onReleasePointer = { viewModel.occupiedPointers.remove(it) }
                        )
                    },
                    emptyAreaContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .transformable(state = transformableState)
                        )
                    },
                    onAreaChanged = { show ->
                        if (!show) {
                            resetScreenOffset()
                        }
                    }
                )
            }

            if (AllSettings.gamepadControl.state) {
                //手柄事件捕获层
                SimpleGamepadCapture(
                    gamepadViewModel = gamepadViewModel
                )
            }
        }

        //陀螺仪控制
        val isGyroscopeAvailable = remember(context) {
            isGyroscopeAvailable(context = context)
        }
        if (isGrabbing && isGyroscopeAvailable && AllSettings.gyroscopeControl.state) {
            GyroscopeReader(
                xEvent = { delta ->
                    CallbackBridge.sendCursorDelta(if (AllSettings.gyroscopeInvertX.state) -delta else delta, 0f)
                },
                yEvent = { delta ->
                    CallbackBridge.sendCursorDelta(0f, if (AllSettings.gyroscopeInvertY.state) delta else -delta)
                },
                sampleRate = AllSettings.gyroscopeSampleRate.state,
                smoothing = AllSettings.gyroscopeSmoothing.state,
                smoothingWindow = AllSettings.gyroscopeSmoothingWindow.state,
                sensitivity = AllSettings.gyroscopeSensitivity.state / 100f
            )
        }

        LogBox(
            enableLog = !viewModel.isEditingLayout && logState.value,
            modifier = Modifier.fillMaxSize()
        )

        GameMenuSubscreen(
            state = viewModel.gameMenuState,
            controlMenuTabIndex = viewModel.controlMenuTabIndex,
            onControlMenuTabChange = { viewModel.controlMenuTabIndex = it },
            closeScreen = { viewModel.gameMenuState = MenuState.HIDE },
            onForceClose = { viewModel.forceCloseState = ForceCloseOperation.Show },
            onSwitchLog = { onLogStateChange(logState.next()) },
            enableTerracotta = AllSettings.enableTerracotta.state,
            onOpenTerracottaMenu = { terracottaViewModel.openMenu(activity!!) },
            onRefreshWindowSize = { eventViewModel.sendEvent(EventViewModel.Event.Game.RefreshSize) },
            onInputMethod = { viewModel.switchIME() },
            onSendKeycode = { viewModel.sendKeycodeState = SendKeycodeState.ShowDialog },
            onReplacementControl = { viewModel.replacementControlState = ReplacementControlState.Show },
            onEditLayout = {
                viewModel.startControlEditor(
                    editorVM = editorViewModel
                )
            }
        )

        if (viewModel.isEditingLayout) {
            viewModel.currentControlFile?.let {
                ControlEditor(
                    viewModel = editorViewModel,
                    targetFile = it,
                    exit = {
                        viewModel.exitControlEditor()
                    },
                    menuExit = {
                        editorViewModel.showExitEditorDialog(
                            context = context,
                            onExit = {
                                viewModel.exitControlEditor()
                            }
                        )
                    }
                )
            }
        } else {
            if (AllSettings.showMenuBall.state) {
                DraggableGameBall(
                    position = viewModel.gameBallPosition,
                    onPositionChanged = { viewModel.gameBallPosition = it },
                    showGameFps = AllSettings.showFPS.state,
                    showMemory = AllSettings.showMemory.state,
                    alpha = AllSettings.menuBallOpacity.state / 100f,
                    onClick = {
                        viewModel.switchMenu()
                    }
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        eventViewModel.events
            .filterIsInstance<EventViewModel.Event.Game>()
            .collect { event ->
                when (event) {
                    is EventViewModel.Event.Game.ShowIme -> {
                        viewModel.textInputMode = TextInputMode.ENABLE
                    }
                    is EventViewModel.Event.Game.OnBack -> {
                        if (viewModel.isEditingLayout) {
                            //处于控制布局编辑模式
                            editorViewModel.onBackPressed(
                                context = context,
                                onExit = {
                                    viewModel.exitControlEditor()
                                }
                            )
                        } else if (!AllSettings.showMenuBall.getValue()) {
                            viewModel.switchMenu()
                        } else {
                            //按下返回键
                            val events = viewModel.pressedKeyEvents
                            val escape = ControlEventKeycode.GLFW_KEY_ESCAPE
                            events.pressKey(escape)
                            delay(10)
                            events.releaseKey(escape)
                        }
                    }
                    is EventViewModel.Event.Game.OnResume -> {
                        viewModel.clearState()
                    }
                    else -> { /*忽略*/ }
                }
            }
    }
}

@Composable
private fun GameInfoBox(
    modifier: Modifier = Modifier,
    version: Version,
    isGameRendering: Boolean
) {
    AnimatedVisibility(
        visible = !isGameRendering,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        BackgroundCard(
            modifier = modifier,
            influencedByBackground = false,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterVertically)
                )

                //提示信息
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.game_loading),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.game_loading_version_name, version.getVersionName()),
                        style = MaterialTheme.typography.labelLarge
                    )
                    version.getVersionInfo()?.let { info ->
                        Text(
                            text = stringResource(R.string.game_loading_version_info, info.getInfoString()),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

/**
 * 鼠标控制层
 * @param isTouchProxyEnabled 是否启用控制代理（TouchController模组支持）
 * @param textInputMode 输入法状态
 * @param isMoveOnlyPointer 检查指针是否被标记为仅处理滑动事件
 * @param onOccupiedPointer 标记指针已被占用
 * @param onReleasePointer 标记指针已被释放
 * @param onMouseMoved 实体鼠标操作时回调
 * @param onTouch 手指触摸操作鼠标层时回调
 */
@Composable
private fun MouseControlLayout(
    isTouchProxyEnabled: Boolean,
    modifier: Modifier = Modifier,
    screenSize: IntSize,
    onInputAreaRectUpdated: (IntRect?) -> Unit,
    textInputMode: TextInputMode,
    onCloseInputMethod: () -> Unit,
    isMoveOnlyPointer: (PointerId) -> Boolean,
    onOccupiedPointer: (PointerId) -> Unit,
    onReleasePointer: (PointerId) -> Unit,
    onMouseMoved: () -> Unit,
    onTouch: () -> Unit,
    gamepadViewModel: GamepadViewModel?
) {
    Box(
        modifier = modifier
            .then(
                if (isTouchProxyEnabled) {
                    Modifier
                        .touchControllerTouchModifier()
                        .touchControllerInputModifier(
                            onInputAreaRectUpdated = onInputAreaRectUpdated,
                        )
                } else Modifier
            )
            .textInputHandler(
                mode = textInputMode,
                sender = LWJGLCharSender,
                onCloseInputMethod = onCloseInputMethod
            )
    ) {

        val capturedSpeedFactor = AllSettings.mouseCaptureSensitivity.state / 100f
        val capturedTapMouseAction = AllSettings.gestureTapMouseAction.state.toAction()
        val capturedLongPressMouseAction = AllSettings.gestureLongPressMouseAction.state.toAction()

        SwitchableMouseLayout(
            modifier = Modifier.fillMaxSize(),
            screenSize = screenSize,
            cursorMode = ZLBridgeStates.cursorMode,
            onTouch = onTouch,
            onMouse = onMouseMoved,
            gamepadViewModel = gamepadViewModel,
            onTap = { position ->
                CallbackBridge.putMouseEventWithCoords(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), position.x.sumPosition(), position.y.sumPosition())
            },
            onCapturedTap = {
                if (AllSettings.gestureControl.state) {
                    CallbackBridge.putMouseEvent(capturedTapMouseAction)
                }
            },
            onLongPress = {
                CallbackBridge.putMouseEvent(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), true)
            },
            onLongPressEnd = {
                CallbackBridge.putMouseEvent(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), false)
            },
            onCapturedLongPress = {
                if (AllSettings.gestureControl.state) {
                    CallbackBridge.putMouseEvent(capturedLongPressMouseAction, true)
                }
            },
            onCapturedLongPressEnd = {
                if (AllSettings.gestureControl.state) {
                    CallbackBridge.putMouseEvent(capturedLongPressMouseAction, false)
                }
            },
            onPointerMove = { pos ->
                pos.sendPosition()
            },
            onCapturedMove = { delta ->
                CallbackBridge.sendCursorDelta(
                    delta.x * capturedSpeedFactor,
                    delta.y * capturedSpeedFactor
                )
            },
            onMouseScroll = { scroll ->
                CallbackBridge.sendScroll(scroll.x.toDouble(), scroll.y.toDouble())
            },
            onMouseButton = { button, pressed ->
                val code = LWJGLCharSender.getMouseButton(button) ?: return@SwitchableMouseLayout
                CallbackBridge.sendMouseButton(code.toInt(), pressed)
            },
            isMoveOnlyPointer = isMoveOnlyPointer,
            onOccupiedPointer = onOccupiedPointer,
            onReleasePointer = onReleasePointer
        )
    }
}

private fun Offset.sendPosition() {
    CallbackBridge.sendCursorPos(x.sumPosition(), y.sumPosition())
}

private fun Float.sumPosition(): Float {
    return (this * (AllSettings.resolutionRatio.state / 100f))
}
