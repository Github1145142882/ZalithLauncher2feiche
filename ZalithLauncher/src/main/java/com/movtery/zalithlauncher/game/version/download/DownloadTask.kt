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

package com.movtery.zalithlauncher.game.version.download

import com.movtery.zalithlauncher.utils.file.compareSHA1
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import com.movtery.zalithlauncher.utils.network.downloadFromMirrorList
import com.movtery.zalithlauncher.utils.string.getMessageOrToString
import com.movtery.zalithlauncher.utils.string.isNotEmptyOrBlank
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileNotFoundException

class DownloadTask(
    val urls: List<String>,
    private val verifyIntegrity: Boolean,
    private val bufferSize: Int = 32768,
    val targetFile: File,
    val sha1: String?,
    /** 是否本身是可以被下载的，如果不可下载，则通过提供url尝试下载，如果失败则抛出 FileNotFoundException */
    val isDownloadable: Boolean,
    private val onDownloadFailed: (DownloadTask) -> Unit = {},
    private val onFileDownloadedSize: (Long) -> Unit = {},
    private val onFileDownloaded: () -> Unit = {}
) {
    /**
     * 文件下载成功后执行的任务
     */
    var fileDownloadedTask: (suspend () -> Unit)? = null

    suspend fun download() {
        //若目标文件存在，验证通过或关闭完整性验证时，跳过此次下载
        if (verifySha1()) {
            downloadedSize(FileUtils.sizeOf(targetFile))
            downloadedFile()
            return
        }

        runCatching {
            runInterruptible {
                downloadFromMirrorList(
                    urls = urls,
                    sha1 = sha1,
                    outputFile = targetFile,
                    bufferSize = bufferSize
                ) { size ->
                    downloadedSize(size)
                }
            }
            downloadedFile()
        }.onFailure { e ->
            if (e is CancellationException) throw e
            //fix: 下载中途断开网络，导致过多文本刷入日志
            //此处不再详细记录堆栈信息
            lError("Download failed: ${targetFile.absolutePath}\nurls: ${urls.joinToString("\n")}, string = ${e.getMessageOrToString()}")
            if (!isDownloadable && e is FileNotFoundException) throw e
            onDownloadFailed(this)
        }
    }

    private fun downloadedSize(size: Long) {
        onFileDownloadedSize(size)
    }

    private suspend fun downloadedFile() {
        onFileDownloaded()
        withContext(Dispatchers.IO) {
            fileDownloadedTask?.invoke()
        }
    }

    /**
     * 若目标文件存在，验证完整性
     * @return 是否跳过此次下载
     */
    private fun verifySha1(): Boolean {
        if (targetFile.exists()) {
            sha1?.takeIf { it.isNotEmptyOrBlank() } ?: return true //sha1 不存在，可能目标无法被下载
            if (!verifyIntegrity || compareSHA1(targetFile, sha1)) {
                return true
            } else {
                FileUtils.deleteQuietly(targetFile)
            }
        }
        return false
    }
}