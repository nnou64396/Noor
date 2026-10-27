package dz.techvision.noor.domain.repository

import dz.techvision.noor.domain.model.quran.Ayah
import dz.techvision.noor.domain.model.quran.Surah
import kotlinx.coroutines.flow.Flow

/**
 * Read-only, offline-first source for the bundled Quran database
 * (`assets/quran/quran_reader.db`). No runtime network is ever used.
 */
interface QuranRepository {

    fun observeSurahs(): Flow<List<Surah>>

    suspend fun getSurah(surahNumber: Int): Surah?

    suspend fun getAyahs(surahNumber: Int): List<Ayah>

    suspend fun getAyah(surahNumber: Int, ayahNumberInSurah: Int): Ayah?

    suspend fun getAyahRange(firstGlobal: Int, lastGlobal: Int): List<Ayah>

    /** Plain-text Arabic query; normalized against the same rules used to build the index. */
    suspend fun search(query: String, limit: Int = 50): List<Ayah>
}
