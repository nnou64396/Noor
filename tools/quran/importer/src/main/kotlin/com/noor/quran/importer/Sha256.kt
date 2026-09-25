package com.noor.quran.importer

import java.io.File
import java.security.MessageDigest

object Sha256 {

    fun of(bytes: ByteArray): String = hex(MessageDigest.getInstance("SHA-256").digest(bytes))

    fun of(file: File): String = file.inputStream().use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(1 shl 16)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
        hex(digest.digest())
    }

    private fun hex(bytes: ByteArray): String =
        buildString(bytes.size * 2) {
            for (b in bytes) {
                append(HEX[(b.toInt() shr 4) and 0x0F])
                append(HEX[b.toInt() and 0x0F])
            }
        }

    private val HEX = "0123456789ABCDEF".toCharArray()
}