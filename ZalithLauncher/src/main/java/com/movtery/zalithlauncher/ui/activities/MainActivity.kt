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

package com.movtery.zalithlauncher.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.movtery.layer_controller.layout.BackdropBlurConfig
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.control.ControlManager
import com.movtery.zalithlauncher.notification.NotificationManager
import com.movtery.zalithlauncher.path.URL_SUPPORT
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.base.BaseAppCompatActivity
import com.movtery.zalithlauncher.ui.backdrop.BackdropCaptureSource
import com.movtery.zalithlauncher.ui.backdrop.BackdropFrameSampler
import com.movtery.zalithlauncher.ui.backdrop.BackdropSamplingProfile
import com.movtery.zalithlauncher.ui.components.LauncherSafeArea
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.Background
import com.movtery.zalithlauncher.ui.screens.content.elements.LaunchGameOperation
import com.movtery.zalithlauncher.ui.screens.main.MainScreen
import com.movtery.zalithlauncher.ui.theme.ZalithLauncherTheme
import com.movtery.zalithlauncher.upgrade.TooFrequentOperationException
import com.movtery.zalithlauncher.utils.logging.Logger.lInfo
import com.movtery.zalithlauncher.utils.network.openLink
import com.movtery.zalithlauncher.viewmodel.BackgroundViewModel
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.LaunchGameViewModel
import com.movtery.zalithlauncher.viewmodel.LauncherUpgradeOperation
import com.movtery.zalithlauncher.viewmodel.LauncherUpgradeViewModel
import com.movtery.zalithlauncher.viewmodel.ModpackConfirmUseMobileDataOperation
import com.movtery.zalithlauncher.viewmodel.ModpackImportOperation
import com.movtery.zalithlauncher.viewmodel.ModpackImportViewModel
import com.movtery.zalithlauncher.viewmodel.ModpackVersionNameOperation
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : BaseAppCompatActivity() {
    /**
     * 屏幕堆栈管理ViewModel
     */
    private val screenBackStackModel: ScreenBackStackViewModel by viewModels()

    /**
     * 启动游戏ViewModel
     */
    private val launchGameViewModel: LaunchGameViewModel by viewModels()

    /**
     * 错误信息ViewModel
     */
    private val errorViewModel: ErrorViewModel by viewModels()

    /**
     * 与Compose交互的事件ViewModel
     */
    val eventViewModel: EventViewModel by viewModels()

    /**
     * 启动器背景内容管理 ViewModel
     */
    val backgroundViewModel: BackgroundViewModel by viewModels()

    /**
     * 整合包导入 ViewModel
     */
    val modpackImportViewModel: ModpackImportViewModel by viewModels()

    /**
     * 启动器更新状态 ViewModel
     */
    val launcherUpgradeViewModel: LauncherUpgradeViewModel by viewModels()

    /**
     * 是否开启捕获按键模式
     */
    private var isCaptureKey = false
    private val launcherBackdropSampler by lazy {
        BackdropFrameSampler(
            refreshRateProvider = { display?.refreshRate ?: 60f }
        )
    }
    private var launcherBackdropSource: BackdropCaptureSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //初始化通知管理（创建渠道）
        NotificationManager.initManager(this)

        //处理外部导入
        val isImporting = handleImportIfNeeded(intent)

        //检查更新
        if (!isImporting && launcherUpgradeViewModel.operation == LauncherUpgradeOperation.None) {
            lifecycleScope.launch {
                launcherUpgradeViewModel.checkOnAppStart()
            }
        }

        //错误信息展示
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                errorViewModel.errorEvents.collect { tm ->
                    errorViewModel.showErrorDialog(
                        context = this@MainActivity,
                        tm = tm
                    )
                }
            }
        }

        //事件处理
        lifecycleScope.launch {
            eventViewModel.events.collect { event ->
                when (event) {
                    is EventViewModel.Event.Key.StartKeyCapture -> {
                        lInfo("Start key capture!")
                        isCaptureKey = true
                    }
                    is EventViewModel.Event.Key.StopKeyCapture -> {
                        lInfo("Stop key capture!")
                        isCaptureKey = false
                    }
                    is EventViewModel.Event.OpenLink -> {
                        val url = event.url
                        lifecycleScope.launch(Dispatchers.Main) {
                            this@MainActivity.openLink(url)
                        }
                    }
                    is EventViewModel.Event.RefreshFullScreen -> {
                        lifecycleScope.launch(Dispatchers.Main) {
                            refreshWindow()
                        }
                    }
                    is EventViewModel.Event.CheckUpdate -> {
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val success = launcherUpgradeViewModel.checkManually(
                                    onInProgress = {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(this@MainActivity, getString(R.string.generic_in_progress), Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onIsLatest = {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(this@MainActivity, getString(R.string.upgrade_is_latest), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                if (!success) throw RuntimeException()
                            } catch (_: TooFrequentOperationException) {
                                //太频繁了
                                return@launch
                            } catch (_: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@MainActivity, getString(R.string.upgrade_get_remote_failed), Toast.LENGTH_SHORT).show()
                                }
                                return@launch
                            }
                        }
                    }
                    is EventViewModel.Event.KeepScreen -> {
                        keepScreen(event.on)
                    }
                    else -> {
                        //忽略
                    }
                }
            }
        }

        val finishedGame = AllSettings.finishedGame
        val showSponsorship = AllSettings.showSponsorship

        setContent {
            ZalithLauncherTheme(
                applyLauncherSafeArea = false,
                backgroundViewModel = backgroundViewModel
            ) {
                LauncherSafeArea(
                    applyContentPadding = false,
                    drawBackground = false
                ) {
                    val launcherBackdropFrame by launcherBackdropSampler.frameFlow.collectAsStateWithLifecycle()
                    val blurRadius = AllSettings.launcherComponentsBackdropBlurRadius.state.dp
                    val blurEnabled = backgroundViewModel.isValid && blurRadius > 0.dp

                    val launcherBackdropConfig = remember(
                        launcherBackdropFrame?.frameVersion,
                        blurRadius,
                        blurEnabled
                    ) {
                        if (!blurEnabled) {
                            null
                        } else {
                            launcherBackdropFrame?.let { frame ->
                                BackdropBlurConfig(
                                    frame = frame.frame,
                                    sourceWidth = frame.sourceWidth,
                                    sourceHeight = frame.sourceHeight,
                                    captureWidth = frame.captureWidth,
                                    captureHeight = frame.captureHeight,
                                    radiusDp = blurRadius
                                )
                            }
                        }
                    }
                    val launcherBackdropActive = blurEnabled &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        launcherBackdropConfig != null

                    LaunchedEffect(blurEnabled, AllSettings.launcherComponentsBackdropBlurSampleFps.state) {
                        if (!blurEnabled) {
                            launcherBackdropSampler.clear(recycleBuffers = false)
                        } else {
                            captureLauncherBackdrop(force = true)
                        }
                    }

                    // 视频背景下主动按采样率轮询抓帧，避免仅依赖回调导致毛玻璃静止。
                    LaunchedEffect(
                        blurEnabled,
                        backgroundViewModel.isVideo,
                        AllSettings.launcherComponentsBackdropBlurSampleFps.state
                    ) {
                        if (!blurEnabled || !backgroundViewModel.isVideo) return@LaunchedEffect
                        while (true) {
                            captureLauncherBackdrop(force = false)
                            val fps = AllSettings.launcherComponentsBackdropBlurSampleFps.state.coerceIn(30, 120)
                            delay((1000L / fps).coerceAtLeast(8L))
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface)
                        )

                        Background(
                            modifier = Modifier.fillMaxSize(),
                            viewModel = backgroundViewModel,
                            onCaptureSourceChanged = { source ->
                                updateLauncherBackdropSource(source)
                            },
                            onFrameAvailable = {
                                captureLauncherBackdrop(force = false)
                            }
                        )

                        MainScreen(
                            screenBackStackModel = screenBackStackModel,
                            launchGameViewModel = launchGameViewModel,
                            eventViewModel = eventViewModel,
                            modpackImportViewModel = modpackImportViewModel,
                            launcherBackdropConfig = launcherBackdropConfig,
                            launcherBackdropEnabled = launcherBackdropActive,
                            submitError = {
                                errorViewModel.showError(it)
                            }
                        )

                        //启动游戏操作流程
                        LaunchGameOperation(
                            activity = this@MainActivity,
                            launchGameOperation = launchGameViewModel.launchGameOperation,
                            updateOperation = { launchGameViewModel.updateOperation(it) },
                            exitActivity = {
                                this@MainActivity.finish()
                            },
                            submitError = {
                                errorViewModel.showError(it)
                            },
                            toAccountManageScreen = {
                                screenBackStackModel.mainScreen.navigateTo(NormalNavKey.AccountManager)
                            },
                            toVersionManageScreen = {
                                screenBackStackModel.mainScreen.removeAndNavigateTo(
                                    remove = NestedNavKey.VersionSettings::class,
                                    screenKey = NormalNavKey.VersionsManager
                                )
                            }
                        )
                    }
                }

                //显示赞助支持的小弹窗
                if (!isImporting && finishedGame.state >= 100 && showSponsorship.state) {
                    SimpleAlertDialog(
                        title = stringResource(R.string.about_sponsor),
                        text = stringResource(R.string.game_saponsorship_finished_game, finishedGame.state),
                        dismissText = stringResource(R.string.generic_close),
                        onDismiss = {
                            showSponsorship.save(false)
                        },
                        onConfirm = {
                            showSponsorship.save(false)
                            eventViewModel.sendEvent(
                                EventViewModel.Event.OpenLink(URL_SUPPORT)
                            )
                        }
                    )
                }

                ModpackImportOperation(
                    operation = modpackImportViewModel.importOperation,
                    changeOperation = { modpackImportViewModel.importOperation = it },
                    importer = modpackImportViewModel.importer,
                    onCancel = {
                        modpackImportViewModel.cancel()
                        lifecycleScope.launch {
                            keepScreen(false)
                        }
                    }
                )

                //用户确认版本名称 操作流程
                ModpackVersionNameOperation(
                    operation = modpackImportViewModel.versionNameOperation,
                    onConfirmVersionName = { name ->
                        modpackImportViewModel.confirmVersionName(name)
                    }
                )

                //用户确认使用移动网络 操作流程
                ModpackConfirmUseMobileDataOperation(
                    operation = modpackImportViewModel.confirmMobileDataOperation,
                    onConfirmUse = { use ->
                        modpackImportViewModel.confirmUseMobileData(use)
                    }
                )

                //检查更新操作流程
                LauncherUpgradeOperation(
                    operation = launcherUpgradeViewModel.operation,
                    onChanged = { launcherUpgradeViewModel.operation = it },
                    onIgnoredClick = { ver ->
                        AllSettings.lastIgnoredVersion.save(ver)
                    },
                    onLinkClick = { eventViewModel.sendEvent(EventViewModel.Event.OpenLink(it)) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleImportIfNeeded(intent)
    }

    /**
     * 是否保持屏幕不熄屏
     */
    private suspend fun keepScreen(on: Boolean) {
        withContext(Dispatchers.Main) {
            window?.apply {
                if (on) {
                    addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }
    }

    /**
     * 处理外部导入
     * @return 是否有导入任务正在进行中
     */
    private fun handleImportIfNeeded(intent: Intent?): Boolean {
        if (intent == null) return false

        val type = intent.getStringExtra(EXTRA_IMPORT_TYPE) ?: return false

        val importing = when (type) {
            IMPORT_TYPE_MODPACK -> handleModpackImport(intent)
            else -> false
        }

        intent.removeExtra(EXTRA_IMPORT_TYPE)
        return importing
    }

    /**
     * @return 是否已经触发了整合包导入程序
     */
    private fun handleModpackImport(intent: Intent): Boolean {
        val uri: Uri? = intent.getParcelableExtra(EXTRA_IMPORT_URI)
        if (uri != null) {
            modpackImportViewModel.import(
                context = this@MainActivity,
                uri = uri,
                onStart = {
                    lifecycleScope.launch {
                        keepScreen(true)
                    }
                },
                onStop = {
                    lifecycleScope.launch {
                        keepScreen(false)
                    }
                }
            )
        }
        return uri != null
    }

    private fun updateLauncherBackdropSource(source: BackdropCaptureSource?) {
        if (launcherBackdropSource === source) return
        launcherBackdropSource?.release()
        launcherBackdropSource = source
        launcherBackdropSampler.clear(recycleBuffers = false)
        captureLauncherBackdrop(force = true)
    }

    private fun captureLauncherBackdrop(force: Boolean) {
        if (!backgroundViewModel.isValid) {
            launcherBackdropSampler.clear(recycleBuffers = false)
            return
        }
        if (AllSettings.launcherComponentsBackdropBlurRadius.state <= 0) {
            launcherBackdropSampler.clear(recycleBuffers = false)
            return
        }
        launcherBackdropSampler.requestCapture(
            source = launcherBackdropSource,
            userFps = AllSettings.launcherComponentsBackdropBlurSampleFps.state,
            blurRadius = AllSettings.launcherComponentsBackdropBlurRadius.state,
            profile = BackdropSamplingProfile.LAUNCHER_BALANCED,
            force = force
        )
    }

    override fun onResume() {
        super.onResume()
        ControlManager.checkDefaultAndRefresh(this@MainActivity)
        captureLauncherBackdrop(force = true)
    }

    override fun onPause() {
        super.onPause()
        launcherBackdropSampler.clear(recycleBuffers = false)
    }

    override fun onDestroy() {
        launcherBackdropSource?.release()
        launcherBackdropSource = null
        launcherBackdropSampler.clear(recycleBuffers = true)
        super.onDestroy()
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (isCaptureKey) {
            lInfo("Capture key event: $event")
            eventViewModel.sendEvent(EventViewModel.Event.Key.OnKeyDown(event))
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
