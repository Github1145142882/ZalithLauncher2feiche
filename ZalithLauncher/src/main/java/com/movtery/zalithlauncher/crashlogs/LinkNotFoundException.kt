package com.movtery.zalithlauncher.crashlogs

/**
 * API 站点返回的链接不可用、不存在
 */
class LinkNotFoundException : RuntimeException(
    "Unable to find an available link from the data returned by the remote end"
)