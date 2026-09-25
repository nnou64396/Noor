package com.noor.islamic.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.noor.islamic.data.audio.EveryAyahReciterCatalog
import com.noor.islamic.data.audio.ReciterCatalog
import com.noor.islamic.domain.model.quran.AyahAudioPosition
import com.noor.islamic.domain.model.quran.AyahRef
import com.noor.islamic.domain.model.quran.Reciter
import com.noor.islamic.domain.repository.QuranAudioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.noorQuranAudioDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "noor_quran_audio"
)

/**
 * DataStore-backed audio preferences: selected reciter, repeat behaviour and the last
 * known playback position. Only lightweight, resumable state is stored — no audio
 * bytes, and nothing leaves the device.
 */
class QuranAudioPreferences internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val catalog: ReciterCatalog,
) : QuranAudioRepository {

    override val reciters: List<Reciter> get() = catalog.reciters

    override val selectedReciter: Flow<Reciter> =
        dataStore.data.map { prefs ->
            catalog.byId(prefs[Keys.RECITER_ID]) ?: catalog.defaultReciter
        }

    override val repeatCurrentSurah: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.REPEAT_SURAH] ?: false }

    override val resumePosition: Flow<AyahAudioPosition?> =
        dataStore.data.map { prefs ->
            val surah = prefs[Keys.LAST_SURAH]
            val ayah = prefs[Keys.LAST_AYAH]
            val global = prefs[Keys.LAST_GLOBAL]
            val position = prefs[Keys.LAST_POSITION_MS]
            if (surah == null || ayah == null || global == null || position == null) {
                null
            } else {
                AyahAudioPosition(
                    ref = AyahRef(
                        surahNumber = surah,
                        ayahNumberInSurah = ayah,
                        globalAyahNumber = global,
                    ),
                    positionMs = position,
                )
            }
        }

    override suspend fun selectReciter(reciter: Reciter) {
        dataStore.edit { prefs -> prefs[Keys.RECITER_ID] = reciter.id }
    }

    override suspend fun setRepeatCurrentSurah(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.REPEAT_SURAH] = enabled }
    }

    override suspend fun saveResumePosition(position: AyahAudioPosition) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_SURAH] = position.ref.surahNumber
            prefs[Keys.LAST_AYAH] = position.ref.ayahNumberInSurah
            prefs[Keys.LAST_GLOBAL] = position.ref.globalAyahNumber
            prefs[Keys.LAST_POSITION_MS] = position.positionMs
        }
    }

    override suspend fun clearResumePosition() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.LAST_SURAH)
            prefs.remove(Keys.LAST_AYAH)
            prefs.remove(Keys.LAST_GLOBAL)
            prefs.remove(Keys.LAST_POSITION_MS)
        }
    }

    companion object {
        fun create(context: Context): QuranAudioPreferences =
            QuranAudioPreferences(context.noorQuranAudioDataStore, EveryAyahReciterCatalog)
    }

    private object Keys {
        val RECITER_ID = stringPreferencesKey("reciter_id")
        val REPEAT_SURAH = booleanPreferencesKey("repeat_current_surah")
        val LAST_SURAH = intPreferencesKey("last_played_surah")
        val LAST_AYAH = intPreferencesKey("last_played_ayah")
        val LAST_GLOBAL = intPreferencesKey("last_played_global")
        val LAST_POSITION_MS = longPreferencesKey("last_played_position_ms")
    }
}
