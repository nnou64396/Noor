package dz.techvision.noor.presentation.quran

import androidx.annotation.StringRes
import dz.techvision.noor.R
import dz.techvision.noor.domain.model.quran.QuranAudioError
import dz.techvision.noor.domain.model.quran.RecitationStyle
import dz.techvision.noor.domain.model.quran.Reciter
import dz.techvision.noor.domain.model.quran.ReciterNameKey
import java.util.Locale

/** Localized reciter name, resolved from resources rather than hardcoded text. */
@get:StringRes
val ReciterNameKey.labelRes: Int
    get() = when (this) {
        ReciterNameKey.ALAFASY -> R.string.quran_reciter_alafasy
        ReciterNameKey.ABDUL_BASIT -> R.string.quran_reciter_abdul_basit
        ReciterNameKey.HUSARY -> R.string.quran_reciter_husary
        ReciterNameKey.MINSHAWY -> R.string.quran_reciter_minshawy
        ReciterNameKey.AS_SUDAIS -> R.string.quran_reciter_as_sudais
        ReciterNameKey.AYYOUB -> R.string.quran_reciter_ayyoub
        ReciterNameKey.AD_DUSSARY -> R.string.quran_reciter_ad_dussary
    }

@get:StringRes
val RecitationStyle.labelRes: Int
    get() = when (this) {
        RecitationStyle.MURATTAL -> R.string.quran_style_murattal
        RecitationStyle.MUJAWWAD -> R.string.quran_style_mujawwad
        RecitationStyle.MUALLIM -> R.string.quran_style_muallim
    }

@get:StringRes
val QuranAudioError.messageRes: Int
    get() = when (this) {
        QuranAudioError.NETWORK -> R.string.quran_audio_error_network
        QuranAudioError.SOURCE_UNAVAILABLE -> R.string.quran_audio_error_source
        QuranAudioError.UNKNOWN -> R.string.quran_audio_error_unknown
    }

/**
 * Audio timestamps use ASCII digits on purpose: playback positions are technical
 * values, and the same string is reused from tests and accessibility labels.
 */
fun formatAudioTimestamp(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0L) / 1000L).toInt()
    return String.format(Locale.ROOT, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

