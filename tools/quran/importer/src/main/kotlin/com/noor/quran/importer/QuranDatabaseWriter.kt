package com.noor.quran.importer

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement

/**
 * Builds the prebuilt SQLite database by executing Room's own exported schema DDL
 * (tables + indices + setup queries/triggers), inserting the validated data, then
 * seeding `room_master_table` with the identity hash Room expects.
 */
object QuranDatabaseWriter {

    fun write(
        dbFile: File,
        schema: RoomSchema,
        surahRows: List<Map<String, Any?>>,
        ayahRows: List<Map<String, Any?>>,
    ): WriteResult {
        dbFile.parentFile?.mkdirs()
        dbFile.delete()

        Class.forName("org.sqlite.JDBC")
        DriverManager.getConnection("jdbc:sqlite:" + dbFile.absolutePath).use { conn ->
            conn.autoCommit = false
            try {
                // Room's exported DDL uses a `${TABLE_NAME}` placeholder resolved per entity.
                for (entity in schema.entities) {
                    conn.createStatement().use { it.execute(entity.createSql.replace("\${TABLE_NAME}", entity.tableName)) }
                    for (indexSql in entity.indexCreateSql) {
                        conn.createStatement().use { it.execute(indexSql.replace("\${TABLE_NAME}", entity.tableName)) }
                    }
                }
                for (sql in schema.syncTriggers) {
                    conn.createStatement().use { it.execute(sql) }
                }
                for (sql in schema.setupQueries) {
                    conn.createStatement().use { it.execute(sql) }
                }
                insertRows(conn, schema, "surahs", surahRows)
                insertRows(conn, schema, "ayahs", ayahRows)
                conn.createStatement().use { it.execute("PRAGMA user_version = ${schema.version}") }
                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }

            val integrity = scalar(conn, "PRAGMA integrity_check")
            require(integrity == "ok") { "PRAGMA integrity_check returned '$integrity'" }
            val storedVersion = scalar(conn, "PRAGMA user_version")
            require(storedVersion.toInt() == schema.version) { "user_version $storedVersion != ${schema.version}" }
            val surahCount = scalarInt(conn, "SELECT count(*) FROM `surahs`")
            val ayahCount = scalarInt(conn, "SELECT count(*) FROM `ayahs`")
            require(surahCount == Validator.EXPECTED_SURAH_COUNT) { "surahs=$surahCount" }
            require(ayahCount == Validator.EXPECTED_AYAH_COUNT) { "ayahs=$ayahCount" }
            val ftsCounts = schema.ftsTableNames().associateWith { t ->
                scalarInt(conn, "SELECT count(*) FROM `$t`")
            }
            require(ftsCounts.all { it.value == Validator.EXPECTED_AYAH_COUNT }) {
                "fts not fully populated: $ftsCounts"
            }
            val hash = scalar(conn, "SELECT identity_hash FROM `room_master_table` LIMIT 1")
            require(hash == schema.identityHash) { "room_master_table hash mismatch: $hash vs ${schema.identityHash}" }

            return WriteResult(
                surahCount = surahCount,
                ayahCount = ayahCount,
                ftsCounts = ftsCounts,
                integrityCheck = integrity,
                identityHash = hash,
            )
        }
    }

    private fun insertRows(
        conn: Connection,
        schema: RoomSchema,
        tableName: String,
        rows: List<Map<String, Any?>>,
    ) {
        val columns = schema.entity(tableName).columnNames
        val sql = "INSERT INTO `$tableName` (${columns.joinToString(",") { "`$it`" }}) " +
            "VALUES (${columns.joinToString(",") { "?" }})"
        conn.prepareStatement(sql).use { ps ->
            for (row in rows) {
                columns.forEachIndexed { index, column ->
                    bind(ps, index + 1, row[column])
                }
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    private fun bind(ps: PreparedStatement, index: Int, value: Any?) {
        when (value) {
            null -> ps.setObject(index, null)
            is Int -> ps.setInt(index, value)
            is Boolean -> ps.setInt(index, if (value) 1 else 0)
            is String -> ps.setString(index, value)
            else -> ps.setString(index, value.toString())
        }
    }

    private fun scalar(conn: Connection, sql: String): String {
        conn.createStatement().use { st ->
            st.executeQuery(sql).use { rs ->
                check(rs.next())
                return rs.getString(1)
            }
        }
    }

    private fun scalarInt(conn: Connection, sql: String): Int = scalar(conn, sql).toInt()
}

data class WriteResult(
    val surahCount: Int,
    val ayahCount: Int,
    val ftsCounts: Map<String, Int>,
    val integrityCheck: String,
    val identityHash: String,
)