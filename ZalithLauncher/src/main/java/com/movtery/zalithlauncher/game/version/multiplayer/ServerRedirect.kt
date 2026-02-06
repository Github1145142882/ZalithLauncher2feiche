package com.movtery.zalithlauncher.game.version.multiplayer

import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import com.movtery.zalithlauncher.utils.network.ServerAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.minidns.hla.DnssecResolverApi
import org.minidns.record.SRV
import java.util.Random
import kotlin.collections.filter

/**
 * 在默认端口尝试 SRV 重定向
 */
/**
 * 在默认端口尝试 SRV 重定向
 */
suspend fun lookupRedirect(original: ServerAddress): ServerAddress? {
    if (original.port != ServerAddress.DEFAULT_PORT) return null

    return try {
        val tcpLink = "_minecraft._tcp.${original.getASCIIHost("")}"

        val result = withContext(Dispatchers.IO) {
            DnssecResolverApi.INSTANCE.resolveSrv(tcpLink)
        }

        val srvRecords = result.answers
        if (srvRecords.isEmpty()) return null

        val selected = selectSrvRecord(srvRecords)

        ServerAddress(
            selected.target.toString(),
            selected.port
        )
    } catch (e: Exception) {
        lWarning("Unable to redirect server $original", e)
        null
    }
}

private fun selectSrvRecord(records: Set<SRV>): SRV {
    val minPriority = records.minOf { it.priority }
    val samePriority = records.filter { it.priority == minPriority }

    val totalWeight = samePriority.sumOf { it.weight }
    if (totalWeight <= 0) return samePriority.random()

    val rand = Random().nextInt(totalWeight)
    var acc = 0

    for (record in samePriority) {
        acc += record.weight
        if (rand < acc) {
            return record
        }
    }

    return samePriority.first()
}