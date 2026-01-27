package com.movtery.zalithlauncher.upgrade

import java.util.Locale

/**
 * 获取简单的语言标签
 */
fun Locale.toLangTag(): String {
    return language + "_" + country.lowercase()
}

private fun Locale.compareLangTag(
    targetTag: String
): Boolean {
    return if (targetTag.contains("_")) {
        toLangTag() == targetTag
    } else {
        language == targetTag
    }
}

/**
 * 根据当前系统语言寻找合适的Body
 */
fun RemoteData.findCurrentBody(
    locale: Locale
): RemoteData.RemoteBody? {
    return bodies.sortedByDescending {
        it.language.contains("_")
    }.find { body ->
        locale.compareLangTag(body.language)
    }
}

/**
 * 根据当前系统语言寻找合适的网盘链接，若未找到则尝试匹配默认网盘链接
 */
fun RemoteData.getCurrentCouldDrive(
    locale: Locale
): RemoteData.CloudDrive? {
    return cloudDrives.sortedByDescending {
        it.language.contains("_")
    }.find { drive ->
        locale.compareLangTag(drive.language)
    } ?: defaultCloudDrive?.takeIf {
        //如果是 NULL，则是全区域可用
        //否则根据语言决定是否可用
        it.language == "NULL" || locale.compareLangTag(it.language)
    }
}