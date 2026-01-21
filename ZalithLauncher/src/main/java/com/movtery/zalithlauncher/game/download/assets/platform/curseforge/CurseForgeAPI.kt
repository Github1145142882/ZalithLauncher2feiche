package com.movtery.zalithlauncher.game.download.assets.platform.curseforge

import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSearchFilter
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeCategory
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeModLoader

/**
 * CurseForge 平台的 API 链接
 * [CurseForge REST API](https://docs.curseforge.com/rest-api/?shell#base-url)
 */
const val CURSEFORGE_API = "https://api.curseforge.com/v1"

/**
 * MCIM 镜像：CurseForge 平台的 API 链接
 * [MCIM CurseForge API](https://github.com/mcmod-info-mirror/mcim-rust-api?tab=readme-ov-file#curseforge)
 */
const val MCIM_CURSEFORGE_API = "https://mod.mcimirror.top/curseforge/v1"

fun PlatformSearchFilter.toCurseForgeRequest(
    query: String,
    platformClasses: PlatformClasses
): CurseForgeSearchRequest {
    val curseforgeCategories = categories.map { category ->
        category as? CurseForgeCategory
    }.toTypedArray()

    return CurseForgeSearchRequest(
        classId = platformClasses.curseforge.classID,
        categories = setOfNotNull(
            *curseforgeCategories
        ),
        searchFilter = query,
        gameVersion = gameVersion,
        sortField = sortField,
        modLoader = modloader as? CurseForgeModLoader,
        index = index,
        pageSize = limit
    )
}