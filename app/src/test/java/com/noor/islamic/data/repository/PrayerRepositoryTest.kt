package com.noor.islamic.data.repository

import com.noor.islamic.data.local.PrayerCacheKey
import com.noor.islamic.data.local.db.PrayerEntityMapper
import com.noor.islamic.data.local.db.PrayerTimesDao
import com.noor.islamic.data.local.db.PrayerTimesEntity
import com.noor.islamic.data.remote.prayer.PrayerApi
import com.noor.islamic.data.remote.prayer.TimingsResponse
import com.noor.islamic.domain.model.CalculationMethod
import com.noor.islamic.domain.model.CalculationMethodConfig
import com.noor.islamic.domain.model.AsrSchool
import com.noor.islamic.domain.model.DailyPrayerSchedule
import com.noor.islamic.domain.model.GeographicPoint
import com.noor.islamic.domain.model.PrayerDataException
import com.noor.islamic.domain.model.PrayerName
import com.noor.islamic.domain.model.PrayerTime
import com.noor.islamic.domain.model.PrayerErrorKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.test.runTest
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime

class PrayerRepositoryTest {

    private val method = CalculationMethodConfig.defaultMethod
    private val date = LocalDate.of(2026, 9, 24)
    private val point = GeographicPoint(36.75, 3.05)
    private val key = PrayerCacheKey.of(date, point, method)

    private val schedule = DailyPrayerSchedule(
        date = date,
        hijriReadable = "12 Rabi' al-awwal 1448",
        timezone = "Africa/Algiers",
        method = method,
        prayers = listOf(
            PrayerTime(PrayerName.FAJR, LocalTime.of(5, 20)),
            PrayerTime(PrayerName.SUNRISE, LocalTime.of(6, 20)),
            PrayerTime(PrayerName.DHUHR, LocalTime.of(12, 44)),
            PrayerTime(PrayerName.ASR, LocalTime.of(16, 30)),
            PrayerTime(PrayerName.SUNSET, LocalTime.of(19, 40)),
            PrayerTime(PrayerName.MAGHRIB, LocalTime.of(19, 40)),
            PrayerTime(PrayerName.ISHA, LocalTime.of(21, 0)),
        ),
    )

    private class FakeApi(var response: TimingsResponse? = null, var error: Exception? = null) : PrayerApi {
        var lastRequest: Request? = null

        data class Request(
            val date: String,
            val latitude: Double,
            val longitude: Double,
            val method: Int,
            val school: Int,
        )

        override suspend fun getTimings(
            date: String,
            latitude: Double,
            longitude: Double,
            method: Int,
            school: Int,
        ): TimingsResponse {
            lastRequest = Request(date, latitude, longitude, method, school)
            error?.let { throw it }
            return response ?: throw IOException("no response configured")
        }
    }

    private class InMemoryDao : PrayerTimesDao {
        val store = mutableMapOf<String, PrayerTimesEntity>()

        override suspend fun get(cacheKey: String): PrayerTimesEntity? = store[cacheKey]

        override suspend fun upsert(entity: PrayerTimesEntity) {
            store[entity.cacheKey] = entity
        }

        override suspend fun pruneBefore(cutoffMillis: Long) {
            store.values.removeAll { it.fetchedAtEpochMillis < cutoffMillis }
        }
    }

    private fun validResponse() = TimingsResponse(
        code = 200,
        status = "OK",
        data = com.noor.islamic.data.remote.prayer.TimingsData(
            timings = mapOf(
                "Fajr" to "05:20", "Sunrise" to "06:20", "Dhuhr" to "12:44",
                "Asr" to "16:30", "Sunset" to "19:40", "Maghrib" to "19:40",
                "Isha" to "21:00",
            ),
            date = com.noor.islamic.data.remote.prayer.TimingsDate(
                hijri = com.noor.islamic.data.remote.prayer.HijriDate("12 Rabi' al-awwal 1448"),
            ),
            meta = com.noor.islamic.data.remote.prayer.TimingsMeta(
                latitude = point.latitude,
                longitude = point.longitude,
                timezone = "Africa/Algiers",
            ),
        ),
    )

