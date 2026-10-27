package dz.techvision.noor.utils

import dz.techvision.noor.domain.model.AppSettings
import dz.techvision.noor.services.ServiceLocator
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/**
 * Restores the persisted language and theme before the first frame.
 *
 * `SettingsDataStore` remains the single source of truth; this only reads it
 * once at process start and hands the values to the AppCompat delegates, which
 * is what actually makes the XML theme and the resource configuration match the
 * stored choice. Without this, a process restart would render with the system
 * locale and system night mode and only visibly switch later, once
 * [dz.techvision.noor.presentation.MainViewModel] observed the stored value.
 *
 * Called from `Application.onCreate`, so it runs before any Activity exists and
 * therefore cannot flash the wrong theme.
 *
 * The read stays synchronous on purpose. Deferring it would let the first
 * Activity render with the system locale and night mode and then visibly switch,
 * which is the exact failure this class exists to prevent. What is bounded
 * instead is how long the main thread is allowed to wait for it, so a slow or
 * wedged preferences file cannot stall process start indefinitely.
 */
object SettingsBootstrapper {

    /**
     * Upper bound on the blocking startup read.
     *
     * A healthy read is a single small preferences file and completes in
     * single-digit milliseconds, so this is several orders of magnitude of
     * headroom. It exists only to bound the worst case, and it stays well below
     * the ANR threshold: exceeding it degrades to the first-run defaults rather
     * than risking a frozen cold start.
     */
    internal const val RESTORE_TIMEOUT_MILLIS = 2_000L

    /**
     * Blocking by design: the stored values must be applied before the first
     * Activity is created.
     *
     * Exactly one [AppSettings] value is resolved and both delegates are then
     * applied from it, so the language and the theme can never be left half
     * restored relative to each other.
     */
    fun restorePersistedSettings() {
        val settings = readPersistedSettings()
        LocaleManager.apply(settings.language)
        ThemeManager.apply(settings.themeMode)
    }

    /**
     * The stored settings exactly as a fresh process start would observe them.
     * Exposed separately so the restoration contract can be tested.
     */
    fun readPersistedSettings(): AppSettings = readBoundedSettings {
        ServiceLocator.observeSettings.invoke().first()
    }

    /**
     * Reads the settings within [timeoutMillis], falling back to the documented
     * first-run defaults if the store does not answer in time or cannot be read.
     *
     * Both failure paths converge on the same [AppSettings] default, which is
     * what makes the outcome deterministic: the app starts on the system theme
     * and the default language instead of crashing during `onCreate` or
     * blocking cold start forever. Because [restorePersistedSettings] still
     * applies both delegates from whatever this returns, a failed read leaves a
     * complete, self-consistent state rather than a partially initialised one.
     *
     * The only visible consequence of a failed read is that `onboardingComplete`
     * falls back to `false`, so onboarding would be shown again. That is
     * accepted deliberately: re-showing onboarding is recoverable, whereas a
     * crashed or frozen cold start is not.
     */
    internal fun readBoundedSettings(
        timeoutMillis: Long = RESTORE_TIMEOUT_MILLIS,
        read: suspend () -> AppSettings,
    ): AppSettings =
        try {
            runBlocking { withTimeout(timeoutMillis) { read() } }
        } catch (_: TimeoutCancellationException) {
            AppSettings()
        } catch (_: Exception) {
            AppSettings()
        }
}

