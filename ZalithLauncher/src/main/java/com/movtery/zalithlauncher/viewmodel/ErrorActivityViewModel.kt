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
                
                // 定义主备 API 列表
                val apiConfigs = if (isChinese) {
                    listOf(
                        ApiConfig("https://api.mclogs.lemwood.icu/1/log", "https://mclogs.lemwood.icu", true),
                        ApiConfig("https://api.mclo.gs/1/log", "https://mclo.gs", false)
                    )
                } else {
                    listOf(
                        ApiConfig("https://api.mclo.gs/1/log", "https://mclo.gs", false),
                        ApiConfig("https://api.mclogs.lemwood.icu/1/log", "https://mclogs.lemwood.icu", true)
                    )
                }

                var lastException: Exception? = null
                var successResponse: MclogsResponse? = null

                val client = createOkHttpClient()

                // 遍历尝试 API
                for (config in apiConfigs) {
                    try {
                        val formBody = FormBody.Builder()
                            .add("content", logContent)
                            .build()

                        val request = createRequestBuilder(config.apiUrl, formBody).build()

                        val response = withContext(Dispatchers.IO) {
                            client.newCall(request).execute().use { resp ->
                                val rawBody = resp.body?.string() ?: throw Exception("Empty body")
                                
                                // 清洗响应内容：提取第一个 { 和最后一个 } 之间的 JSON 部分
                                val body = try {
                                    val startIndex = rawBody.indexOf('{')
                                    val endIndex = rawBody.lastIndexOf('}')
                                    if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                                        rawBody.substring(startIndex, endIndex + 1)
                                    } else {
                                        rawBody
                                    }
                                } catch (_: Exception) {
                                    rawBody
                                }

                                if (!resp.isSuccessful) {
                                    val errorMsg = try {
                                        json.decodeFromString<MclogsResponse>(body).error
                                    } catch (_: Exception) {
                                        null
                                    }
                                    throw Exception(errorMsg ?: "HTTP ${resp.code}")
                                }
                                
                                val mclogsResponse = json.decodeFromString<MclogsResponse>(body)
                                
                                // 补齐 URL：统一处理 mclogs.lemwood.icu 和 mclo.gs 的返回格式
                                if (mclogsResponse.success && (mclogsResponse.url != null || mclogsResponse.id != null)) {
                                    val id = mclogsResponse.id ?: mclogsResponse.url?.substringAfterLast("/") ?: ""
                                    
                                    // 统一构造符合 Hash Mode 的 URL (国内站必须带 /#/)
                                    val finalUrl = if (config.isHashMode) {
                                        "${config.baseUrl}/#/$id"
                                    } else {
                                        "${config.baseUrl}/$id"
                                    }
                                    mclogsResponse.copy(url = finalUrl)
                                } else {
                                    mclogsResponse
                                }
                            }
                        }

                        if (response.success && response.url != null) {
                            successResponse = response
                            break // 成功则跳出循环
                        } else {
                            throw Exception(response.error ?: "Unknown error")
                        }
                    } catch (e: Exception) {
                        lastException = e
                        continue // 失败则尝试下一个 API
                    }
                }

                if (successResponse != null) {
                    operation = ErrorOperation.UploadSuccess(successResponse.url!!)
                } else {
                    operation = ErrorOperation.UploadFailed(lastException?.message ?: "All APIs failed")
                }
            } catch (e: Exception) {
                operation = ErrorOperation.UploadFailed(e.message ?: "Unknown error")
            }
        }
    }

    private data class ApiConfig(
        val apiUrl: String,
        val baseUrl: String,
        val isHashMode: Boolean
    )

    fun resetOperation() {
        operation = ErrorOperation.None
    }

    @Serializable
    private data class MclogsResponse(
        val success: Boolean,
        val url: String? = null,
        val id: String? = null,
        val error: String? = null
    )
}