    @Test
    fun `cache is empty before the first refresh`() = runTest {
        val repository = PrayerRepositoryImpl(FakeApi(), InMemoryDao())
        assertNull(repository.getCached(date, point, method))
    }

    @Test
    fun `refresh stores the fresh schedule and returns it`() = runTest {
        val dao = InMemoryDao()
        val repository = PrayerRepositoryImpl(FakeApi(response = validResponse()), dao)

        val fresh = repository.refresh(date, point, method)

        assertEquals(schedule.date, fresh.date)
        assertEquals(7, fresh.prayers.size)
        assertNotNull(dao.store[key])
        assertEquals(7, dao.store[key]!!.let { PrayerEntityMapper.toDomain(it) }!!.prayers.size)
    }

    @Test
    fun `getCached returns the stored schedule offline`() = runTest {
        val dao = InMemoryDao()
        val repository = PrayerRepositoryImpl(FakeApi(response = validResponse()), dao)
        repository.refresh(date, point, method)

        val cached = repository.getCached(date, point, method)

        assertEquals(PrayerName.FAJR, cached?.prayers?.first()?.prayer)
    }

    @Test
    fun `getCachedFetchedAt reports the store time and null when empty`() = runTest {
        val dao = InMemoryDao()
        val repository = PrayerRepositoryImpl(FakeApi(response = validResponse()), dao)

        assertNull(repository.getCachedFetchedAt(date, point, method))

        repository.refresh(date, point, method)

        val fetchedAt = repository.getCachedFetchedAt(date, point, method)
        assertNotNull(fetchedAt)
        // A refresh stamps the row with "now"; it must fall inside a plausible
        // window around this test run.
        assertTrue(fetchedAt!!.toEpochMilli() >= System.currentTimeMillis() - 60_000)
    }

    @Test
    fun `a network failure leaves the cache intact`() = runTest {
        val dao = InMemoryDao()
        val repository = PrayerRepositoryImpl(FakeApi(response = validResponse()), dao)
        repository.refresh(date, point, method)

        val failing = PrayerRepositoryImpl(
            api = FakeApi(error = IOException("offline")),
            dao = dao,
        )

        val thrown = kotlin.runCatching { failing.refresh(date, point, method) }.exceptionOrNull()
        assertTrue(thrown is PrayerDataException)
        assertEquals(PrayerErrorKind.NETWORK, (thrown as PrayerDataException).kind)

        assertNotNull(dao.store[key])
        assertEquals(PrayerName.FAJR, failing.getCached(date, point, method)?.prayers?.first()?.prayer)
    }

    @Test
    fun `a malformed response maps to MALFORMED and nothing is cached`() = runTest {
        val badResponse = TimingsResponse(
            code = 200,
            status = "OK",
            data = com.noor.islamic.data.remote.prayer.TimingsData(
                timings = mapOf("Fajr" to "05:20"),
                date = null,
                meta = null,
            ),
        )
        val dao = InMemoryDao()
        val repository = PrayerRepositoryImpl(FakeApi(response = badResponse), dao)

        val thrown = kotlin.runCatching { repository.refresh(date, point, method) }.exceptionOrNull()

        assertEquals(PrayerErrorKind.MALFORMED, (thrown as PrayerDataException).kind)
        assertTrue(dao.store.isEmpty())
    }

    @Test
    fun `an http error maps to HTTP`() = runTest {
        val repository = PrayerRepositoryImpl(
            FakeApi(
                response = TimingsResponse(code = 500, status = "ERROR", data = null),
            ),
            InMemoryDao(),
        )
        val thrown = kotlin.runCatching { repository.refresh(date, point, method) }.exceptionOrNull()
        assertEquals(PrayerErrorKind.HTTP, (thrown as PrayerDataException).kind)
    }

