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
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.terracotta.Terracotta
import com.movtery.zalithlauncher.terracotta.TerracottaState
import com.movtery.zalithlauncher.terracotta.TerracottaVPNService
import com.movtery.zalithlauncher.utils.copyText
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TerracottaViewModel(
    val eventViewModel: EventViewModel,
    val getUserName: () -> String?
): ViewModel() {
    var operation by mutableStateOf<TerracottaOperation>(TerracottaOperation.None)

    /**
     * 联机菜单状态
     */
    var dialogState by mutableStateOf<TerracottaState.Ready?>(null)

    /**
     * 陶瓦联机核心版本号，在初始化完成后非null
     */
    var terracottaVer by mutableStateOf<String?>(null)

    /**
     * EasyTier版本号，在初始化完成后非null
     */
    var easyTierVer by mutableStateOf<String?>(null)

    /**
     * VPN权限申请，由TerracottaOperation设置
     */
    var vpnLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>? = null

    private val allJobs: MutableList<Job> = mutableListOf()

    /**
     * 打开陶瓦联机菜单
     */
    fun openMenu(activity: Activity) {
        if (operation !is TerracottaOperation.None) return

        if (!Terracotta.initialized) initialize(activity)
        if (allJobs.isEmpty()) initJobs(activity)

        operation = TerracottaOperation.ShowMenu
    }

    /**
     * 复制房间邀请码到系统剪贴板
     */
    fun copyInviteCode(
        context: Context,
        state: TerracottaState.HostOK
    ) {
        val code = state.code ?: return //理论上不会是null
        copyText(
            label = "invite_code",
            text = code,
            context = context
        )
        Toast.makeText(context, context.getString(R.string.terracotta_status_host_ok_code_copy_toast), Toast.LENGTH_SHORT).show()
    }

    /**
     * 初始化陶瓦联机
     */
    private fun initialize(context: Context) {
        Terracotta.initialize(context, viewModelScope, eventViewModel)
        Terracotta.setWaiting(context, true)

        val metadata = Terracotta.getMetadata()
        terracottaVer = metadata.terracottaVersion
        easyTierVer = metadata.easyTierVersion
    }

    private fun initJobs(activity: Activity) {
        val stateChangeJob = viewModelScope.launch {
            Terracotta.stateChanges.collect { (old, new) ->
                if (new is TerracottaState.HostOK) {
                    if (old !is TerracottaState.HostOK) {
                        //刚切换到这个状态，默认复制一次邀请码
                        copyInviteCode(activity, new)
                    }
                    if (new.isForkOf(old)) {
                        //TODO refresh hostOk UI
                        return@collect
                    }
                } else if (new is TerracottaState.GuestOK) {
                    if (new.isForkOf(old)) {
                        //TODO refresh guestOk UI
                        return@collect
                    }
                }
                dialogState = new
            }
        }

        val eventJob = viewModelScope.launch {
            eventViewModel.events
                .filterIsInstance<EventViewModel.Event.Terracotta>()
                .collect { event ->
                    when (event) {
                        is EventViewModel.Event.Terracotta.RequestVPN -> {
                            withContext(Dispatchers.Main) {
                                val intent = VpnService.prepare(activity)
                                if (intent != null) {
                                    vpnLauncher?.launch(intent)
                                } else {
                                    val vpnIntent = Intent(activity, TerracottaVPNService::class.java).apply {
                                        action = TerracottaVPNService.ACTION_START
                                    }
                                    ContextCompat.startForegroundService(activity, vpnIntent)
                                }
                            }
                        }
                        is EventViewModel.Event.Terracotta.StopVPN -> {
                            if (TerracottaVPNService.isRunning()) {
                                val vpnIntent = Intent(activity, TerracottaVPNService::class.java).apply {
                                    action = TerracottaVPNService.ACTION_STOP
                                }
                                ContextCompat.startForegroundService(activity, vpnIntent)
                            }
                        }
                    }
                }
        }

        allJobs.add(stateChangeJob)
        allJobs.add(eventJob)
    }

    override fun onCleared() {
        allJobs.forEach { it.cancel() }
        allJobs.clear()
    }
}

@Composable
fun rememberTerracottaViewModel(
    keyTag: String,
    eventViewModel: EventViewModel,
    getUserName: () -> String?
): TerracottaViewModel {
    return viewModel(
        key = keyTag
    ) {
        TerracottaViewModel(eventViewModel, getUserName)
    }
}