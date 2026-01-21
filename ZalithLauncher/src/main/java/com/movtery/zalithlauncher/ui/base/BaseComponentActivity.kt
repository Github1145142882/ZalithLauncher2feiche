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

package com.movtery.zalithlauncher.ui.base

import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.annotation.CallSuper
import com.movtery.zalithlauncher.context.refreshContext
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.path.GamePathManager
import com.movtery.zalithlauncher.game.plugin.PluginLoader
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.loadAllSettings
import com.movtery.zalithlauncher.utils.checkStoragePermissionsForInit
import com.movtery.zalithlauncher.utils.logging.Logger.lInfo
import kotlin.math.min

open class BaseComponentActivity(
    /** 是否刷新数据 */
    private val refreshData: Boolean = true
) : FullScreenComponentActivity() {
    private var notchSize = -1

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        refreshContext(this)
        checkStoragePermissions()

        if (refreshData) {
            //加载渲染器
            Renderers.init()
            //加载插件
            PluginLoader.loadAllPlugins(this, false)
            //刷新其他内容
            refreshData()
        }
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        loadAllSettings(this, true)
        checkStoragePermissions()
    }

    override fun onAttachedToWindow() {
        computeNotchSize()
    }

    override fun getWindowMode(): WindowMode {
        runCatching {
            return if (AllSettings.launcherFullScreen.getValue()) {
                WindowMode.FULL_IMMERSIVE
            } else {
                WindowMode.DEFAULT
            }
        }
        //AllSettings初始化出现异常（MMKV在Application未正常初始化）
        //不出意外应该正在展示FatalErrorActivity，忽略并关闭当前Activity
        finish()
        return WindowMode.DEFAULT
    }

    private fun refreshData() {
        AccountsManager.reloadAccounts()
        AccountsManager.reloadAuthServers()
        GamePathManager.reloadPath()
    }

    private fun checkStoragePermissions() {
        //检查所有文件管理权限
        checkStoragePermissionsForInit(this)
    }

    /**
     * [Modified from PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/blob/a6f3fc0/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/Tools.java#L598-L620)
     */
    @Suppress("DEPRECATION")
    fun getDisplayMetrics(): DisplayMetrics {
        var displayMetrics = DisplayMetrics()
        if (isInMultiWindowMode || isInPictureInPictureMode) {
            displayMetrics = resources.displayMetrics
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display.getRealMetrics(displayMetrics)
            } else {
                windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            }
            if (getWindowMode() == WindowMode.DEFAULT) {
                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) displayMetrics.heightPixels -= notchSize
                else displayMetrics.widthPixels -= notchSize
            }
        }
        return displayMetrics
    }

    /**
     * Compute the notch size to avoid being out of bounds
     * [Modified from PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/blob/5de6822/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/prefs/LauncherPreferences.java#L196-L219)
     */
    private fun computeNotchSize() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            notchSize = -1
            return
        }

        runCatching {
            val displayCutout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                windowManager.currentWindowMetrics.windowInsets.displayCutout
            } else {
                window.decorView.rootWindowInsets.displayCutout
            }

            if (displayCutout == null || displayCutout.boundingRects.isEmpty()) {
                notchSize = -1
                return@runCatching
            }

            var cutout = Rect()
            for (rect in displayCutout.boundingRects) {
                if (rect.width() * rect.height() > cutout.width() * cutout.height()) {
                    cutout = rect
                }
            }

            // Notch values are rotation sensitive, handle all cases
            val orientation: Int = resources.configuration.orientation
            notchSize = when (orientation) {
                Configuration.ORIENTATION_PORTRAIT -> cutout.height()
                Configuration.ORIENTATION_LANDSCAPE -> cutout.width()
                else -> min(cutout.width(), cutout.height())
            }
        }.onFailure {
            lInfo("No notch detected, or the device if in split screen mode")
            notchSize = -1
        }
    }

    protected fun runFinish() = run { finish() }
}