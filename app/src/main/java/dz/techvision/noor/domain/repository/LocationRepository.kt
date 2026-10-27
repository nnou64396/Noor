package dz.techvision.noor.domain.repository

import dz.techvision.noor.domain.model.GeographicPoint

/**
 * Reusable abstraction over Android location services, shared by Prayer
 * and Qibla (and future features). The implementation must stay coarse:
 * a single current/last-known position, no tracking, no history.
 */
interface LocationRepository {

    /**
     * Tries to resolve a current coarse position (falling back to the last
     * known fix). Returns [Result.failure] when no location is available.
     * Callers are responsible for checking permission first.
     */
    suspend fun obtainLocation(): Result<GeographicPoint>

    /**
     * Best-effort human-readable place name (locality / region) for a
     * position. Never shown as a precise street address; null when the
     * platform cannot resolve a name.
     */
    suspend fun cityLabel(point: GeographicPoint): String?
}
