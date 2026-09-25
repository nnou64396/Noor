package com.noor.islamic.domain.model

/** Coarse categories of prayer-data failures. Never shown verbatim to users. */
enum class PrayerErrorKind {
    NETWORK,
    TIMEOUT,
    HTTP,
    MALFORMED,
    LOCATION,
    UNKNOWN,
}

/** Typed exception thrown by the data layer when fetching prayer times fails. */
class PrayerDataException(
    val kind: PrayerErrorKind,
    cause: Throwable? = null,
) : RuntimeException(cause?.message, cause)