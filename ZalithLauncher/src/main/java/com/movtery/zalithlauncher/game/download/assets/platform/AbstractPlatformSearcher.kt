package com.movtery.zalithlauncher.game.download.assets.platform

import java.io.File

/**
 * 平台资源搜索抽象类
 * @param platform 目标平台类型（仅作标识）
 * @param source 源（区分官方源、镜像源）名称，仅日志需要
 */
abstract class AbstractPlatformSearcher(
    val platform: Platform,
    val source: String
) {
    /**
     * 搜索资源结果列表
     */
    abstract suspend fun searchAssets(
        query: String,
        searchFilter: PlatformSearchFilter,
        platformClasses: PlatformClasses
    ): PlatformSearchResult

    /**
     * 获取单个项目的信息
     */
    abstract suspend fun getProject(
        projectID: String,
    ): PlatformProject

    /**
     * 获取单个项目的所有版本信息
     */
    abstract suspend fun getVersions(
        projectID: String,
        pageCallback: (chunk: Int, page: Int) -> Unit = { _, _ -> },
    ): List<PlatformVersion>

    /**
     * 通过本地文件的sha值尝试找到对应的版本信息
     */
    abstract suspend fun getVersionByLocalFile(
        file: File,
        sha1: String,
    ): PlatformVersion?
}