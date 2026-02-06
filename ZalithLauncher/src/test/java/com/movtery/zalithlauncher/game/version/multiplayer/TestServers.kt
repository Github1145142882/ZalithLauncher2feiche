package com.movtery.zalithlauncher.game.version.multiplayer

import com.movtery.zalithlauncher.utils.network.ServerAddress
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File

class TestServers {

    @Test
    fun testParseServerData() {
        val serverDat = File("E:\\Minecraft\\.minecraft\\versions\\1.21.1-Hypixel\\servers.dat")

        runBlocking {
            val list = parseServerData(serverDat)
            val jobs = list.map { data ->
                println("ServerInfo: name = ${data.name}, ip = ${data.ip}, icon = ${data.icon}")
                launch {
                    runCatching {
                        val resolvedAddress = data.ip.resolve()
                        val result = pingServer(resolvedAddress)
                        println("ip = ${data.ip}, Ping = ${result.pingMs}, status = ${result.status}")
                    }.onFailure {
                        it.printStackTrace()
                    }
                }
            }

            jobs.joinAll()
        }
    }

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