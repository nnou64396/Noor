package dz.techvision.noor.data.local.db.userstate

import dz.techvision.noor.domain.model.quran.AyahRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Bookmark identity is derived from the canonical ayah numbers only. These tests pin
 * that contract, so a presentation or corpus change can never silently move a bookmark.
 */
class QuranUserStateEntityMapperTest {

    @Test
    fun bookmarkRoundTrip_preservesCanonicalIdentity() {
        val ref = AyahRef(surahNumber = 2, ayahNumberInSurah = 255, globalAyahNumber = 297)

        val entity = QuranUserStateEntityMapper.bookmarkToEntity(ref, createdAt = 1_700_000L)
        val restored = QuranUserStateEntityMapper.bookmarkToDomain(entity)

        assertEquals(ref, restored.ref)
        assertEquals(1_700_000L, restored.createdAt)
        assertEquals(297, entity.globalAyahNumber)
    }

    @Test
    fun progressToEntity_usesTheSingletonRowSoOnlyTheLatestPositionSurvives() {
        val first = AyahRef(surahNumber = 1, ayahNumberInSurah = 1, globalAyahNumber = 1)
        val second = AyahRef(surahNumber = 2, ayahNumberInSurah = 1, globalAyahNumber = 8)

        val firstRow = QuranUserStateEntityMapper.progressToEntity(first, updatedAt = 10L)
        val secondRow = QuranUserStateEntityMapper.progressToEntity(second, updatedAt = 20L)

        assertEquals(QuranReadingProgressEntity.SINGLETON_ID, firstRow.id)
        assertEquals(firstRow.id, secondRow.id)
        assertEquals(8, secondRow.globalAyahNumber)
        assertEquals(2, secondRow.surahNumber)
    }

    @Test
    fun progressRoundTrip_keepsAyahNumbersAndTimestamp() {
        val ref = AyahRef(surahNumber = 112, ayahNumberInSurah = 5, globalAyahNumber = 6_233)

        val entity = QuranUserStateEntityMapper.progressToEntity(ref, updatedAt = 99L)
        val restored = QuranUserStateEntityMapper.progressToDomain(entity)

        assertEquals(ref, restored.ref)
        assertEquals(99L, restored.updatedAt)
        assertNull(
            "global ayah number must never be 0 for a real ayah",
            ref.globalAyahNumber.takeIf { it == 0 },
        )
    }
}

