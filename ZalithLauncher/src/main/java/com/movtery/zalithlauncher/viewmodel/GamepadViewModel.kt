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

package com.movtery.zalithlauncher.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.movtery.zalithlauncher.ui.control.gamepad.DpadDirection
import com.movtery.zalithlauncher.ui.control.gamepad.GamepadMap
import com.movtery.zalithlauncher.ui.control.gamepad.GamepadMapping
import com.movtery.zalithlauncher.ui.control.gamepad.GamepadRemap
import com.movtery.zalithlauncher.ui.control.gamepad.Joystick
import com.movtery.zalithlauncher.ui.control.gamepad.JoystickType
import com.movtery.zalithlauncher.ui.control.gamepad.keyMappingMMKV

private const val BUTTON_PRESS_THRESHOLD = 0.85f

class GamepadViewModel() : ViewModel() {
    private val listeners = mutableListOf<(Event) -> Unit>()

    /**
     * 手柄与键盘按键映射绑定
     */
    private val allKeyMappings = mutableMapOf<Int, TargetKeys>()
    private val allDpadMappings = mutableMapOf<DpadDirection, TargetKeys>()

    /** 左摇杆状态 */
    private val leftJoystick = Joystick(JoystickType.Left)
    /** 右摇杆状态 */
    private val rightJoystick = Joystick(JoystickType.Right)

    private val dpadStates = mutableMapOf<DpadDirection, Boolean>()
    private val buttonStates = mutableMapOf<Int, Boolean>()

    /**
     * 手柄活动状态控制
     */
    var gamepadEngaged by mutableStateOf(false)
        private set

    private var lastActivityTime = System.nanoTime()
    private var pollLevel = PollLevel.Close

    init {
        reloadAllMappings()
    }

    /**
     * 检查并更新手柄是否活动中
     * @return 当前轮询频率等级
     */
    fun checkGamepadActive(): PollLevel {
        val now = System.nanoTime()

        if (
            dpadStates.containsValue(true) ||
            buttonStates.containsValue(true) ||
            leftJoystick.isUsing() ||
            rightJoystick.isUsing()
        ) {
            lastActivityTime = now
        }

        pollLevel = if (now - lastActivityTime < 10_000_000_000L) PollLevel.High else PollLevel.Close
        gamepadEngaged = pollLevel != PollLevel.Close

        return pollLevel
    }

    /** 激活状态更新 */
    private fun onActive() {
        val wasInactive = !gamepadEngaged
        lastActivityTime = System.nanoTime()
        if (wasInactive) {
            gamepadEngaged = true
            pollLevel = PollLevel.High
        }
    }

    fun reloadAllMappings() {
        allKeyMappings.clear()
        allDpadMappings.clear()

        val mmkv = keyMappingMMKV()
        GamepadMap.entries.forEach { entry ->
            val mapping = mmkv.decodeParcelable(entry.identifier, GamepadMapping::class.java)
                ?: GamepadMapping(
                    key = entry.gamepad,
                    dpadDirection = entry.dpadDirection,
                    targetsInGame = entry.defaultKeysInGame,
                    targetsInMenu = entry.defaultKeysInMenu
                )
            addInMappingsMap(mapping)
        }
    }

    private fun addInMappingsMap(mapping: GamepadMapping) {
        val target = TargetKeys(mapping.targetsInGame, mapping.targetsInMenu)
        mapping.dpadDirection?.let { allDpadMappings[it] = target } ?: run {
            allKeyMappings[mapping.key] = target
        }
    }

    /**
     * 重置手柄与键盘按键映射绑定
     */
    fun resetMapping(gamepadMap: GamepadMap, inGame: Boolean) =
        applyMapping(gamepadMap, inGame, useDefault = true)

    /**
     * 为指定手柄映射设置目标键盘映射
     */
    fun saveMapping(gamepadMap: GamepadMap, targets: Set<String>, inGame: Boolean) =
        applyMapping(gamepadMap, inGame, customTargets = targets)

    /**
     * 保存或重置手柄与键盘按键映射绑定
     * @param gamepadMap 手柄映射对象
     * @param inGame 是否为游戏内映射（true 为游戏内，false 为菜单内）
     * @param customTargets 自定义目标键
     * @param useDefault 是否使用默认按键
     */
    private fun applyMapping(
        gamepadMap: GamepadMap,
        inGame: Boolean,
        customTargets: Set<String>? = null,
        useDefault: Boolean = false
    ) {
        val dpad = gamepadMap.dpadDirection
        val existing = if (dpad != null) allDpadMappings[dpad] else allKeyMappings[gamepadMap.gamepad]

        val (targetsInGame, targetsInMenu) = if (inGame) {
            val newTargets = customTargets ?: if (useDefault) gamepadMap.defaultKeysInGame else emptySet()
            newTargets to (existing?.inMenu ?: emptySet())
        } else {
            (existing?.inGame ?: emptySet()) to (customTargets ?: if (useDefault) gamepadMap.defaultKeysInMenu else emptySet())
        }

        GamepadMapping(
            key = gamepadMap.gamepad,
            dpadDirection = dpad,
            targetsInGame = targetsInGame,
            targetsInMenu = targetsInMenu
        ).also { it.save(gamepadMap.identifier) }
    }

