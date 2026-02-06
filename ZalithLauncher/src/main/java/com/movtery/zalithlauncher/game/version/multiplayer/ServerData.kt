package com.movtery.zalithlauncher.game.version.multiplayer

import com.movtery.zalithlauncher.utils.network.ServerAddress

/**
 * Minecraft 服务器主要信息数据类
 * @param name 由玩家定义的服务器名称
 * @param ip 服务器的ip地址
 * @param originIp 玩家填写的原始服务器ip地址
 * @param icon 服务器保存在本地的图标
 */
data class ServerData(
    val name: String,
    val ip: ServerAddress,
    val originIp: String,
    val icon: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ServerData

        if (name != other.name) return false
        if (ip != other.ip) return false
        if (!icon.contentEquals(other.icon)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + ip.hashCode()
        result = 31 * result + (icon?.contentHashCode() ?: 0)
        return result
    }
}