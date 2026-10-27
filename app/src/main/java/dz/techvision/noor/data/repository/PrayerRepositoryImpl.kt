package dz.techvision.noor.data.repository

import dz.techvision.noor.data.local.PrayerCacheKey
import dz.techvision.noor.data.local.db.PrayerEntityMapper
import dz.techvision.noor.data.local.db.PrayerTimesDao
import dz.techvision.noor.data.remote.prayer.PrayerApi
import dz.techvision.noor.data.remote.prayer.PrayerTimesMapper
import dz.techvision.noor.domain.model.CalculationMethod
import dz.techvision.noor.domain.model.CalculationMethodConfig
import dz.techvision.noor.domain.model.DailyPrayerSchedule
import dz.techvision.noor.domain.model.GeographicPoint
import dz.techvision.noor.domain.model.PrayerDataException
import dz.techvision.noor.domain.model.PrayerErrorKind
import dz.techvision.noor.domain.repository.PrayerRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Instant
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
        school: Int,
    ): DailyPrayerSchedule? = withContext(ioDispatcher) {
        val entity = dao.get(PrayerCacheKey.of(date, point, method, school)) ?: return@withContext null
        PrayerEntityMapper.toDomain(entity)
    }

    override suspend fun getCachedFetchedAt(
        date: LocalDate,
        point: GeographicPoint,
        method: CalculationMethod,
        school: Int,
    ): Instant? = withContext(ioDispatcher) {
        dao.get(PrayerCacheKey.of(date, point, method, school))
            ?.fetchedAtEpochMillis
            ?.let { Instant.ofEpochMilli(it) }
    }

    override suspend fun refresh(
        date: LocalDate,
        point: GeographicPoint,
        method: CalculationMethod,
        school: Int,
    ): DailyPrayerSchedule = withContext(ioDispatcher) {
        val response = apiCall {
            api.getTimings(
                date = date.format(datePathFormatter),
                latitude = point.latitude,
                longitude = point.longitude,
                method = method.apiId,
                school = school,
            )
        }

        if (response.code != 200 || response.status != "OK") {
            throw PrayerDataException(PrayerErrorKind.HTTP)
        }

        val schedule = mapper.map(response, date, method)
            ?: throw PrayerDataException(PrayerErrorKind.MALFORMED)

        val entity = PrayerEntityMapper.toEntity(schedule)
            .copy(cacheKey = PrayerCacheKey.of(date, point, method, school))

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
