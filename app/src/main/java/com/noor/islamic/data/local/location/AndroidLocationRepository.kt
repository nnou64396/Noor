package com.noor.islamic.data.local.location

import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import com.noor.islamic.domain.model.GeographicPoint
import com.noor.islamic.domain.repository.LocationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Play-Services Fused Location Provider implementation of [LocationRepository].
 *
 * Privacy: uses a single current fix (falling back to the last known one),
 * never tracks the user, never stores coordinates, and only ever sends them
 * to the prayer-times API request in the repository layer.
 */
class AndroidLocationRepository(
    private val context: Context,
) : LocationRepository {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    override suspend fun obtainLocation(): Result<GeographicPoint> = withContext(Dispatchers.IO) {
        val point: GeographicPoint = try {
            currentPoint()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            val fallback = lastKnownPointOrNull()
            if (fallback != null) {
                fallback
            } else {
                return@withContext Result.failure(
                    IllegalStateException("No current or last-known location fix available"),
                )
            }
        }
        Result.success(point)
    }

    private fun currentPoint(): GeographicPoint {
        val cancellation = CancellationTokenSource()
        val location = Tasks.await(
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellation.token,
            ),
            CURRENT_FIX_TIMEOUT_SECONDS,
            TimeUnit.SECONDS,
        )
        if (location != null) return location.toPoint()
        throw TimeoutException("No current fix")
    }

    private fun lastKnownPointOrNull(): GeographicPoint? =
        try {
            Tasks.await(
                fusedLocationClient.lastLocation,
                LAST_KNOWN_TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
            )?.toPoint()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

    override suspend fun cityLabel(point: GeographicPoint): String? = withContext(Dispatchers.IO) {
        runCatching {
            Geocoder(context, Locale.getDefault())
                .getFromLocation(point.latitude, point.longitude, 1)
                ?.firstOrNull()
                ?.let { address ->
                    address.locality
                        ?: address.subAdminArea
                        ?: address.adminArea
                }
        }.getOrNull()
    }

    private fun android.location.Location.toPoint(): GeographicPoint =
        GeographicPoint(latitude = latitude, longitude = longitude)

    private companion object {
        const val CURRENT_FIX_TIMEOUT_SECONDS = 10L
        const val LAST_KNOWN_TIMEOUT_SECONDS = 3L
    }
}