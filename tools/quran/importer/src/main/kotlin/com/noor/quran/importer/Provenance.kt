package com.noor.quran.importer

import java.io.File
import org.json.JSONObject

/** Machine-readable provenance distributed with the generated asset. */
object Provenance {

    const val SOURCE_NAME = "Tanzil Quran Text - Uthmani"
    const val SOURCE_VERSION = "v1.1 (February 2021)"
    const val LICENSE = "Creative Commons Attribution 3.0 (CC BY 3.0) - verbatim copies only, no modification"
    const val DOWNLOAD_DATE = "2026-09-24"

    val SOURCE_URLS = listOf(
        "https://tanzil.net/pub/download/index.php",
        "https://tanzil.net/res/text/metadata/quran-data.xml",
    )

    val EXPECTED_SOURCE_SHA256 = mapOf(
        "uthmani.txt" to "AC0724796CBBDA0F4801470FBBD11D0F3C5802067BAE0493466D0128B0C667AF",
        "uthmani_txt2.txt" to "BF4F57B968D03F4131C070B1E285DA9BE0E0A108A21C910E872801CA273312C8",
        "uthmani.xml" to "203F0F1BF3158B1E5BE4AB9F8F6870E570AAB6D9A626FE6192A70B75D4AFE0FD",
        "quran-data.xml" to "8867C1D88191472ADEC9DB694B3CD9F135B1A2EF580574D32CF888DCB22C5C7A",
    )

    fun verifySourceHashes(sourcesDir: File) {
        for ((name, expected) in EXPECTED_SOURCE_SHA256) {
            val file = File(sourcesDir, name)
            require(file.isFile) { "missing source file $name" }
            val actual = Sha256.of(file)
            require(actual == expected) {
                "source $name hash mismatch: expected $expected, got $actual - re-download and re-verify"
            }
        }
    }

    fun write(
        assetsDir: File,
        schema: RoomSchema,
        databaseSha256: String,
        sourcesDir: File,
    ): File {
        val json = JSONObject().apply {
            put("sourceName", SOURCE_NAME)
            put("sourceVersion", SOURCE_VERSION)
            put("license", LICENSE)
            put("downloadDate", DOWNLOAD_DATE)
            put("sourceUrls", JSONObject().put("downloadForm", SOURCE_URLS[0]).put("metadata", SOURCE_URLS[1]))
            put("sourceFileSha256", JSONObject(EXPECTED_SOURCE_SHA256))
            put("schemaVersion", schema.version)
            put("surahCount", Validator.EXPECTED_SURAH_COUNT)
            put("ayahCount", Validator.EXPECTED_AYAH_COUNT)
            put("databaseName", "quran_reader.db")
            put("databaseSha256", databaseSha256)
            put("builder", "com.noor.quran.importer (tools/quran)")
        }
        val out = File(assetsDir, "quran_provenance.json")
        out.writeText(json.toString(2) + "\n", Charsets.UTF_8)
        return out
    }

    /** Copies the Tanzil license/attribution header verbatim into a bundled asset. */
    fun writeTanzilNotice(sourcesDir: File, assetsDir: File): File {
        val out = File(assetsDir, "TANZIL.txt")
        val sb = StringBuilder()
        File(sourcesDir, "uthmani.txt").forEachLine { line ->
            if (line.trimStart().startsWith("#")) {
                sb.append(line).append('\n')
            }
        }
        out.writeText(sb.toString(), Charsets.UTF_8)
        return out
    }
}