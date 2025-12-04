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

package com.movtery.zalithlauncher.ui.screens.game.multiplayer

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.terracotta.Terracotta
import com.movtery.zalithlauncher.terracotta.TerracottaVPNService
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import net.burningtnt.terracotta.TerracottaAndroidAPI

/**
 * 陶瓦联机状态操作
 */
sealed interface TerracottaOperation {
    data object None: TerracottaOperation
    /** 打开陶瓦联机菜单 */
    data object ShowMenu: TerracottaOperation
}

/**
 * 陶瓦联机状态操作
 */
@Composable
fun TerracottaOperation(
    viewModel: TerracottaViewModel
) {
    val context = LocalContext.current

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val vpnIntent = Intent(context, TerracottaVPNService::class.java).apply {
                action = TerracottaVPNService.ACTION_START
            }
            ContextCompat.startForegroundService(context, vpnIntent)
        } else {
            TerracottaAndroidAPI.getPendingVpnServiceRequest().reject()
        }
    }

    DisposableEffect(Unit) {
        viewModel.vpnLauncher = vpnLauncher
        onDispose {
            viewModel.vpnLauncher = null
        }
    }

    when (viewModel.operation) {
        is TerracottaOperation.None -> {}
        is TerracottaOperation.ShowMenu -> {
            val anonymousString = stringResource(R.string.terracotta_player_anonymous)

            val userName: String = remember(viewModel) {
                viewModel.getUserName()
            } ?: anonymousString //未设置，使用“匿名玩家”

            MultiplayerDialog(
                onClose = { viewModel.operation = TerracottaOperation.None },
                dialogState = viewModel.dialogState,
                terracottaVer = viewModel.terracottaVer,
                easyTierVer = viewModel.easyTierVer,
                onHostRoleClick = {
                    runCatching {
                        Terracotta.setScanning(null, userName)
                    }.onFailure { e ->
                        lWarning("Error occurred at \"Terracotta.setGuesting(null, userName)\", message = ${e.message}")
                    }
                },
                onHostCopyCode = { state ->
                    viewModel.copyInviteCode(context, state)
                },
                onHostBack = {
                    //取消端口扫描/取消启动房间/退出房间
                    Terracotta.setWaiting(context, true)
                }
            )
        }
    }
}