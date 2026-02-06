package com.movtery.zalithlauncher.game.version.multiplayer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import com.movtery.zalithlauncher.utils.network.ServerAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Minecraft 服务器主要信息数据类
 * @param name 由玩家定义的服务器名称
 * @param ip 服务器的ip地址
 * @param originIp 玩家填写的原始服务器ip地址
 * @param texturePackStatus 服务器的纹理包启用状态
 * @param acceptedCodeOfConduct 是否已接受服务器代码条款
 * @param icon 服务器保存在本地的图标
 */
data class ServerData(
    val name: String,
    val ip: ServerAddress,
    val originIp: String,
    val texturePackStatus: TexturePackStatus,
    val acceptedCodeOfConduct: Boolean?,
    val icon: ByteArray? = null
) {
    enum class TexturePackStatus(
        val storageCode: Int?
    ) {
        ENABLED(1),
        DISABLED(0),
        /** 提示用户启用纹理包 */
        PROMPT(null)
    }

    sealed interface Operation {
        data object Loading : Operation
        /** 服务器加载成功 */
        data class Loaded(val result: ServerPingResult) : Operation
        /** 无法连接至服务器 */
        data object Failed : Operation
    }

    var uiIcon by mutableStateOf<Any?>(icon)
        private set

    var operation by mutableStateOf<Operation>(Operation.Loading)
        private set

    suspend fun load() {
        withContext(Dispatchers.Main) {
            operation = Operation.Loading
        }

        runCatching {
            val resolvedAddress = ip.resolve()
            val result = pingServer(resolvedAddress)

            withContext(Dispatchers.Main) {
                uiIcon = result.status.favicon ?: icon
                operation = Operation.Loaded(result)
            }
        }.onFailure {
            lWarning("Unable to load/connect to server: $ip", it)
            withContext(Dispatchers.Main) {
                operation = Operation.Failed
            }
        }
    }

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