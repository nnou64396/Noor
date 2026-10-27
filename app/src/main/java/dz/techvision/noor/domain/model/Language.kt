package dz.techvision.noor.domain.model

import androidx.annotation.StringRes
import dz.techvision.noor.R

/**
 * Languages supported by Noor.
 *
 * Arabic is the default application language.
 */
enum class Language(
    val code: String,
    val localeTag: String,
    @param:StringRes val displayNameRes: Int,
) {
    ARABIC("ar", "ar", R.string.language_arabic),
    ENGLISH("en", "en", R.string.language_english),
    FRENCH("fr", "fr", R.string.language_french);

    companion object {
        val DEFAULT: Language = ARABIC

        fun fromCode(code: String?): Language =
            entries.firstOrNull { it.code == code } ?: DEFAULT
    }
}
