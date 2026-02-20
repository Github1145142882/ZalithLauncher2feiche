package com.movtery.zalithlauncher.ui.screens.content.versions.export

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Output
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.export.data.FileSelectionData
import com.movtery.zalithlauncher.game.version.export.data.Selected
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState

/**
 * 选择要导出的文件
 * @param onFinish 完成文件选择
 */
@Composable
fun ExportSelectFilesScreen(
    allFiles: List<FileSelectionData>,
    selectedFiles: Boolean,
    onRefreshRootSelect: () -> Unit,
    isSelectingFolder: Boolean,
    mainScreenKey: NavKey?,
    exportScreenKey: NavKey?,
    version: Version,
    backToMainScreen: () -> Unit,
    onFinish: () -> Unit
) {
    if (!version.isValid()) {
        backToMainScreen()
        return
    }

    BaseScreen(
        levels1 = listOf(
            Pair(NestedNavKey.VersionExport::class.java, mainScreenKey)
        ),
        Triple(NormalNavKey.VersionExports.SelectFiles, exportScreenKey, false)
    ) { isVisible ->
        val yOffset by swapAnimateDpAsState(
            targetValue = (-40).dp,
            swapIn = isVisible
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
            contentAlignment = Alignment.BottomEnd
        ) {
            //文件选择区域
            FileSelectorList(
                modifier = Modifier.fillMaxSize(),
                list = allFiles,
                onUnselectedAll = { data ->
                    data.updateSelectState(Selected.Unselected)
                    onRefreshRootSelect()
                },
                onSelectedAll = { data ->
                    data.updateSelectState(Selected.Selected)
                    onRefreshRootSelect()
                }
            )

            Button(
                modifier = Modifier.padding(all = 12.dp),
                onClick = onFinish,
                enabled = !isSelectingFolder && selectedFiles
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val text = stringResource(R.string.versions_export_pack_select_output)
                    Icon(
                        imageVector = Icons.Default.Output,
                        contentDescription = text
                    )

                    Text(text = text)
                }
            }
        }
    }
}

@Composable
private fun FileSelectorList(
    list: List<FileSelectionData>,
    onUnselectedAll: (FileSelectionData) -> Unit,
    onSelectedAll: (FileSelectionData) -> Unit,
    modifier: Modifier = Modifier
) {
    //实际文件选择区域
    val visibleNodes = rememberVisibleNodes(list)

    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.onSurface
    ) {
        LazyColumn(
            modifier = modifier
                .horizontalScroll(rememberScrollState()),
            contentPadding = PaddingValues(all = 12.dp),
        ) {
            item {
                Text(
                    modifier = Modifier.padding(bottom = 16.dp),
                    text = stringResource(R.string.versions_export_pack_files),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            items(
                items = visibleNodes,
                key = { it.key }
            ) { node ->
                FileNodeItem(
                    modifier = Modifier.animateItem(),
                    node = node,
                    onUnselectedAll = onUnselectedAll,
                    onSelectedAll = onSelectedAll
                )
            }
        }
    }
}

@Composable
private fun FileNodeItem(
    node: VisibleNode,
    onUnselectedAll: (FileSelectionData) -> Unit,
    onSelectedAll: (FileSelectionData) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val indentation = node.indentation

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width((indentation * 16).dp))

            when (node) {
                is VisibleNode.EmptyNode -> {
                    Text(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .padding(start = 46.dp)
                            .alpha(0.7f),
                        text = stringResource(R.string.versions_export_pack_dir_empty),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                is VisibleNode.FileNode -> {
                    val data = node.data
                    val child = remember(data) { data.child }

                    if (child != null) {
                        val expand by data.expand.collectAsStateWithLifecycle()

                        IconButton(
                            modifier = Modifier.size(48.dp),
                            onClick = { data.expandDirs(!expand) }
                        ) {
                            val rotation by animateFloatAsState(
                                if (expand) 90f else 0f
                            )

                            Icon(
                                modifier = Modifier.rotate(rotation),
                                imageVector = Icons.AutoMirrored.Default.ArrowRight,
                                contentDescription = null
                            )
                        }
                    } else {
                        //仅用于视觉上的对齐
                        Spacer(Modifier.size(48.dp))
                    }

                    val selected by data.selected.collectAsStateWithLifecycle()
                    TriStateCheckbox(
                        state = when (selected) {
                            Selected.Selected -> ToggleableState.On
                            Selected.Indeterminate -> ToggleableState.Indeterminate
                            Selected.Unselected -> ToggleableState.Off
                        },
                        onClick = {
                            when (selected) {
                                Selected.Selected -> onUnselectedAll(data)
                                else -> onSelectedAll(data)
                            }
                        }
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        //文件别名
                        data.alias?.let { alias ->
                            Text(
                                modifier = Modifier.alpha(0.7f),
                                text = stringResource(alias),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        //文件名
                        Text(
                            text = data.file.name,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}