package dz.techvision.noor.domain.repository

import dz.techvision.noor.domain.model.quran.AyahAudioPosition
import dz.techvision.noor.domain.model.quran.Reciter
import kotlinx.coroutines.flow.Flow

/**
 * Reciter catalog plus the lightweight, device-local audio preferences: selected
 * reciter, repeat behaviour and the last known playback position.
 *
 * Recitations are streamed; nothing is downloaded, so no offline audio is claimed here.
 */
interface QuranAudioRepository {

    val reciters: List<Reciter>

    val selectedReciter: Flow<Reciter>

    val repeatCurrentSurah: Flow<Boolean>

    val resumePosition: Flow<AyahAudioPosition?>

    suspend fun selectReciter(reciter: Reciter)

    suspend fun setRepeatCurrentSurah(enabled: Boolean)

    suspend fun saveResumePosition(position: AyahAudioPosition)

    suspend fun clearResumePosition()
}

