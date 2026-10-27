package dz.techvision.noor.data.local.quran

import dz.techvision.noor.data.local.db.quran.QuranDao
import dz.techvision.noor.data.local.db.quran.QuranEntityMapper
import dz.techvision.noor.domain.model.quran.Ayah
import dz.techvision.noor.domain.model.quran.Surah
import dz.techvision.noor.domain.repository.QuranRepository
import com.noor.quran.common.ArabicSearchNormalizer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Backs [QuranRepository] with Room reads from the prebuilt asset. All reads run
 * on [ioDispatcher]; search rebuilds FTS4 query terms from the shared normalizer
 * so queries match exactly the rules used when the database was generated.
 */
class QuranRepositoryImpl(
    private val dao: QuranDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : QuranRepository {

    override fun observeSurahs(): Flow<List<Surah>> =
        dao.observeSurahs().map { list -> list.map(QuranEntityMapper::toDomain) }

    override suspend fun getSurah(surahNumber: Int): Surah? = withContext(ioDispatcher) {
        dao.getSurah(surahNumber)?.let(QuranEntityMapper::toDomain)
    }

    override suspend fun getAyahs(surahNumber: Int): List<Ayah> = withContext(ioDispatcher) {
        dao.getAyahsOfSurah(surahNumber).map(QuranEntityMapper::toDomain)
    }

    override suspend fun getAyah(surahNumber: Int, ayahNumberInSurah: Int): Ayah? =
        withContext(ioDispatcher) {
            dao.getAyah(surahNumber, ayahNumberInSurah)?.let(QuranEntityMapper::toDomain)
        }

    override suspend fun getAyahRange(firstGlobal: Int, lastGlobal: Int): List<Ayah> =
        withContext(ioDispatcher) {
            dao.getAyahRange(firstGlobal, lastGlobal).map(QuranEntityMapper::toDomain)
        }

    override suspend fun search(query: String, limit: Int): List<Ayah> = withContext(ioDispatcher) {
        val ftsQuery = buildFtsQuery(query) ?: return@withContext emptyList()
        dao.search(ftsQuery, limit).map(QuranEntityMapper::toDomain)
    }

    /** Turns a plain-text query into safe FTS4 prefix terms, or null when there is nothing to match. */
    private fun buildFtsQuery(query: String): String? {
        val normalized = ArabicSearchNormalizer.normalize(query.trim())
        val tokens = normalized
            .split(Regex("\\s+"))
            .mapNotNull { token -> sanitizeToken(token) }
            .filter { it.isNotEmpty() && !it.isFtsOperator() }
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" AND ") { "$it*" }
    }

    /** FTS4 reserved operators cannot appear as bare query terms. */
    private fun String.isFtsOperator(): Boolean =
        equals("AND", ignoreCase = true) || equals("OR", ignoreCase = true) ||
            equals("NOT", ignoreCase = true) || equals("NEAR", ignoreCase = true)

    private fun sanitizeToken(token: String): String {
        val out = StringBuilder(token.length)
        var i = 0
        while (i < token.length) {
            val cp = token.codePointAt(i)
            i += Character.charCount(cp)
            val isArabic = cp in 0x0600..0x06FF || cp in 0x0750..0x077F
            val isLatin = cp in 'a'.code..'z'.code || cp in 'A'.code..'Z'.code
            val isDigit = cp in '0'.code..'9'.code
            if (isArabic || isLatin || isDigit) out.appendCodePoint(cp)
        }
        return out.toString()
    }
}
