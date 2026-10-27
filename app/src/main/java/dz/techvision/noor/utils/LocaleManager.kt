package dz.techvision.noor.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dz.techvision.noor.domain.model.Language

/**
 * Applies the persisted language to the whole application using the
 * per-app locale APIs.
 *
 * [AppCompatActivity] hosts the Compose UI so the AndroidX backport works
 * on API 32 and below; on API 33+ it delegates to the platform.
 */
object LocaleManager {

    fun apply(language: Language) {
        val requested = LocaleListCompat.forLanguageTags(language.localeTag)
        val current = AppCompatDelegate.getApplicationLocales()
        if (current == requested) return
        AppCompatDelegate.setApplicationLocales(requested)
    }
}
