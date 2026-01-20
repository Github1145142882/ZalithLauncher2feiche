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

package com.movtery.zalithlauncher.ui.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.jakewharton.processphoenix.ProcessPhoenix
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.launch.LogName
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.path.createOkHttpClient
import com.movtery.zalithlauncher.path.createRequestBuilder
import com.movtery.zalithlauncher.ui.base.BaseComponentActivity
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.ui.components.ScalingActionButton
import com.movtery.zalithlauncher.ui.screens.main.ErrorScreen
import com.movtery.zalithlauncher.ui.theme.ZalithLauncherTheme
import com.movtery.zalithlauncher.utils.file.shareFile
import com.movtery.zalithlauncher.utils.getParcelableSafely
import com.movtery.zalithlauncher.utils.getSerializableSafely
import com.movtery.zalithlauncher.utils.network.openLink
import com.movtery.zalithlauncher.utils.string.throwableToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import java.io.File
import java.util.Locale

private const val BUNDLE_EXIT_TYPE = "BUNDLE_EXIT_TYPE"
private const val BUNDLE_THROWABLE = "BUNDLE_THROWABLE"
private const val BUNDLE_JVM_CRASH = "BUNDLE_JVM_CRASH"
private const val BUNDLE_CAN_RESTART = "BUNDLE_CAN_RESTART"
private const val EXIT_JVM = "EXIT_JVM"
private const val EXIT_LAUNCHER = "EXIT_LAUNCHER"

fun showExitMessage(context: Context, code: Int, isSignal: Boolean) {
    val intent = Intent(context, ErrorActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(BUNDLE_EXIT_TYPE, EXIT_JVM)
        putExtra(BUNDLE_JVM_CRASH, JvmCrash(code, isSignal))
    }
    context.startActivity(intent)
}

@Parcelize
private data class JvmCrash(val code: Int, val isSignal: Boolean): Parcelable

