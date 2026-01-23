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

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movtery.zalithlauncher.path.createOkHttpClient
import com.movtery.zalithlauncher.path.createRequestBuilder
import com.movtery.zalithlauncher.utils.isChinese
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import java.io.File

sealed interface ErrorOperation {
    data object None : ErrorOperation
    data object Uploading : ErrorOperation
    data class UploadSuccess(val url: String) : ErrorOperation
    data class UploadFailed(val error: String) : ErrorOperation
}

class ErrorActivityViewModel : ViewModel() {
    private val json = Json { ignoreUnknownKeys = true }
    var operation by mutableStateOf<ErrorOperation>(ErrorOperation.None)

    fun uploadLogs(context: Context, logFile: File) {
        if (!logFile.exists() || !logFile.isFile) return

        viewModelScope.launch {
            operation = ErrorOperation.Uploading
            try {
                val logContent = withContext(Dispatchers.IO) { logFile.readText() }
                val isChinese = isChinese(context)
                val apiUrl = if (isChinese) "https://api.mclogs.lemwood.icu/1/log" else "https://api.mclo.gs/1/log"

                val formBody = FormBody.Builder()
                    .add("content", logContent)
                    .build()

                val request = createRequestBuilder(apiUrl, formBody).build()
                val client = createOkHttpClient()

                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { resp ->
                        val body = resp.body?.string() ?: throw Exception("Empty body")
                        if (!resp.isSuccessful) {
                            // 尝试解析错误信息，如果解析失败则抛出 HTTP 状态码
                            val errorMsg = try {
                                json.decodeFromString<MclogsResponse>(body).error
                            } catch (_: Exception) {
                                null
                            }
                            throw Exception(errorMsg ?: "HTTP ${resp.code}")
                        }
                        json.decodeFromString<MclogsResponse>(body)
                    }
                }

                if (response.success && response.url != null) {
                    operation = ErrorOperation.UploadSuccess(response.url)
                } else {
                    operation = ErrorOperation.UploadFailed(response.error ?: "Unknown error")
                }
            } catch (e: Exception) {
                operation = ErrorOperation.UploadFailed(e.message ?: "Unknown error")
            }
        }
    }

    fun resetOperation() {
        operation = ErrorOperation.None
    }

    @Serializable
    private data class MclogsResponse(
        val success: Boolean,
        val url: String? = null,
        val error: String? = null
    )
}
