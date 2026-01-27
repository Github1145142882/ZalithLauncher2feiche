package com.movtery.zalithlauncher.ui.screens.main.crashlogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.crashlogs.LinkNotFoundException
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.utils.string.getMessageOrToString

/**
 * 上传游戏崩溃日志操作流程
 */
sealed interface ShareLinkOperation {
    data object None : ShareLinkOperation
    /** 提示对话框 */
    data object Tip : ShareLinkOperation
    /**
     * 上传日志中
     * @param apiRoot API 站点链接，仅作透明化展示
     */
    data class Uploading(val apiRoot: String) : ShareLinkOperation
    /** 发生错误，展示对话框 */
    data class Error(val error: Throwable) : ShareLinkOperation
}

@Composable
fun ShareLinkOperation(
    operation: ShareLinkOperation,
    onChange: (ShareLinkOperation) -> Unit,
    onUpload: () -> Unit,
    onUploadChancel: () -> Unit
) {
    when (operation) {
        is ShareLinkOperation.None -> {}
        is ShareLinkOperation.Tip -> {
            SimpleAlertDialog(
                title = stringResource(R.string.crash_link_share_button),
                text = stringResource(R.string.crash_link_share_tip),
                dismissByDialog = false,
                onConfirm = onUpload,
                onDismiss = {
                    onChange(ShareLinkOperation.None)
                }
            )
        }
        is ShareLinkOperation.Uploading -> {
            SimpleAlertDialog(
                title = stringResource(R.string.crash_link_share_button),
                text = stringResource(R.string.crash_link_share_uploading, operation.apiRoot),
                dismissByDialog = false,
                confirmText = stringResource(R.string.generic_cancel),
                onDismiss = onUploadChancel
            )
        }
        is ShareLinkOperation.Error -> {
            SimpleAlertDialog(
                title = stringResource(R.string.crash_link_share_failed),
                text = when (val error = operation.error) {
                    is LinkNotFoundException -> {
                        stringResource(R.string.crash_link_share_failed_link_not_found)
                    }
                    else -> {
                        error.getMessageOrToString()
                    }
                },
                dismissByDialog = false,
                onDismiss = {
                    onChange(ShareLinkOperation.None)
                }
            )
        }
    }
}