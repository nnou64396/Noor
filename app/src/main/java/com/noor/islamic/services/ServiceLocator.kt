package com.noor.islamic.services

import android.content.Context
import com.noor.islamic.data.audio.EveryAyahReciterCatalog
import com.noor.islamic.data.audio.Media3AudioEngine
import com.noor.islamic.data.audio.QuranAudioControllerImpl
import com.noor.islamic.data.local.athkar.AssetAthkarCorpusSource
import com.noor.islamic.data.local.datastore.QuranAudioPreferences
import com.noor.islamic.data.local.datastore.SettingsDataStore
import com.noor.islamic.data.local.db.NoorDatabase
import com.noor.islamic.data.local.db.practice.PracticeStateDatabase
import com.noor.islamic.data.local.db.quran.QuranReaderDatabase
import com.noor.islamic.data.local.db.userstate.QuranUserStateDatabase
import com.noor.islamic.data.local.location.AndroidLocationRepository
import com.noor.islamic.data.local.quran.QuranRepositoryImpl
import com.noor.islamic.data.local.sensor.HeadingSensor
import com.noor.islamic.data.local.userdata.AthkarPracticeRepository
import com.noor.islamic.data.local.userdata.QuranUserStateRepository
import com.noor.islamic.data.local.userdata.TasbihPracticeRepository
import com.noor.islamic.data.remote.network.NetworkModule
import com.noor.islamic.data.remote.prayer.PrayerApiProvider
import com.noor.islamic.data.remote.prayer.PrayerTimesMapper
import com.noor.islamic.data.repository.PrayerRepositoryImpl
import com.noor.islamic.data.remote.update.UpdateApi
import com.noor.islamic.data.repository.UpdateRepositoryImpl
import com.noor.islamic.data.update.ApkDownloader
import com.noor.islamic.domain.audio.QuranAudioController
import com.noor.islamic.domain.repository.AthkarProgressRepository
import com.noor.islamic.domain.repository.DhikrRepository
import com.noor.islamic.domain.repository.LocationRepository
import com.noor.islamic.domain.repository.PrayerRepository
import com.noor.islamic.domain.repository.QuranAudioRepository
import com.noor.islamic.domain.repository.QuranBookmarksRepository
import com.noor.islamic.domain.repository.QuranRepository
import com.noor.islamic.domain.repository.ReadingProgressRepository
import com.noor.islamic.domain.repository.SettingsRepository
import com.noor.islamic.domain.repository.TasbihRepository
import com.noor.islamic.domain.repository.UpdateRepository
import com.noor.islamic.domain.usecase.ObserveSettingsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient

/**
 * Minimal manual dependency container. Keeps Phase 0 free of a DI
 * framework while allowing easy swaps once Hilt or another DI approach
 * is introduced.
 */
object ServiceLocator {

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsDataStore.create(appContext)
    }

    val observeSettings: ObserveSettingsUseCase by lazy {
        ObserveSettingsUseCase(settingsRepository)
    }

    private val updateApi: UpdateApi? by lazy {
        runCatching {
            NetworkModule.provideRetrofit(
                baseUrl = "https://api.github.com/",
                client = provideOkHttpClient(),
            ).create(UpdateApi::class.java)
        }.getOrNull()
    }

    private val apkDownloader: ApkDownloader by lazy {
        ApkDownloader(appContext, provideOkHttpClient())
    }

    val updateRepository: UpdateRepository by lazy {
        val versionCode = runCatching {
            val pInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        }.getOrDefault(1L)

        UpdateRepositoryImpl(
            context = appContext,
            currentVersionCode = versionCode,
            settingsRepository = settingsRepository,
            apkDownloader = apkDownloader,
            apiProvider = { updateApi },
        )
    }

    private val noorDatabase: NoorDatabase by lazy {
        NoorDatabase.create(appContext)
    }

    val locationRepository: LocationRepository by lazy {
        AndroidLocationRepository(appContext)
    }

    val prayerRepository: PrayerRepository by lazy {
        PrayerRepositoryImpl(
            api = PrayerApiProvider.provide(provideOkHttpClient()),
            dao = noorDatabase.prayerTimesDao(),
            mapper = PrayerTimesMapper(),
        )
    }

    /**
     * HTTP logging is enabled only for a debuggable build, and is redacted even
     * then, so the request's `latitude`/`longitude` never reach Logcat in a
     * release APK.
     */
    private fun provideOkHttpClient(): OkHttpClient =
        NetworkModule.provideOkHttpClient(
            logging = NetworkModule.shouldLogHttp(appContext.applicationInfo.flags),
        )

    private val quranReaderDatabase: QuranReaderDatabase by lazy {
        QuranReaderDatabase.create(appContext)
    }

    val quranRepository: QuranRepository by lazy {
        QuranRepositoryImpl(quranReaderDatabase.quranDao())
    }

    private val quranUserStateDatabase: QuranUserStateDatabase by lazy {
        QuranUserStateDatabase.create(appContext)
    }

    /** One shared instance backs both bookmarks and reading progress (no duplicate handles). */
    private val quranUserStateRepository: QuranUserStateRepository by lazy {
        QuranUserStateRepository(
            bookmarkDao = quranUserStateDatabase.bookmarkDao(),
            readingProgressDao = quranUserStateDatabase.readingProgressDao(),
        )
    }

    val quranBookmarksRepository: QuranBookmarksRepository get() = quranUserStateRepository

    val readingProgressRepository: ReadingProgressRepository get() = quranUserStateRepository

    val quranAudioRepository: QuranAudioRepository by lazy {
        QuranAudioPreferences.create(appContext)
    }

    /**
     * The single app-scoped Media3 engine. The in-app controller and the media
     * session service share this instance, so system transport controls and
     * in-app playback drive the exact same player.
     */
    val quranAudioEngine: Media3AudioEngine by lazy {
        Media3AudioEngine(appContext)
    }

    /**
     * Application-scoped audio player. One controller serves every screen, so playback
     * survives navigation and configuration changes without a service or a second player.
     */
    val quranAudioController: QuranAudioController by lazy {
        QuranAudioControllerImpl(
            repository = quranAudioRepository,
            engine = quranAudioEngine,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
            catalog = EveryAyahReciterCatalog,
        )
    }

    private val practiceStateDatabase: PracticeStateDatabase by lazy {
        PracticeStateDatabase.create(appContext)
    }

    /**
     * Athkar content (read-only, bundled in the APK) and the user's repetition
     * counters (mutable, local). One instance serves both Athkar screens.
     */
    private val athkarPracticeRepository: AthkarPracticeRepository by lazy {
        AthkarPracticeRepository(
            corpusSource = AssetAthkarCorpusSource(appContext),
            progressDao = practiceStateDatabase.athkarProgressDao(),
        )
    }

    val dhikrRepository: DhikrRepository get() = athkarPracticeRepository

    val athkarProgressRepository: AthkarProgressRepository get() = athkarPracticeRepository

    val tasbihRepository: TasbihRepository by lazy {
        TasbihPracticeRepository(tasbihDao = practiceStateDatabase.tasbihDao())
    }

    /** Lightweight; constructed per Qibla screen so sensors only run while it is open. */
    fun headingSensor(context: Context): HeadingSensor = HeadingSensor(context)
}
