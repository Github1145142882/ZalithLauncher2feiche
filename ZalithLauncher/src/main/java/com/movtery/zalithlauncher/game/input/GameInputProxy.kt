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

package com.movtery.zalithlauncher.game.input

import android.view.KeyEvent
import androidx.compose.ui.text.TextRange
import kotlin.math.min

/**
 * 这是一个代理方案，和其他启动器一样，通过一个输入框UI输入文本，对输入的字符串进行处理，最后发送字符给游戏，
 * 或者发送指定事件给游戏，以尽可能达到预期的输入效果
 */
class GameInputProxy(
    var sender: CharacterSenderStrategy
) {
    /**
     * 处理文本变化并发送到游戏
     * @param oldText 变化前的文本
     * @param newText 变化后的文本
     * @param oldSelection 变化前的选择范围
     * @param newSelection 变化后的选择范围
     */
    fun handleTextChange(
        oldText: String,
        newText: String,
        oldSelection: TextRange,
        newSelection: TextRange
    ) {
        when (
            val diff = calculateTextDifference(oldText, newText, oldSelection, newSelection)
        ) {
            is TextDifference.Insert -> handleInsert(diff)
            is TextDifference.Delete -> handleDelete(diff)
            //操作游戏内的指针，上面的插入与删除逻辑，都是建立在输入框的指针位置，与游戏内的指针位置一致的基础上实现的
            is TextDifference.MoveCursor -> handleCursorMove(diff)
        }
    }

    private fun handleInsert(diff: TextDifference.Insert) {
        diff.insertedText.forEach { char ->
            sender.sendChar(char)
        }
    }

    private fun handleDelete(diff: TextDifference.Delete) {
        repeat(diff.deletedText.length) {
            sender.sendBackspace()
        }
    }

    /**
     * 处理移动光标的逻辑，尝试发送方向键来模拟移动光标
     */
    private fun handleCursorMove(diff: TextDifference.MoveCursor) {
        val cursorDiff = diff.newPosition - diff.oldPosition
        
        if (cursorDiff > 0) {
            repeat(cursorDiff) {
                sender.sendRight()
            }
        } else if (cursorDiff < 0) {
            repeat(-cursorDiff) {
                sender.sendLeft()
            }
        }
    }

    /**
     * 返回这个按键事件是否允许被处理
     */
    fun keyCanHandle(keyEvent: KeyEvent): Boolean {
        val keyCode = keyEvent.keyCode
        //因为输入法选区时会发出Shift键的事件，但同步为游戏内的文本进行选区会比较复杂
        //比如选区时没法拿到当前输入框选择了哪些文本，极容易导致输入框与游戏内的文本出现状态差异
        //这类比较打破预期的情况应该尽量避免，所以应该忽略Shift
        return !(keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT)
    }
    
    /**
     * 处理特殊按键
     */
    fun handleSpecialKey(keyEvent: KeyEvent) {
        when (keyEvent.keyCode) {
            KeyEvent.KEYCODE_ENTER -> sender.sendEnter()
            KeyEvent.KEYCODE_TAB -> sender.sendTab()

            KeyEvent.KEYCODE_DEL,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                //已经在处理文本差异时尝试过发送按键了，但是可能需要考虑到这类情况
                //文本框本身没有文本，但是游戏内的输入框还有文本
                //所以应该继续发送退格键，但不应该在这里进行处理
            }

            KeyEvent.KEYCODE_DPAD_UP -> sender.sendUp()
            KeyEvent.KEYCODE_DPAD_DOWN -> sender.sendDown()

            else -> sender.sendOther(keyEvent)
        }
    }

    /**
     * 需要考虑到文本框是否有文本的特殊按键处理
     * 有时候文本框没有文本，但还是允许对游戏发送按键
     */
    fun handleSpecialKey(keyEvent: KeyEvent, text: CharSequence): Boolean {
        if (text.isEmpty()) {
            when (keyEvent.keyCode) {
                KeyEvent.KEYCODE_DEL -> sender.sendBackspace()
                KeyEvent.KEYCODE_DPAD_LEFT -> sender.sendLeft()
                KeyEvent.KEYCODE_DPAD_RIGHT -> sender.sendRight()
                else -> return false
            }
            return true
        }
        return false
    }
}

