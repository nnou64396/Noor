package dz.techvision.noor.data.local.db.quran

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {

    @Query("SELECT * FROM surahs ORDER BY surah_number")
    fun observeSurahs(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs WHERE surah_number = :surahNumber")
    suspend fun getSurah(surahNumber: Int): SurahEntity?

    @Query("SELECT * FROM ayahs WHERE surah_number = :surahNumber ORDER BY ayah_number_in_surah")
    suspend fun getAyahsOfSurah(surahNumber: Int): List<AyahEntity>

    @Query("SELECT * FROM ayahs WHERE surah_number = :surahNumber AND ayah_number_in_surah = :ayahNumberInSurah")
    suspend fun getAyah(surahNumber: Int, ayahNumberInSurah: Int): AyahEntity?

    @Query("SELECT * FROM ayahs WHERE global_ayah_number BETWEEN :firstGlobal AND :lastGlobal ORDER BY global_ayah_number")
    suspend fun getAyahRange(firstGlobal: Int, lastGlobal: Int): List<AyahEntity>

    /** `query` is expected in FTS4 syntax (see QuranRepositoryImpl). */
    @Query(
        "SELECT * FROM ayahs WHERE global_ayah_number IN " +
            "(SELECT rowid FROM ayahs_fts WHERE ayahs_fts MATCH :query) " +
            "ORDER BY global_ayah_number LIMIT :limit",
    )
    suspend fun search(query: String, limit: Int): List<AyahEntity>
}
