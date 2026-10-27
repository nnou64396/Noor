package dz.techvision.noor.data.remote.prayer

/**
 * Raw AlAdhan network payloads. These stay strictly in the remote layer —
 * the UI never sees them (see [PrayerTimesMapper]).
 */
data class TimingsResponse(
    val code: Int,
    val status: String?,
    val data: TimingsData?,
)

data class TimingsData(
    val timings: Map<String, String>?,
    val date: TimingsDate?,
    val meta: TimingsMeta?,
)

data class TimingsDate(
    val hijri: HijriDate?,
    val gregorian: GregorianDate? = null,
)

data class HijriDate(
    val readable: String?,
)

data class GregorianDate(
    val date: String?,
)

data class TimingsMeta(
    val latitude: Double?,
    val longitude: Double?,
    val timezone: String?,
)
