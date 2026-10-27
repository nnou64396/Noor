package com.noor.quran.importer

import com.noor.quran.common.ArabicSearchNormalizer
import com.noor.quran.common.QuranMetadata
import com.noor.quran.common.QuranText
import com.noor.quran.common.TanzilMetadataParser
import com.noor.quran.common.TanzilTxt2Parser
import com.noor.quran.common.TanzilTxtParser
import com.noor.quran.common.TanzilXmlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.sql.DriverManager

/**
 * Integration tests against the real Tanzil files in `_sources` plus the exported
 * Room schema JSON. Guards the pipeline end to end: parsing -> validation -> the
 * exact row maps -> a byte-deterministic database whose FTS corpus actually answers
 * prefix searches and whose `room_master_table` matches what Room will verify on open.
 */
class QuranDatabaseBuilderTest {

    private lateinit var sourcesDir: File

    private data class Parsed(
        val xml: QuranText,
        val txt: List<String>,
        val txt2: List<com.noor.quran.common.QuranAyah>,
        val metadata: QuranMetadata,
    )

    @Before
    fun setUp() {
        sourcesDir = File("_sources")
        check(sourcesDir.isDirectory) { "cannot find _sources; run from tools/quran/importer" }
    }

    private fun schemaFile(): File =
        File("../../../app/schemas/dz.techvision.noor.data.local.db.quran.QuranReaderDatabase/1.json")
            .also { check(it.isFile) { "room schema json not found: $it" } }

    private fun parse(): Parsed = Parsed(
        xml = TanzilXmlParser.parse(File(sourcesDir, "uthmani.xml")),
        txt = TanzilTxtParser.parse(File(sourcesDir, "uthmani.txt")),
        txt2 = TanzilTxt2Parser.parse(File(sourcesDir, "uthmani_txt2.txt")),
        metadata = TanzilMetadataParser.parse(File(sourcesDir, "quran-data.xml")),
    )

    @Test
    fun realSources_passValidation() {
        val p = parse()
        Validator.validate(p.xml, p.txt, p.txt2, p.metadata)
        assertEquals(114, p.xml.surahs.size)
        assertEquals(6236, p.xml.totalAyahs)
        assertEquals(6236, p.txt.size)
        assertEquals(6236, p.txt2.size)
        assertEquals(6236, p.metadata.totalAyahCount)
    }

    @Test
    fun txtPlainStream_matchesXmlComposedDisplayText() {
        val p = parse()
        val ordered = p.xml.ayahs()
        val display = ordered.map { p.xml.displayText(it.surahNumber, it.ayahNumberInSurah) }
        assertEquals(p.txt, display)
    }

    @Test
    fun rebuild_isByteDeterministic_andEqualsCommittedAsset() {
        val p = parse()
        val schema = RoomSchema.fromJson(schemaFile())
        val ordered = p.xml.ayahs()
        val attributes = com.noor.quran.common.QuranSequencing.computeAttributes(ordered, p.metadata)
        val startPages = com.noor.quran.common.QuranSequencing.surahStartPages(attributes)
        val rows = RowSets(
            surahs = buildSurahRows(p.xml, p.metadata, startPages),
            ayahs = buildAyahRows(ordered, attributes, p.xml),
        )

        val dir = Files.createTempDirectory("quran-build").toFile()
        val first = File(dir, "first.db")
        val second = File(dir, "second.db")
        QuranDatabaseWriter.write(first, schema, rows.surahs, rows.ayahs)
        QuranDatabaseWriter.write(second, schema, rows.surahs, rows.ayahs)

        assertEquals(Sha256.of(first), Sha256.of(second))

        val committedAsset = File("../../../app/src/main/assets/quran/quran_reader.db")
        if (committedAsset.isFile) {
            assertEquals("fresh build must reproduce the committed asset", Sha256.of(committedAsset), Sha256.of(first))
        }
    }

    @Test
    fun database_matchesRoomExpectations_andServesFtsSearch() {
        val p = parse()
        val schema = RoomSchema.fromJson(schemaFile())
        val ordered = p.xml.ayahs()
        val attributes = com.noor.quran.common.QuranSequencing.computeAttributes(ordered, p.metadata)
        val startPages = com.noor.quran.common.QuranSequencing.surahStartPages(attributes)
        val rows = RowSets(
            surahs = buildSurahRows(p.xml, p.metadata, startPages),
            ayahs = buildAyahRows(ordered, attributes, p.xml),
        )
        val dir = Files.createTempDirectory("quran-build").toFile()
        val db = File(dir, "quran_reader.db")
        val result = QuranDatabaseWriter.write(db, schema, rows.surahs, rows.ayahs)

        assertEquals("ok", result.integrityCheck)
        assertEquals(114, result.surahCount)
        assertEquals(6236, result.ayahCount)
        assertEquals(6236, result.ftsCounts["ayahs_fts"])

        DriverManager.getConnection("jdbc:sqlite:" + db.absolutePath).use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("SELECT identity_hash FROM room_master_table LIMIT 1").use { rs ->
                    assertTrue(rs.next())
                    assertEquals(schema.identityHash, rs.getString(1))
                }
                st.executeQuery("PRAGMA user_version").use { rs ->
                    assertTrue(rs.next())
                    assertEquals(1, rs.getInt(1))
                }
                st.executeQuery("SELECT basmala FROM surahs WHERE surah_number = 2").use { rs ->
                    assertTrue(rs.next())
                    assertNotNull(rs.getString(1))
                }
                st.executeQuery("SELECT basmala FROM surahs WHERE surah_number IN (1, 9) ORDER BY surah_number").use { rs ->
                    assertTrue(rs.next())
                    assertNull(rs.getString(1))
                    assertTrue(rs.next())
                    assertNull(rs.getString(1))
                }
                // ayah 1:1 must carry a basmala-prefixed display line, matching the txt stream
                st.executeQuery("SELECT text_uthmani FROM ayahs WHERE global_ayah_number = 1").use { rs ->
                    assertTrue(rs.next())
                    assertEquals(p.txt[0], rs.getString(1))
                }
            }
            // FTS corpus must actually answer a normalized prefix query
            val probe = ArabicSearchNormalizer.normalize("الرحمن")
            conn.createStatement().use { st ->
                st.executeQuery(
                    "SELECT count(*) FROM ayahs WHERE global_ayah_number IN " +
                        "(SELECT rowid FROM ayahs_fts WHERE ayahs_fts MATCH '$probe*')",
                ).use { rs ->
                    assertTrue(rs.next())
                    assertTrue("expected hits for '$probe*'", rs.getInt(1) > 0)
                }
            }
        }
        assertNotEquals("", result.identityHash)
    }
}
