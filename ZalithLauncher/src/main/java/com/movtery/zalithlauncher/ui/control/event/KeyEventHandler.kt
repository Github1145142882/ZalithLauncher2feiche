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

package com.movtery.zalithlauncher.ui.control.event

/**
 * 处理启动器按键事件标识
 */
class KeyEventHandler(
    private val handle: (key: String, pressed: Boolean) -> Unit
) {
    /**
     * 当前按键总共按住的数量（也许有同一个按键同时按下的情况）
     */
    private val keyEvents = mutableMapOf<String, Int>()

    /**
     * 按下按键
     */
    fun pressKey(key: String) {
        val count = keyEvents[key] ?: 0
        keyEvents[key] = count + 1
        handle(key)
    }

    fun releaseKey(key: String) {
        val count = keyEvents[key] ?: 0
        keyEvents[key] = count - 1
        handle(key)
    }

    /**
     * 清除所有按键事件
     */
    fun clearEvent() {
        keyEvents.replaceAll { _, _ -> 0 }
        handle()
    }

    private fun handle(primaryKey: String? = null) {
        val entries = keyEvents.entries.toList()
        entries.forEach { (key, count) ->
            val pressed = when (count) {
                1 -> true
                0 -> {
                    keyEvents.remove(key)
                    false
                }
                else -> {
                    if (count < 0) {
                        keyEvents.remove(key)
                        false
                    } else {
                        return@forEach
                    }
                }
            }
            if (pressed) {
                if (key == primaryKey) {
                    handle(key, true)
                }
            } else {
                handle(key, false)
            }
        }
    }
}