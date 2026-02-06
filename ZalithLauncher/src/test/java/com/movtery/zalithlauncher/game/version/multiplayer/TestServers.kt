package com.movtery.zalithlauncher.game.version.multiplayer

import com.movtery.zalithlauncher.utils.network.ServerAddress
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test

class TestServers {

    @Test
    fun testSingleServerData() {
        val ip = "cn.mccisland.net"
        val address = ServerAddress.parse(ip)

        runBlocking {
            launch {
                runCatching {
                    val resolvedAddress = address.resolve()
                    val result = pingServer(resolvedAddress)
                    println("ip = $ip, Ping = ${result.pingMs}, status = ${result.status}")
                }.onFailure {
                    it.printStackTrace()
                }
            }
        }
    }
}