/**
 * 文本差异类型
 */
sealed class TextDifference {
    data class Insert(
        val insertedText: String
    ) : TextDifference()

    data class Delete(
        val deletedText: String
    ) : TextDifference()

    data class MoveCursor(
        val oldPosition: Int,
        val newPosition: Int
    ) : TextDifference()
}

/**
 * 计算文本差异
 */
fun calculateTextDifference(
    oldText: String,
    newText: String,
    oldSelection: TextRange,
    newSelection: TextRange
): TextDifference {
    //如果文本相同，则判断只有光标移动
    //让游戏同步光标状态（发送左右方向键实现）
    if (oldText == newText) {
        return TextDifference.MoveCursor(
            oldPosition = oldSelection.start,
            newPosition = newSelection.start
        )
    }

    val oldLength = oldText.length
    val newLength = newText.length

    // 当有选区时，删除操作应该只删除选区内的文本
    if (!oldSelection.collapsed) {
        // 选区范围
        val selectionStart = oldSelection.min
        val selectionEnd = oldSelection.max

        // 检查是否是删除选区内容
        if (newLength == oldLength - (selectionEnd - selectionStart)) {
            // 构造删除选区后的预期文本
            val expectedText = oldText.take(selectionStart) + oldText.substring(selectionEnd)

            if (newText == expectedText) {
                return TextDifference.Delete(
                    deletedText = oldText.substring(selectionStart, selectionEnd)
                )
            }
        }
    }

    // 检查是否是在光标位置插入
    if (newLength > oldLength) {
        // 尝试找到插入的位置
        // 最简单的情况：在光标位置插入
        val cursorPos = oldSelection.start

        // 检查是否是在光标位置插入
        val beforeCursor = oldText.take(cursorPos)
        val afterCursor = oldText.substring(cursorPos)

        // 新文本应该以 beforeCursor 开头，以 afterCursor 结尾
        if (newText.startsWith(beforeCursor) && newText.endsWith(afterCursor)) {
            val insertedText = newText.substring(
                beforeCursor.length,
                newLength - afterCursor.length
            )

            return TextDifference.Insert(
                insertedText = insertedText
            )
        }
    }

    // 检查是否是在光标位置删除
    if (newLength < oldLength) {
        val cursorPos = oldSelection.start

        // 光标位置可能是删除的结束位置
        // 简单逻辑：如果新文本是旧文本的前缀，那么是从末尾删除
        if (newText == oldText.take(newLength)) {
            return TextDifference.Delete(
                deletedText = oldText.substring(newLength)
            )
        }

        // 检查是否删除光标前的字符
        if (cursorPos in 1..oldLength) {
            // 尝试在光标前删除一个字符
            val potentialDeleted = oldText.take(cursorPos - 1) + oldText.substring(cursorPos)
            if (potentialDeleted == newText) {
                return TextDifference.Delete(
                    deletedText = oldText.substring(cursorPos - 1, cursorPos)
                )
            }
        }
    }

    // 如果光标位置没变，我们尝试分析差异
    if (oldSelection.start == newSelection.start) {
        // 寻找第一个不同的字符
        val minLength = min(oldLength, newLength)
        var firstDiffIndex = -1
        for (i in 0 until minLength) {
            if (oldText[i] != newText[i]) {
                firstDiffIndex = i
                break
            }
        }

        if (firstDiffIndex != -1) {
            return TextDifference.Delete(
                deletedText = oldText.substring(firstDiffIndex)
            )
        }
    }

    return TextDifference.Insert(
        insertedText = newText
    )
}