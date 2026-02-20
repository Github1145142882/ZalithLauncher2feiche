package com.movtery.zalithlauncher.game.version.export

import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import java.io.File

/**
 * 导出整合包时需要的所有信息
 * @param name 用户指定要导出的整合包名称
 * @param summary 用户指定的整合包描述
 * @param author 整合包作者名称
 * @param version 用户指定的整合包版本
 * @param mcVersion Minecraft 版本
 * @param loader 该版本所加载的模组加载器
 * @param selectedFiles 用户选定的要导出的文件
 * @param minMemory 用户指定的整合包最小内存大小
 * @param maxMemory 用户指定的整合包最大内存大小
 * @param jvmArgs 游戏参数
 * @param javaArgs Java虚拟机参数
 * @param fileApi 整合包下载链接前缀
 * @param url 整合包官方网站
 * @param forceUpdate 强制更新整合包
 * @param packType 导出整合包的类型
 * @param packRemote 是否打包远程资源
 * @param packCurseForge 是否打包CurseForge的远程资源
 */
data class ExportInfo(
    val gamePath: File,
    val name: String = "",
    val summary: String? = null,
    val author: String = "",
    val version: String = "",
    val mcVersion: String = "",
    val loader: LoaderVersion? = null,
    val selectedFiles: List<File> = emptyList(),
    val minMemory: Int = 0,
    val maxMemory: Int = 0,
    val jvmArgs: String = "",
    val javaArgs: String = "",
    val fileApi: String? = null,
    val url: String = "",
    val forceUpdate: Boolean = false,
    val packType: PackType = PackType.Modrinth,
    val packRemote: Boolean = true,
    val packCurseForge: Boolean = true
) {
    /**
     * 模组加载器信息
     * @param version 加载器版本
     */
    data class LoaderVersion(
        val loader: ModLoader,
        val version: String
    )
}
