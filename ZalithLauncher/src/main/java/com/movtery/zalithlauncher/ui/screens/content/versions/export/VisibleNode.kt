package com.movtery.zalithlauncher.ui.screens.content.versions.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.game.version.export.data.FileSelectionData
import kotlinx.coroutines.flow.StateFlow

sealed interface VisibleNode {
    data class FileNode(
        val data: FileSelectionData,
        private val indentation0: Int
    ) : VisibleNode {
        override val indentation: Int get() = indentation0
        override val key: String get() = data.file.absolutePath
    }

    /** 仅展示空目录提示文本 */
    data class EmptyNode(
        private val key0: String,
        private val indentation0: Int,
    ) : VisibleNode {
        override val indentation: Int get() = indentation0
        override val key: String get() = key0
    }

    val indentation: Int
    val key: String
}

@Composable
fun rememberVisibleNodes(
    list: List<FileSelectionData>
): List<VisibleNode> {
    val expandStates = list
        .flatMap { it.collectExpandStates() }
        .map { it.collectAsStateWithLifecycle() }

    return remember(list, expandStates.map { it.value }) {
        buildList {
            /**
             * 递归添加文件节点
             * @param indentation 缩进次数
             */
            fun addNodes(
                nodes: List<FileSelectionData>,
                indentation: Int
            ) {
                nodes.forEach { node ->
                    add(
                        VisibleNode.FileNode(node, indentation)
                    )

                    val child = node.child
                    if (child != null && node.expand.value) {
                        val indentation0 = indentation + 1
                        if (child.isNotEmpty()) {
                            addNodes(child, indentation0)
                        } else {
                            val key = "parent:" + node.file.absolutePath + ",indentation=" + indentation.toString()
                            add(
                                VisibleNode.EmptyNode(key,indentation0)
                            )
                        }
                    }
                }
            }

            addNodes(list, 0)
        }
    }
}

private fun FileSelectionData.collectExpandStates(): List<StateFlow<Boolean>> {
    val result = mutableListOf(expand)
    child?.forEach {
        result += it.collectExpandStates()
    }
    return result
}