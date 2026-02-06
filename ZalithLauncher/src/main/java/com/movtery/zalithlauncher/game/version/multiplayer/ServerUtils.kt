package com.movtery.zalithlauncher.game.version.multiplayer

import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import com.movtery.zalithlauncher.utils.nbt.asBoolean
import com.movtery.zalithlauncher.utils.nbt.asList
import com.movtery.zalithlauncher.utils.nbt.asString
import com.movtery.zalithlauncher.utils.nbt.asStringNotNull
import com.movtery.zalithlauncher.utils.network.ServerAddress
import net.querz.nbt.io.NBTUtil
import net.querz.nbt.tag.CompoundTag
import java.io.File
import java.util.Base64

/**
 * 尝试读取 servers.dat 文件中存储的服务器信息
 */
fun parseServerData(file: File): List<ServerData> {
    return runCatching {
        if (!file.exists()) return emptyList()

        val compound = NBTUtil.read(file, false).tag as? CompoundTag
            ?: error("Failed to read the server.dat file as a CompoundTag.")
        val servers = compound.asList("servers")
            ?: error("servers entry not found in the NBT structure tree.")

        servers.mapNotNull { serverTag ->
            //是否被隐藏了，隐藏了的话，将不使用这个服务器数据
            val isHidden = serverTag.asBoolean("hidden", false)
            if (!isHidden) {
                serverTag.parseServerData()
            } else {
                null
            }
        }
    }.onFailure {
        lWarning("An exception occurred while reading and parsing the servers.dat file (${file.absolutePath}).", it)
    }.getOrElse {
        emptyList()
    }
}

/**
 * [参考 WIKI](https://zh.minecraft.wiki/w/%E6%9C%8D%E5%8A%A1%E5%99%A8%E5%88%97%E8%A1%A8%E5%AD%98%E5%82%A8%E6%A0%BC%E5%BC%8F#%E5%AD%98%E5%82%A8%E6%A0%BC%E5%BC%8F)
 */
private fun CompoundTag.parseServerData(): ServerData? {
    val name = asStringNotNull("name", "")

    //尝试解析服务器的ip
    val (ip, origin) = asStringNotNull("ip", "").let { ip ->
        val address = runCatching {
            ServerAddress.parse(ip)
        }.onFailure {
            lWarning("Unable to parse server $name's IP address, original string: $ip")
        }.getOrNull() ?: return null

        address to ip
    }

    //尝试解析icon作为placeholder
    val icon = asString("icon", null)?.let { base64 ->
        runCatching {
            Base64.getDecoder().decode(base64)
        }.onFailure {
            lWarning("Unable to recognize server $name's icon as a valid icon array")
        }.getOrNull()
    }

    return ServerData(
        name = name,
        ip = ip,
        originIp = origin,
        icon = icon
    )
}