    private fun GamepadMapping.save(identifier: String) {
        addInMappingsMap(this)
        keyMappingMMKV().encode(identifier, this)
    }

    /**
     * 根据手柄按键键值获取对应的键盘映射代码
     * @return 若未找到，则返回null
     */
    fun findByCode(key: Int, inGame: Boolean) =
        allKeyMappings[key]?.getKeys(inGame)

    /**
     * 根据手柄方向键获取对应的键盘映射代码
     * @return 若未找到，则返回null
     */
    fun findByDpad(dir: DpadDirection, inGame: Boolean) =
        allDpadMappings[dir]?.getKeys(inGame)

    /**
     * 根据手柄映射获取对应的键盘映射代码
     * @return 若未找到，则返回null
     */
    fun findByMap(map: GamepadMap, inGame: Boolean) =
        (map.dpadDirection?.let { allDpadMappings[it] } ?: allKeyMappings[map.gamepad])
            ?.getKeys(inGame)

    fun updateButton(code: Int, pressed: Boolean) {
        onActive()
        if (updateState(buttonStates, code, pressed)) {
            sendEvent(Event.Button(code, pressed))
        }
    }

    fun updateMotion(axisCode: Int, value: Float) {
        onActive()
        when (axisCode) {
            //更新摇杆状态
            GamepadRemap.MotionX.code -> leftJoystick.updateState(horizontal = value)
            GamepadRemap.MotionY.code -> leftJoystick.updateState(vertical = value)
            GamepadRemap.MotionZ.code -> rightJoystick.updateState(horizontal = value)
            GamepadRemap.MotionRZ.code -> rightJoystick.updateState(vertical = value)
        }

        when (axisCode) {
            //更新左右触发器状态
            GamepadRemap.MotionLeftTrigger.code,
            GamepadRemap.MotionRightTrigger.code
                -> updateButton(axisCode, value > BUTTON_PRESS_THRESHOLD)

            //更新方向键状态
            GamepadRemap.MotionHatX.code -> {
                updateDpad(DpadDirection.Left, value < -BUTTON_PRESS_THRESHOLD)
                updateDpad(DpadDirection.Right, value > BUTTON_PRESS_THRESHOLD)
            }
            GamepadRemap.MotionHatY.code -> {
                updateDpad(DpadDirection.Up, value < -BUTTON_PRESS_THRESHOLD)
                updateDpad(DpadDirection.Down, value > BUTTON_PRESS_THRESHOLD)
            }
        }
    }

    private fun updateDpad(direction: DpadDirection, pressed: Boolean) {
        if (updateState(dpadStates, direction, pressed)) {
            sendEvent(Event.Dpad(direction, pressed))
        }
    }

    private fun <K> updateState(map: MutableMap<K, Boolean>, key: K, new: Boolean): Boolean {
        val old = map[key]
        return if (old != new) {
            map[key] = new
            true
        } else false
    }

    /**
     * 轮询调用，持续发送当前拥有的摇杆状态
     */
    fun pollJoystick() {
        leftJoystick.onTick(::sendEvent)
        rightJoystick.onTick(::sendEvent)
    }

    private fun sendEvent(event: Event) {
        listeners.forEach { listener ->
            listener(event)
        }
    }

    /**
     * 添加一个事件监听者，在事件发送时立即回调
     */
    fun addListener(listener: (Event) -> Unit) {
        listeners.add(listener)
    }

    /**
     * 移除已添加的事件监听者
     */
    fun removeListener(listener: (Event) -> Unit) {
        listeners.remove(listener)
    }

    /**
     * 便于记录目标键盘映射的数据类
     */
    private data class TargetKeys(
        val inGame: Set<String>,
        val inMenu: Set<String>
    ) {
        fun getKeys(isInGame: Boolean) = if (isInGame) inGame else inMenu
    }

    sealed interface Event {
        /**
         * 手柄按钮按下/松开事件
         * @param code 经过映射转化后的标准按钮键值
         */
        data class Button(val code: Int, val pressed: Boolean) : Event

        /**
         * 手柄摇杆偏移量事件
         * @param joystickType 摇杆类型（左、右）
         */
        data class StickOffset(val joystickType: JoystickType, val offset: Offset) : Event

        /**
         * 手柄摇杆方向变更事件
         * @param joystickType 摇杆类型（左、右）
         */
        data class StickDirection(val joystickType: JoystickType, val direction: Joystick.Direction) : Event

        /**
         * 手柄方向键按下/松开事件
         * @param direction 方向
         */
        data class Dpad(val direction: DpadDirection, val pressed: Boolean) : Event
    }

    enum class PollLevel(val delayMs: Long) {
        /**
         * 高轮询等级：16ms延迟 ≈ 60fps
         */
        High(16L),

        /**
         * 不进行轮询
         */
        Close(10_000L)
    }
}