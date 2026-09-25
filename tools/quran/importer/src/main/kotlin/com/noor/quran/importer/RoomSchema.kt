package com.noor.quran.importer

import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** Parsed Room schema JSON exported via KSP `room.schemaLocation`. */
data class RoomEntity(
    val tableName: String,
    val createSql: String,
    val columnNames: List<String>,
    val indexCreateSql: List<String>,
    val contentSyncTriggers: List<String>,
)

data class RoomSchema(
    val version: Int,
    val identityHash: String,
    val entities: List<RoomEntity>,
    val setupQueries: List<String>,
) {
    val tableCreateSql: List<String> get() = entities.map { it.createSql }
    val indexCreateSql: List<String> get() = entities.flatMap { it.indexCreateSql }

    /** FTS sync triggers Room generates for `contentEntity` tables. */
    val syncTriggers: List<String> get() = entities.flatMap { it.contentSyncTriggers }

    fun entity(tableName: String): RoomEntity =
        entities.first { it.tableName == tableName }

    fun ftsTableNames(): List<String> =
        entities.filter { it.createSql.contains("VIRTUAL TABLE", ignoreCase = true) }.map { it.tableName }

    companion object {
        fun fromJson(file: File): RoomSchema {
            val root = JSONObject(file.readText(Charsets.UTF_8))
            val db = root.getJSONObject("database")
            fun array(obj: JSONObject, key: String): JSONArray? = obj.optJSONArray(key)
            val entitiesJson = array(db, "entities") ?: JSONArray()
            val entities = (0 until entitiesJson.length()).map { i ->
                val e = entitiesJson.getJSONObject(i)
                val fields = e.getJSONArray("fields")
                val indicesJson = e.optJSONArray("indices")
                val indices = if (indicesJson == null) {
                    emptyList()
                } else {
                    (0 until indicesJson.length()).map { j -> indicesJson.getJSONObject(j).getString("createSql") }
                }
                val syncTriggersJson = e.optJSONArray("contentSyncTriggers")
                val triggers = if (syncTriggersJson == null) {
                    emptyList()
                } else {
                    (0 until syncTriggersJson.length()).map { syncTriggersJson.getString(it) }
                }
                RoomEntity(
                    tableName = e.getString("tableName"),
                    createSql = e.getString("createSql"),
                    columnNames = (0 until fields.length())
                        .map { j -> fields.getJSONObject(j).getString("columnName") },
                    indexCreateSql = indices,
                    contentSyncTriggers = triggers,
                )
            }
            val setupJson = array(db, "setupQueries") ?: JSONArray()
            val setup = (0 until setupJson.length()).map { setupJson.getString(it) }
            return RoomSchema(
                version = db.getInt("version"),
                identityHash = db.getString("identityHash"),
                entities = entities,
                setupQueries = setup,
            )
        }
    }
}