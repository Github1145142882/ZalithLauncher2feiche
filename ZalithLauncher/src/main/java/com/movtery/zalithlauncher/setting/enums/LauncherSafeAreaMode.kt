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

package com.movtery.zalithlauncher.setting.enums

import androidx.annotation.StringRes
import com.movtery.zalithlauncher.R

/**
 * 启动器页面安全区处理模式
 */
enum class LauncherSafeAreaMode(
    @param:StringRes val textRes: Int
) {
    /**
     * 跟随系统安全区
     */
    FOLLOW_SYSTEM(R.string.settings_launcher_safe_area_mode_follow_system),

    /**
     * 忽略系统安全区
     */
    IGNORE(R.string.settings_launcher_safe_area_mode_ignore),

    /**
     * 系统安全区 + 自定义补偿边距
     */
    CUSTOM(R.string.settings_launcher_safe_area_mode_custom)
}
