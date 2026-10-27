package dz.techvision.noor.data.local.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import dz.techvision.noor.data.local.PrayerCacheKey
import dz.techvision.noor.domain.model.CalculationMethod
import dz.techvision.noor.domain.model.GeographicPoint
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Proves the prayer-times cache cannot hold a precise device location, and that
 * the v1 -> v2 migration removes the coordinate columns that used to.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PrayerDatabasePrivacyTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    private val date = LocalDate.of(2026, 9, 24)
    private val point = GeographicPoint(36.753800, 3.058800)
    private val method = CalculationMethod.MUSLIM_WORLD_LEAGUE

    private var inMemory: NoorDatabase? = null

    @Before
    fun setUp() {
        context.deleteDatabase(DB_NAME)
    }

    @After
    fun tearDown() {
        inMemory?.close()
        inMemory = null
        context.deleteDatabase(DB_NAME)
    }

    private fun newInMemoryDatabase(): NoorDatabase =
        Room.inMemoryDatabaseBuilder(context, NoorDatabase::class.java)
            .allowMainThreadQueries()
            .build()
            .also { inMemory = it }

    private fun columnsOf(db: androidx.sqlite.db.SupportSQLiteDatabase, table: String): List<String> =
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name"))) }
        }

    private fun sampleEntity(cacheKey: String) = PrayerTimesEntity(
        cacheKey = cacheKey,
        dateIso = date.toString(),
        hijriReadable = "12 Rabi' al-awwal 1448",
        timezone = "Africa/Algiers",
        methodId = method.apiId,
        fajr = "05:20",
        sunrise = "06:20",
        dhuhr = "12:44",
        asr = "16:30",
        sunset = "19:40",
        maghrib = "19:40",
        isha = "21:00",
        fetchedAtEpochMillis = 1_772_000_000_000L,
    )

    @Test
    fun `the entity declares no coordinate field at all`() {
        val fieldNames = PrayerTimesEntity::class.java.declaredFields
            .filterNot { it.isSynthetic }
            .map { it.name.lowercase() }

        listOf("latitude", "longitude", "lat", "lng", "lon", "latlng", "coordinates", "location").forEach {
            assertFalse("PrayerTimesEntity must not declare '$it'", fieldNames.contains(it))
        }
    }

    @Test
    fun `the live table has no coordinate column`() {
        val database = newInMemoryDatabase()
        val columns = columnsOf(database.openHelper.writableDatabase, "prayer_times")

        assertTrue("expected the prayer_times table to exist", columns.isNotEmpty())
        listOf("latitude", "longitude").forEach {
            assertFalse("prayer_times must not have a '$it' column: $columns", columns.contains(it))
        }
        assertTrue(
            "prayer_times lost required columns: $columns",
            columns.containsAll(listOf("cacheKey", "dateIso", "timezone", "methodId", "fajr", "fetchedAtEpochMillis")),
        )
    }

    @Test
    fun `a round trip through the database stores no coordinate anywhere`() {
        val database = newInMemoryDatabase()
        val key = PrayerCacheKey.of(date, point, method)

        val stored = runBlocking {
            database.prayerTimesDao().upsert(sampleEntity(key))
            database.prayerTimesDao().get(key)
        }
        assertNotNull(stored)
        assertEquals(date.toString(), stored!!.dateIso)

        val everyValue = PrayerTimesEntity::class.java.declaredFields
            .filterNot { it.isSynthetic }
            .joinToString("|") { field ->
                "${field.name}=${field.apply { isAccessible = true }.get(stored)}"
            }
        listOf("36.7538", "36.75", "3.0588", "3.05").forEach { coordinate ->
            assertFalse("stored row leaked '$coordinate': $everyValue", everyValue.contains(coordinate))
        }
    }

    @Test
    fun `the primary key column holds only an opaque digest`() {
        val key = PrayerCacheKey.of(date, point, method)

        assertEquals("cache key should be a sha-256 hex digest", 64, key.length)
        assertTrue(key.all { it in "0123456789abcdef" })
        assertFalse(key.contains("36.75"))
        assertFalse(key.contains("3.05"))
    }

    @Test
    fun `the cache key still separates different locations and methods`() {
        val base = PrayerCacheKey.of(date, point, method)

        assertEquals("the key must be deterministic", base, PrayerCacheKey.of(date, point, method))
        assertTrue("a different city must produce a different key", base != PrayerCacheKey.of(date, GeographicPoint(48.85, 2.35), method))
        assertTrue("a different method must produce a different key", base != PrayerCacheKey.of(date, point, CalculationMethod.EGYPTIAN_GENERAL_AUTHORITY))
        assertTrue("a different day must produce a different key", base != PrayerCacheKey.of(date.plusDays(1), point, method))
    }

    @Test
    fun `migration from version 1 drops the coordinate columns and the legacy row`() {
        seedVersionOneDatabaseWithAPreciseLocation()

        val migrated = Room.databaseBuilder(context, NoorDatabase::class.java, DB_NAME)
            .addMigrations(NoorDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        val columns = columnsOf(migrated.openHelper.writableDatabase, "prayer_times")
        assertFalse("latitude survived the migration: $columns", columns.contains("latitude"))
        assertFalse("longitude survived the migration: $columns", columns.contains("longitude"))
        assertTrue(
            "migration lost required columns: $columns",
            columns.containsAll(listOf("cacheKey", "fajr", "fetchedAtEpochMillis")),
        )

        val freshKey = PrayerCacheKey.of(date, point, method)
        val storedAfterMigration = runBlocking {
            // The v1 row held the coordinates, so it is intentionally discarded.
            assertNull(migrated.prayerTimesDao().get(LEGACY_KEY))

            // The migrated table is still usable for a coordinate-free row.
            migrated.prayerTimesDao().upsert(sampleEntity(freshKey))
            migrated.prayerTimesDao().get(freshKey)
        }
        assertNotNull(storedAfterMigration)

        migrated.close()
    }

    @Test
    fun `migration is declared from version 1 to version 2`() {
        assertEquals(1, NoorDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, NoorDatabase.MIGRATION_1_2.endVersion)
    }

    /**
     * Writes a database file shaped exactly like the version 1 schema, including
     * the two coordinate columns and a row holding a precise location.
     */
    private fun seedVersionOneDatabaseWithAPreciseLocation() {
        val legacy = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(DB_NAME), null)
        legacy.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `prayer_times` (
                `cacheKey` TEXT NOT NULL,
                `dateIso` TEXT NOT NULL,
                `hijriReadable` TEXT,
                `timezone` TEXT NOT NULL,
                `methodId` INTEGER NOT NULL,
                `latitude` REAL NOT NULL,
                `longitude` REAL NOT NULL,
                `fajr` TEXT NOT NULL,
                `sunrise` TEXT NOT NULL,
                `dhuhr` TEXT NOT NULL,
                `asr` TEXT NOT NULL,
                `sunset` TEXT NOT NULL,
                `maghrib` TEXT NOT NULL,
                `isha` TEXT NOT NULL,
                `fetchedAtEpochMillis` INTEGER NOT NULL,
                PRIMARY KEY(`cacheKey`)
            )
            """.trimIndent(),
        )
        legacy.execSQL(
            """
            INSERT INTO `prayer_times`
              (`cacheKey`,`dateIso`,`hijriReadable`,`timezone`,`methodId`,`latitude`,`longitude`,
               `fajr`,`sunrise`,`dhuhr`,`asr`,`sunset`,`maghrib`,`isha`,`fetchedAtEpochMillis`)
            VALUES ('$LEGACY_KEY','2026-09-24',NULL,'Africa/Algiers',3,36.753800,3.058800,
                    '05:20','06:20','12:44','16:30','19:40','19:40','21:00',1)
            """.trimIndent(),
        )
        legacy.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
        legacy.execSQL("INSERT INTO room_master_table (id, identity_hash) VALUES (42, 'legacy-v1-hash')")
        legacy.version = 1
        legacy.close()
    }

    private companion object {
        const val DB_NAME = "noor.db"
        const val LEGACY_KEY = "prayer|2026-09-24|36.75|3.05|3"
    }
}