class ErrorActivity : BaseComponentActivity(refreshData = false) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extras = intent.extras ?: return runFinish()

        val exitType = extras.getString(BUNDLE_EXIT_TYPE, EXIT_LAUNCHER)

        val errorMessage = when (exitType) {
            EXIT_JVM -> {
                val jvmCrash = extras.getParcelableSafely(BUNDLE_JVM_CRASH, JvmCrash::class.java) ?: return runFinish()
                val messageResId = if (jvmCrash.isSignal) R.string.crash_singnal_message else R.string.crash_exit_message
                val message = getString(messageResId, jvmCrash.code)
                val messageBody = getString(R.string.crash_exit_note)
                ErrorMessage(
                    message = message,
                    messageBody = messageBody,
                    crashType = CrashType.GAME_CRASH
                )
            }
            else -> {
                val throwable = extras.getSerializableSafely(BUNDLE_THROWABLE, Throwable::class.java) ?: return runFinish()
                val message = getString(R.string.crash_launcher_message)
                val messageBody = throwableToString(throwable)
                ErrorMessage(
                    message = message,
                    messageBody = messageBody,
                    crashType = CrashType.LAUNCHER_CRASH
                )
            }
        }

        val logFile = when (exitType) {
            EXIT_JVM -> {
                File(PathManager.DIR_FILES_EXTERNAL, "${LogName.GAME.fileName}.log")
            }
            else -> {
                PathManager.FILE_CRASH_REPORT
            }
        }

        val canRestart: Boolean = extras.getBoolean(BUNDLE_CAN_RESTART, true)

        setContent {
            ZalithLauncherTheme {
                var uploadResultUrl by remember { mutableStateOf<String?>(null) }

                Box {
                    ErrorScreen(
                        crashType = errorMessage.crashType,
                        message = errorMessage.message,
                        messageBody = errorMessage.messageBody,
                        shareLogs = logFile.exists() && logFile.isFile,
                        canRestart = canRestart,
                        onShareLogsClick = {
                            if (logFile.exists() && logFile.isFile) {
                                shareFile(this@ErrorActivity, logFile)
                            }
                        },
                        onUploadLogsClick = {
                            if (logFile.exists() && logFile.isFile) {
                                lifecycleScope.launch {
                                    try {
                                        Toast.makeText(
                                            this@ErrorActivity,
                                            R.string.crash_uploading_logs,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        val logContent =
                                            withContext(Dispatchers.IO) { logFile.readText() }

                                        val locale = Locale.getDefault()
                                        val isChinese = locale.language == "zh"
                                        val apiUrl =
                                            if (isChinese) "https://api.mclogs.lemwood.icu/1/log" else "https://api.mclo.gs/1/log"

                                        val formBody = FormBody.Builder()
                                            .add("content", logContent)
                                            .build()

                                        val request = createRequestBuilder(apiUrl, formBody).build()
                                        val client = createOkHttpClient()

                                        val response = withContext(Dispatchers.IO) {
                                            client.newCall(request).execute().use { resp ->
                                                if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
                                                val body = resp.body?.string()
                                                    ?: throw Exception("Empty body")
                                                Json {
                                                    ignoreUnknownKeys = true
                                                }.decodeFromString<MclogsResponse>(body)
                                            }
                                        }

                                        if (response.success && response.url != null) {
                                            uploadResultUrl = response.url
                                        } else {
                                            Toast.makeText(
                                                this@ErrorActivity,
                                                getString(
                                                    R.string.crash_upload_logs_failed,
                                                    response.error ?: "Unknown error"
                                                ),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            this@ErrorActivity,
                                            getString(
                                                R.string.crash_upload_logs_failed,
                                                e.message ?: "Unknown error"
                                            ),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        },
                        onRestartClick = {
                            ProcessPhoenix.triggerRebirth(this@ErrorActivity)
                        },
                        onExitClick = { finish() }
                    )

                    if (uploadResultUrl != null) {
                        val url = uploadResultUrl!!
                        Dialog(onDismissRequest = { uploadResultUrl = null }) {
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                shadowElevation = 6.dp,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                            ) {
                                val scrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .verticalScroll(scrollState),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.crash_upload_success_title),
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = url,
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    val options = listOf(
                                        R.string.crash_upload_option_open to {
                                            openLink(url)
                                            uploadResultUrl = null
                                        },
                                        R.string.crash_upload_option_copy to {
                                            val clipboard =
                                                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("mclogs", url)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(
                                                this@ErrorActivity,
                                                R.string.generic_copy_success,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            uploadResultUrl = null
                                        },
                                        R.string.crash_upload_option_share to {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    getString(
                                                        R.string.crash_upload_share_template,
                                                        url
                                                    )
                                                )
                                            }
                                            startActivity(
                                                Intent.createChooser(
                                                    shareIntent,
                                                    getString(R.string.crash_upload_option_share)
                                                )
                                            )
                                            uploadResultUrl = null
                                        },
                                        R.string.generic_cancel to { uploadResultUrl = null }
                                    )

                                    options.forEach { (resId, action) ->
                                        ScalingActionButton(
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = action
                                        ) {
                                            MarqueeText(text = stringResource(resId))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private data class ErrorMessage(
        val message: String,
        val messageBody: String,
        val crashType: CrashType
    )
}

@Serializable
private data class MclogsResponse(
    val success: Boolean,
    val url: String? = null,
    val error: String? = null
)

/**
 * 崩溃类型
 */
enum class CrashType(val textRes: Int) {
    /**
     * 启动器崩溃
     */
    LAUNCHER_CRASH(R.string.crash_type_launcher),

    /**
     * 游戏运行崩溃
     */
    GAME_CRASH(R.string.crash_type_game)
}

/**
 * 启动软件崩溃信息页面
 */
fun showLauncherCrash(context: Context, throwable: Throwable, canRestart: Boolean = true) {
    val intent = Intent(context, ErrorActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(BUNDLE_EXIT_TYPE, EXIT_LAUNCHER)
        putExtra(BUNDLE_THROWABLE, throwable)
        putExtra(BUNDLE_CAN_RESTART, canRestart)
    }
    context.startActivity(intent)
}