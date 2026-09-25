package com.noor.quran.importer

import com.noor.quran.common.QuranSequencing
import com.noor.quran.common.TanzilMetadataParser
import com.noor.quran.common.TanzilTxt2Parser
import com.noor.quran.common.TanzilTxtParser
import com.noor.quran.common.TanzilXmlParser
import java.io.File

fun main() {
    val sourcesDir = File(systemProperty("noor.sourcesDir"))
    val schemaJson = File(systemProperty("noor.schemaJson"))
    val assetsDir = File(systemProperty("noor.assetsDir"))

    require(sourcesDir.isDirectory) { "sources dir missing: $sourcesDir" }
    require(schemaJson.isFile) { "room schema json missing: $schemaJson" }

    println("== Quran offline database builder ==")
    println("  sources: $sourcesDir")
    println("  schema : $schemaJson")
    println("  assets : $assetsDir")

    Provenance.verifySourceHashes(sourcesDir)

    println("parsing Tanzil streams...")
    val xml = TanzilXmlParser.parse(File(sourcesDir, "uthmani.xml"))
    val txt = TanzilTxtParser.parse(File(sourcesDir, "uthmani.txt"))
    val txt2 = TanzilTxt2Parser.parse(File(sourcesDir, "uthmani_txt2.txt"))
    val metadata = TanzilMetadataParser.parse(File(sourcesDir, "quran-data.xml"))

    Validator.validate(xml, txt, txt2, metadata)
    println("validation OK: ${xml.surahs.size} surahs, ${xml.totalAyahs} ayahs (txt == txt2 == xml streams)")

    val ordered = xml.ayahs()
    val attributes = QuranSequencing.computeAttributes(ordered, metadata)
    val startPages = QuranSequencing.surahStartPages(attributes)

    require(attributes.indices.all { attributes[it].globalAyahNumber == it + 1 }) { "global numbering broken" }
    require(attributes.all { it.juz in 1..30 && it.pageNumber in 1..604 }) { "juz/page out of range" }

    val schema = RoomSchema.fromJson(schemaJson)

    val surahRows = buildSurahRows(xml, metadata, startPages)
    val ayahRows = buildAyahRows(ordered, attributes, xml)

    assertColumnContract(schema, surahRows.first(), "surahs")
    assertColumnContract(schema, ayahRows.first(), "ayahs")

    val dbFile = File(assetsDir, "quran_reader.db")
    val result = QuranDatabaseWriter.write(dbFile, schema, surahRows, ayahRows)
    val databaseSha256 = Sha256.of(dbFile)

    Provenance.write(assetsDir, schema, databaseSha256, sourcesDir)
    Provenance.writeTanzilNotice(sourcesDir, assetsDir)

    println("write complete:")
    println("  surahs=${result.surahCount} ayahs=${result.ayahCount}")
    println("  fts=${result.ftsCounts}")
    println("  integrity=${result.integrityCheck}")
    println("  identityHash=${result.identityHash}")
    println("  databaseSha256=$databaseSha256")
    println("  database=$dbFile")
    println("DONE")
}

private fun systemProperty(name: String): String =
    System.getProperty(name) ?: error("system property '$name' is required")

private fun assertColumnContract(schema: RoomSchema, row: Map<String, Any?>, table: String) {
    val schemaColumns = schema.entity(table).columnNames.toSet()
    val rowColumns = row.keys.toSet()
    require(schemaColumns == rowColumns) {
        "column contract mismatch for `$table`: schema=$schemaColumns row=$rowColumns"
    }
}