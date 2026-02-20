package com.movtery.zalithlauncher.game.version.export

import android.content.Context
import com.movtery.zalithlauncher.coroutine.TitledTask
import com.movtery.zalithlauncher.game.version.installed.Version
import java.io.File

abstract class AbstractExporter(
    val type: PackType
) {
    /**
     * 构建所需的导出任务
     */
    abstract fun MutableList<TitledTask>.buildTasks(
        context: Context,
        version: Version,
        info: ExportInfo,
        tempPath: File
    )

    /**
     * 整合包文件后缀
     */
    abstract val fileSuffix: String

    protected fun generateTargetRoot(
        file: File,
        rootPath: String,
        targetPath: String
    ): File {
        return File(targetPath, relativePath(file, rootPath))
    }

    /**
     * 获取一个文件的相对路径
     */
    protected fun relativePath(
        file: File,
        rootPath: String
    ): String {
        return file.absolutePath
            .removePrefix(rootPath)
            .removePrefix("/")
    }
}