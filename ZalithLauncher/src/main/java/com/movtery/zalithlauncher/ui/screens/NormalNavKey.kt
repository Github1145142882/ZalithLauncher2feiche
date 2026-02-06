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

package com.movtery.zalithlauncher.ui.screens

import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import kotlinx.serialization.Serializable

/**
 * 普通的屏幕
 */
sealed interface NormalNavKey : NavKey {
    /** 解压依赖内容屏幕（启动屏幕） */
    @Serializable data object UnpackDeps: NormalNavKey
    /** 启动器主页屏幕 */
    @Serializable data object LauncherMain : NormalNavKey
    /** 账号管理屏幕 */
    @Serializable data object AccountManager : NormalNavKey
    /** Web屏幕 */
    @Serializable data class WebScreen(val url: String) : NormalNavKey
    /** 版本管理屏幕 */
    @Serializable data object VersionsManager : NormalNavKey
    /** 文件选择屏幕 */
    @Serializable data class FileSelector(
        val startPath: String,
        val selectFile: Boolean,
        val saveKey: NavKey,
        val onSelected: (path: String) -> Unit
    ) : NormalNavKey
    /** 多人联机屏幕 */
    @Serializable data object Multiplayer: NormalNavKey

    /** 设置嵌套子屏幕 */
    sealed interface Settings : NormalNavKey {
        /** 渲染器设置屏幕 */
        @Serializable data object Renderer : Settings
        /** 游戏设置屏幕 */
        @Serializable data object Game : Settings
        /** 控制设置屏幕 */
        @Serializable data object Control : Settings
        /** 手柄设置屏幕 */
        @Serializable data object Gamepad : Settings
        /** 启动器设置屏幕 */
        @Serializable data object Launcher : Settings
        /** Java管理屏幕 */
        @Serializable data object JavaManager : Settings
        /** 控制管理屏幕 */
        @Serializable data object ControlManager : Settings
        /** 关于屏幕 */
        @Serializable data object AboutInfo : Settings
    }

    /** 版本详细设置嵌套子屏幕 */
    sealed interface Versions : NormalNavKey {
        /** 版本概览屏幕 */
        @Serializable data object OverView : Versions
        /** 版本配置屏幕 */
        @Serializable data object Config : Versions
        /** 更新版本的模组加载器 */
        @Serializable data object UpdateLoader : Versions
        /** 模组管理屏幕 */
        @Serializable data object ModsManager : Versions
        /** 存档管理屏幕 */
        @Serializable data object SavesManager : Versions
        /** 资源包管理屏幕 */
        @Serializable data object ResourcePackManager : Versions
        /** 光影包管理屏幕 */
        @Serializable data object ShadersManager : Versions
        /** 服务器列表屏幕 */
        @Serializable data object ServerList : Versions
    }

    /** 下载游戏嵌套子屏幕 */
    sealed interface DownloadGame : NormalNavKey {
        /** 选择游戏版本屏幕 */
        @Serializable data object SelectGameVersion : Versions
        /** 选择附加内容屏幕 */
        @Serializable data class Addons(val gameVersion: String) : Versions
    }

    /** 搜索整合包屏幕 */
    @Serializable data object SearchModPack : NormalNavKey
    /** 搜索模组屏幕 */
    @Serializable data object SearchMod : NormalNavKey
    /** 搜索资源包屏幕 */
    @Serializable data object SearchResourcePack : NormalNavKey
    /** 搜索存档屏幕 */
    @Serializable data object SearchSaves : NormalNavKey
    /** 搜索光影包屏幕 */
    @Serializable data object SearchShaders : NormalNavKey

    /** 下载资源屏幕 */
    @Serializable data class DownloadAssets(
        val platform: Platform,
        val projectId: String,
        val classes: PlatformClasses,
        val iconUrl: String? = null
    ) : NormalNavKey

    /** 协议展示屏幕 */
    @Serializable data class License(
        val raw: Int
    ): NormalNavKey
}