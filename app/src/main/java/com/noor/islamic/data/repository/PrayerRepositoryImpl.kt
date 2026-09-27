package com.noor.islamic.data.repository

import com.noor.islamic.data.local.PrayerCacheKey
import com.noor.islamic.data.local.db.PrayerEntityMapper
import com.noor.islamic.data.local.db.PrayerTimesDao
import com.noor.islamic.data.remote.prayer.PrayerApi
import com.noor.islamic.data.remote.prayer.PrayerTimesMapper
import com.noor.islamic.domain.model.CalculationMethod
import com.noor.islamic.domain.model.CalculationMethodConfig
import com.noor.islamic.domain.model.DailyPrayerSchedule
import com.noor.islamic.domain.model.GeographicPoint
import com.noor.islamic.domain.model.PrayerDataException
import com.noor.islamic.domain.model.PrayerErrorKind
import com.noor.islamic.domain.repository.PrayerRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Offline-first prayer repository: fast Room reads + AlAdhan refresh.
 */
class PrayerRepositoryImpl(
    private val api: PrayerApi,
    private val dao: PrayerTimesDao,
    private val mapper: PrayerTimesMapper = PrayerTimesMapper(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PrayerRepository {

    private val datePathFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    override suspend fun getCached(
        date: LocalDate,
        point: GeographicPoint,
        method: CalculationMethod,
    ): DailyPrayerSchedule? = withContext(ioDispatcher) {
        val entity = dao.get(PrayerCacheKey.of(date, point, method)) ?: return@withContext null
        PrayerEntityMapper.toDomain(entity)
    }

    override suspend fun refresh(
        date: LocalDate,
        point: GeographicPoint,
        method: CalculationMethod,
    ): DailyPrayerSchedule = withContext(ioDispatcher) {
        val response = apiCall {
            api.getTimings(
                date = date.format(datePathFormatter),
                latitude = point.latitude,
                longitude = point.longitude,
                method = method.apiId,
                school = CalculationMethodConfig.defaultSchool,
            )
        }

        if (response.code != 200 || response.status != "OK") {
            throw PrayerDataException(PrayerErrorKind.HTTP)
        }

        val schedule = mapper.map(response, date)
            ?: throw PrayerDataException(PrayerErrorKind.MALFORMED)

        val entity = PrayerEntityMapper.toEntity(schedule)
            .copy(cacheKey = PrayerCacheKey.of(date, point, method))

        dao.upsert(entity)
        dao.pruneBefore(System.currentTimeMillis() - CACHE_RETENTION_MILLIS)
        schedule
    }

    private suspend fun <T> apiCall(block: suspend () -> T): T =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            throw PrayerDataException(PrayerErrorKind.HTTP, e)
        } catch (e: SocketTimeoutException) {
            throw PrayerDataException(PrayerErrorKind.TIMEOUT, e)
        } catch (e: IOException) {
            throw PrayerDataException(PrayerErrorKind.NETWORK, e)
        } catch (e: PrayerDataException) {
            throw e
        } catch (e: Exception) {
            throw PrayerDataException(PrayerErrorKind.UNKNOWN, e)
        }

    private companion object {
        /** Offline usability window: entries older than this are pruned. */
        const val CACHE_RETENTION_MILLIS = 30L * 24L * 60L * 60L * 1000L
    }
}