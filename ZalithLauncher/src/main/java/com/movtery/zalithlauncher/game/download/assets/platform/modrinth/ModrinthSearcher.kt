package com.movtery.zalithlauncher.game.download.assets.platform.modrinth

import com.movtery.zalithlauncher.game.download.assets.platform.AbstractPlatformSearcher
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSearchFilter
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthSingleProject
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthVersion
import com.movtery.zalithlauncher.utils.network.httpGetJson
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.Parameters
import java.io.File

class ModrinthSearcher(
    val api: String = MODRINTH_API,
    source: String = "Official Modrinth"
): AbstractPlatformSearcher(
    platform = Platform.MODRINTH,
    source = source
) {
    override suspend fun searchAssets(
        query: String,
        searchFilter: PlatformSearchFilter,
        platformClasses: PlatformClasses
    ): ModrinthSearchResult {
        return httpGetJson(
            url = "$api/search",
            parameters = searchFilter.toModrinthRequest(
                query = query,
                platformClasses = platformClasses
            ).toParameters()
        )
    }

    override suspend fun getProject(projectID: String): ModrinthSingleProject {
        return httpGetJson(
            url = "$api/project/$projectID"
        )
    }

    /**
     * 获取 Modrinth 项目的版本列表（可设置区间）
     * @param pageSize 每页请求数量，null则为获取所有版本
     * @param offset 开始处，null则为获取所有版本
     */
    suspend fun getVersionsChunk(
        projectID: String,
        pageSize: Int? = null,
        offset: Int? = null,
    ): List<ModrinthVersion> {
        return httpGetJson(
            url = "$api/project/$projectID/version",
            parameters = if (pageSize != null && offset != null) {
                Parameters.build {
                    append("limit", pageSize.toString())
                    append("offset", offset.toString())
                }
            } else null
        )
    }

    override suspend fun getVersions(
        projectID: String,
        pageCallback: (chunk: Int, page: Int) -> Unit
    ): List<ModrinthVersion> {
        return getVersionsChunk(
            projectID = projectID
        )
    }

    override suspend fun getVersionByLocalFile(
        file: File,
        sha1: String
    ): ModrinthVersion? {
        return try {
            httpGetJson(
                url = "$api/version_file/$sha1",
                parameters = Parameters.build {
                    append("algorithm", "sha1")
                }
            )
        } catch (_: ClientRequestException) {
            null
        }
    }
}