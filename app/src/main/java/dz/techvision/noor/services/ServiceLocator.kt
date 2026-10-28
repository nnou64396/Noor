package dz.techvision.noor.services

import android.content.Context
import dz.techvision.noor.data.audio.EveryAyahReciterCatalog
import dz.techvision.noor.data.audio.Media3AudioEngine
import dz.techvision.noor.data.audio.QuranAudioControllerImpl
import dz.techvision.noor.data.local.athkar.AssetAthkarCorpusSource
import dz.techvision.noor.data.local.datastore.QuranAudioPreferences
import dz.techvision.noor.data.local.datastore.SettingsDataStore
import dz.techvision.noor.data.local.db.NoorDatabase
import dz.techvision.noor.data.local.db.practice.PracticeStateDatabase
import dz.techvision.noor.data.local.db.quran.QuranReaderDatabase
import dz.techvision.noor.data.local.db.userstate.QuranUserStateDatabase
import dz.techvision.noor.data.local.location.AndroidLocationRepository
import dz.techvision.noor.data.local.quran.QuranRepositoryImpl
import dz.techvision.noor.data.local.sensor.HeadingSensor
import dz.techvision.noor.data.local.userdata.AthkarPracticeRepository
import dz.techvision.noor.data.local.userdata.QuranUserStateRepository
import dz.techvision.noor.data.local.userdata.TasbihPracticeRepository
import dz.techvision.noor.data.remote.network.NetworkModule
import dz.techvision.noor.data.remote.prayer.PrayerApiProvider
import dz.techvision.noor.data.remote.prayer.PrayerTimesMapper
import dz.techvision.noor.data.repository.PrayerRepositoryImpl
import dz.techvision.noor.data.remote.update.UpdateApi
import dz.techvision.noor.data.repository.UpdateRepositoryImpl
import dz.techvision.noor.data.update.ApkDownloader
import dz.techvision.noor.domain.audio.QuranAudioController
import dz.techvision.noor.domain.repository.AthkarProgressRepository
import dz.techvision.noor.domain.repository.DhikrRepository
import dz.techvision.noor.domain.repository.LocationRepository
import dz.techvision.noor.domain.repository.PrayerRepository
import dz.techvision.noor.domain.repository.QuranAudioRepository
import dz.techvision.noor.domain.repository.QuranBookmarksRepository
import dz.techvision.noor.domain.repository.QuranRepository
import dz.techvision.noor.domain.repository.ReadingProgressRepository
import dz.techvision.noor.domain.repository.SettingsRepository
import dz.techvision.noor.domain.repository.TasbihRepository
import dz.techvision.noor.domain.repository.UpdateRepository
import dz.techvision.noor.domain.usecase.ObserveSettingsUseCase
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

    private var _appContext: Context? = null

    val appContext: Context
        get() = _appContext ?: throw IllegalStateException("ServiceLocator not initialized")

    val contextOrNull: Context?
        get() = _appContext

    fun init(context: Context) {
        _appContext = context.applicationContext
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

