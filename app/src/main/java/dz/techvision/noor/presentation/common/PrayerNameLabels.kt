package dz.techvision.noor.presentation.common

import androidx.annotation.StringRes
import dz.techvision.noor.R
import dz.techvision.noor.domain.model.PrayerName

/** Localized label for each canonical prayer, resolved from resources. */
@get:StringRes
val PrayerName.labelRes: Int
    get() = when (this) {
        PrayerName.FAJR -> R.string.prayer_fajr
        PrayerName.SUNRISE -> R.string.prayer_sunrise
        PrayerName.DHUHR -> R.string.prayer_dhuhr
        PrayerName.ASR -> R.string.prayer_asr
        PrayerName.SUNSET -> R.string.prayer_sunset
        PrayerName.MAGHRIB -> R.string.prayer_maghrib
        PrayerName.ISHA -> R.string.prayer_isha
    }
