package com.movtery.zalithlauncher.game.version.multiplayer

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.build
import io.ktor.utils.io.core.writeFully
import io.ktor.utils.io.readByte
import io.ktor.utils.io.readFully
import io.ktor.utils.io.writeByte
import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.readByteArray

suspend fun ByteWriteChannel.writeVarInt(value: Int) {
    var v = value
    while (true) {
        if ((v and 0x7F.inv()) == 0) {
            writeByte(v.toByte())
            return
        }
        writeByte(((v and 0x7F) or 0x80).toByte())
        v = v ushr 7
    }
}

fun Sink.writeVarInt(value: Int) {
    var v = value
    while (true) {
        if ((v and 0x7F.inv()) == 0) {
            writeByte(v.toByte())
            return
        }
        writeByte(((v and 0x7F) or 0x80).toByte())
        v = v ushr 7
    }
}

suspend fun ByteReadChannel.readVarInt(): Int {
    var numRead = 0
    var result = 0
    var read: Int
    do {
        read = readByte().toInt() and 0xFF
        val value = read and 0x7F
        result = result or (value shl (7 * numRead))
        numRead++
        if (numRead > 5) error("VarInt too big")
    } while ((read and 0x80) != 0)
    return result
}

fun Sink.writeMCString(value: String) {
    val bytes = value.toByteArray(Charsets.UTF_8)
    writeVarInt(bytes.size)
    writeFully(bytes)
}

suspend fun ByteReadChannel.readMCString(): String {
    val length = readVarInt()
    val bytes = ByteArray(length)
    readFully(bytes)
    return String(bytes, Charsets.UTF_8)
}

fun buildPacket(builder: Sink.() -> Unit): ByteArray =
    Buffer().apply(builder).build().readByteArray()