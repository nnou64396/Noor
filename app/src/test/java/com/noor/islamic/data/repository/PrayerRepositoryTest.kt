package com.noor.islamic.data.repository

import com.noor.islamic.data.local.PrayerCacheKey
import com.noor.islamic.data.local.db.PrayerEntityMapper
import com.noor.islamic.data.local.db.PrayerTimesDao
import com.noor.islamic.data.local.db.PrayerTimesEntity
import com.noor.islamic.data.remote.prayer.PrayerApi
import com.noor.islamic.data.remote.prayer.TimingsResponse
import com.noor.islamic.domain.model.CalculationMethod
import com.noor.islamic.domain.model.CalculationMethodConfig
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
        latitude = point.latitude,
        longitude = point.longitude,
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
        override suspend fun getTimings(
            date: String,
            latitude: Double,
            longitude: Double,
            method: Int,
            school: Int,
        ): TimingsResponse {
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
}