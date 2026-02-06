package com.movtery.zalithlauncher.game.version.multiplayer

import com.github.steveice10.opennbt.NBTIO
import com.github.steveice10.opennbt.tag.builtin.CompoundTag
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import com.movtery.zalithlauncher.utils.nbt.asBoolean
import com.movtery.zalithlauncher.utils.nbt.asBooleanNotNull
import com.movtery.zalithlauncher.utils.nbt.asList
import com.movtery.zalithlauncher.utils.nbt.asString
import com.movtery.zalithlauncher.utils.nbt.asStringNotNull
import com.movtery.zalithlauncher.utils.network.ServerAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Base64

class AllServers {
    private val _serverList = mutableListOf<ServerData>()
    val serverList: List<ServerData>
        get() = _serverList

    private val hiddenServerList = mutableListOf<ServerData>()

    /**
     * 尝试读取 servers.dat 文件中存储的服务器信息
     */
    suspend fun loadServers(dataFile: File) {
        withContext(Dispatchers.IO) {
            runCatching {
                //清除当前所有服务器
                _serverList.clear()
                hiddenServerList.clear()

                if (!dataFile.exists()) return@withContext

                val compound = NBTIO.readFile(dataFile, false, false)
                    ?: error("Failed to read the server.dat file as a CompoundTag.")
                val servers = compound.asList("servers", null)
                    ?: error("servers entry not found in the NBT structure tree.")

                servers.forEach { tag ->
                    val serverTag = tag as? CompoundTag ?: return@forEach
                    val data = serverTag.parseServerData() ?: return@forEach
                    val isHidden = serverTag.asBooleanNotNull("hidden", false)
                    if (!isHidden) {
                        _serverList.add(data)
                    } else {
                        hiddenServerList.add(data)
                    }
                }
            }.onFailure {
                lWarning("An exception occurred while reading and parsing the servers.dat file (${dataFile.absolutePath}).", it)
            }
        }
    }

    fun addServer(
        server: ServerData,
        isHidden: Boolean = false
    ) {
        if (isHidden) {
            hiddenServerList.add(server)
            //Minecraft内有隐藏服务器自动清理机制
            //如果隐藏的服务器数量大于16，则会开始清除前面的服务器
            while (this.hiddenServerList.size > 16) {
                this.hiddenServerList.removeAt(this.hiddenServerList.size - 1)
            }
        } else {
            _serverList.add(server)
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

        val texturesStatus = asBoolean("acceptTextures", null).let { code ->
            when (code) {
                true -> ServerData.TexturePackStatus.ENABLED
                false -> ServerData.TexturePackStatus.DISABLED
                null -> ServerData.TexturePackStatus.PROMPT
            }
        }

        val acceptedCodeOfConduct = asBoolean("acceptedCodeOfConduct", null)

        return ServerData(
            name = name,
            ip = ip,
            originIp = origin,
            texturePackStatus = texturesStatus,
            acceptedCodeOfConduct = acceptedCodeOfConduct,
            icon = icon
        )
    }
}