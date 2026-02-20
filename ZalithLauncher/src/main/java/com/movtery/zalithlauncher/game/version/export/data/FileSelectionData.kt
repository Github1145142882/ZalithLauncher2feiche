package com.movtery.zalithlauncher.game.version.export.data

import com.movtery.zalithlauncher.utils.string.compareChar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 可选择的文件树
 * @param alias 当前节点的别称，安卓字符串资源
 * @param child 如果有子节点（不为null），则当前节点为文件夹目录
 *              如果没有子节点（null），则当前节点为文件
 */
class FileSelectionData(
    val file: File,
    val alias: Int? = null,
    val child: List<FileSelectionData>? = null
): Comparable<FileSelectionData> {
    private val _selected = MutableStateFlow(Selected.Unselected)
    /** 当前节点的选中状态 */
    val selected = _selected.asStateFlow()

    private val _expand = MutableStateFlow(false)
    /** 当前节点的展开状态（文件夹节点） */
    val expand = _expand.asStateFlow()

    /**
     * 更新这个节点的选中状态
     */
    fun updateSelectState(new: Selected) {
        if (new == Selected.Indeterminate) {
            error("File node cannot be set to \"Indeterminate\" selection state")
        }

        if (child != null && child.isEmpty()) {
            //子节点为空时，不允许选择
            recursionSelect(Selected.Unselected)
        } else {
            recursionSelect(new)
        }
    }

    private fun recursionSelect(new: Selected) {
        _selected.update { new }

        //更新子节点的选中状态
        child?.forEach { childNode ->
            if (childNode.child != null && childNode.child.isEmpty()) {
                //子节点为空时，不允许选择
                childNode.recursionSelect(Selected.Unselected)
            } else {
                childNode.recursionSelect(new)
            }
        }
    }

    /**
     * 展开/收起当前节点，收起时同时应用到子节点
     */
    fun expandDirs(state: Boolean) {
        _expand.update { state }
        if (!state && child != null) {
            closeDirs(this)
        }
    }

    private fun closeDirs(node: FileSelectionData) {
        node._expand.update { false }
        node.child?.forEach { childNode ->
            closeDirs(childNode)
        }
    }

    /**
     * 刷新文件夹根节点的选中状态
     * @return 当前选中了多少个文件
     */
    suspend fun refreshRootState(): Int {
        var count = if (_selected.value == Selected.Selected) 1 else 0
        if (child != null) {
            withContext(Dispatchers.Default) {
                //是文件夹节点
                val totalCount = collectCount { true }
                val selectedCount = collectCount { it._selected.value == Selected.Selected }

                count += selectedCount

                withContext(Dispatchers.Main) {
                    if (totalCount == 0) {
                        //子节点为空时，不允许选择
                        _selected.update { Selected.Unselected }
                    } else {
                        _selected.update {
                            if (selectedCount <= 0) {
                                Selected.Unselected
                            } else if (selectedCount in 1..<totalCount) {
                                Selected.Indeterminate
                            } else {
                                Selected.Selected
                            }
                        }
                    }
                }
            }
        }

        return count
    }

    private fun collectCount(
        rule: (FileSelectionData) -> Boolean
    ): Int {
        var count = 0
        if (child == null) {
            if (rule(this)) count++
        } else {
            child.forEach { node ->
                if (node.child == null && rule(node)) {
                    count++
                } else node.child?.forEach { childNode ->
                    val count0 = childNode.collectCount(rule)
                    count += count0
                }
            }
        }

        return count
    }

    override fun compareTo(other: FileSelectionData): Int {
        val thisIsFile = isFile()
        val otherIsFile = other.isFile()

        return if (!thisIsFile && otherIsFile) -1
        else if (thisIsFile && !otherIsFile) 1
        else compareChar(file.name, other.file.name)
    }
}

fun FileSelectionData.isFile(): Boolean = child == null

/**
 * 以递归的方式，获取所有节点所有选中的文件
 */
fun List<FileSelectionData>.getSelectedFiles(): List<File> {
    return asSequence()
        .flatMap { node ->
            when {
                node.child == null && node.selected.value == Selected.Selected -> sequenceOf(node.file)
                !node.child.isNullOrEmpty() -> node.child.getSelectedFiles().asSequence()
                else -> emptySequence()
            }
        }
        .toList()
}