    @Test
    fun `the cached row carries no coordinates in any column`() = runTest {
        val dao = InMemoryDao()
        val repository = PrayerRepositoryImpl(FakeApi(response = validResponse()), dao)
        repository.refresh(date, point, method)

        val stored = requireNotNull(dao.store[key])
        val serialised = PrayerTimesEntity::class.java.declaredFields
            .filterNot { it.isSynthetic }
            .joinToString(",") { field ->
                val accessible = field.apply { isAccessible = true }
                "${field.name}=${accessible.get(stored)}"
            }

        listOf("latitude", "longitude", "lat", "lng", "lon").forEach { forbidden ->
            assertTrue(
                "PrayerTimesEntity must not expose a '$forbidden' field",
                PrayerTimesEntity::class.java.declaredFields.none { it.name.equals(forbidden, true) },
            )
        }
        assertTrue(
            "Persisted prayer row must not contain the precise coordinates: $serialised",
            !serialised.contains("36.75") && !serialised.contains("3.05"),
        )
    }

    @Test
    fun `the cache key does not leak the coordinates`() = runTest {
        val key = PrayerCacheKey.of(date, point, method)

        assertTrue("cache key must not contain the latitude", !key.contains("36.75"))
        assertTrue("cache key must not contain the longitude", !key.contains("3.05"))
        assertEquals("cache key must be a bare sha-256 hex digest", 64, key.length)
        assertTrue(key.all { it in "0123456789abcdef" })
    }

    @Test
    fun `the cache key is stable for the same request and differs per location`() {
        assertEquals(PrayerCacheKey.of(date, point, method), PrayerCacheKey.of(date, point, method))
        assertTrue(
            PrayerCacheKey.of(date, point, method) !=
                PrayerCacheKey.of(date, GeographicPoint(36.751, 3.051), method),
        )
    }

    @Test
    fun `cache identity differs by calculation method and Asr school`() {
        val defaultKey = PrayerCacheKey.of(date, point, method, AsrSchool.STANDARD.apiId)

        assertTrue(
            defaultKey != PrayerCacheKey.of(date, point, CalculationMethod.TURKEY, AsrSchool.STANDARD.apiId),
        )
        assertTrue(
            defaultKey != PrayerCacheKey.of(date, point, method, AsrSchool.HANAFI.apiId),
        )
    }

    @Test
    fun `refresh sends selected method and school and caches under their identity`() = runTest {
        val api = FakeApi(response = validResponse())
        val dao = InMemoryDao()
        val repository = PrayerRepositoryImpl(api, dao)
        val selectedMethod = CalculationMethod.TURKEY
        val selectedSchool = AsrSchool.HANAFI

        repository.refresh(date, point, selectedMethod, selectedSchool.apiId)

        assertEquals(
            FakeApi.Request(
                date = "24-09-2026",
                latitude = point.latitude,
                longitude = point.longitude,
                method = selectedMethod.apiId,
                school = selectedSchool.apiId,
            ),
            api.lastRequest,
        )
        assertNotNull(dao.store[PrayerCacheKey.of(date, point, selectedMethod, selectedSchool.apiId)])
        assertNull(repository.getCached(date, point, method, AsrSchool.STANDARD.apiId))
    }

    @Test
    fun `a cached row survives a round trip with all seven timings`() = runTest {
        val dao = InMemoryDao()
        val repository = PrayerRepositoryImpl(FakeApi(response = validResponse()), dao)
        repository.refresh(date, point, method)

        val restored = requireNotNull(repository.getCached(date, point, method))
        assertEquals(date, restored.date)
        assertEquals("Africa/Algiers", restored.timezone)
        assertEquals(7, restored.prayers.size)
        assertEquals(PrayerName.entries.toList(), restored.prayers.map { it.prayer })
    }
}