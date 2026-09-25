package com.noor.islamic.services

import android.content.Context
import com.noor.islamic.data.local.datastore.SettingsDataStore
import com.noor.islamic.data.local.db.NoorDatabase
import com.noor.islamic.data.local.db.quran.QuranReaderDatabase
import com.noor.islamic.data.local.location.AndroidLocationRepository
import com.noor.islamic.data.local.quran.QuranRepositoryImpl
import com.noor.islamic.data.local.sensor.HeadingSensor
import com.noor.islamic.data.remote.prayer.PrayerApiProvider
import com.noor.islamic.data.remote.prayer.PrayerTimesMapper
import com.noor.islamic.data.repository.PrayerRepositoryImpl
import com.noor.islamic.domain.repository.LocationRepository
import com.noor.islamic.domain.repository.PrayerRepository
import com.noor.islamic.domain.repository.QuranRepository
import com.noor.islamic.domain.repository.SettingsRepository
import com.noor.islamic.domain.usecase.ObserveSettingsUseCase

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

    private val noorDatabase: NoorDatabase by lazy {
        NoorDatabase.create(appContext)
    }

    val locationRepository: LocationRepository by lazy {
        AndroidLocationRepository(appContext)
    }

    val prayerRepository: PrayerRepository by lazy {
        PrayerRepositoryImpl(
            api = PrayerApiProvider.provide(),
            dao = noorDatabase.prayerTimesDao(),
            mapper = PrayerTimesMapper(),
        )
    }

    private val quranReaderDatabase: QuranReaderDatabase by lazy {
        QuranReaderDatabase.create(appContext)
    }

    val quranRepository: QuranRepository by lazy {
        QuranRepositoryImpl(quranReaderDatabase.quranDao())
    }

    /** Lightweight; constructed per Qibla screen so sensors only run while it is open. */
    fun headingSensor(context: Context): HeadingSensor = HeadingSensor(context)
}