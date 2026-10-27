package dz.techvision.noor.data.local.db.userstate

import dz.techvision.noor.domain.model.quran.AyahRef
import dz.techvision.noor.domain.model.quran.Bookmark
import dz.techvision.noor.domain.model.quran.ReadingProgress

/**
 * Pure mapping between the app-private user-state entities and the domain models.
 */
object QuranUserStateEntityMapper {

    fun bookmarkToDomain(entity: QuranBookmarkEntity): Bookmark = Bookmark(
        ref = AyahRef(
            surahNumber = entity.surahNumber,
            ayahNumberInSurah = entity.ayahNumberInSurah,
            globalAyahNumber = entity.globalAyahNumber,
        ),
        createdAt = entity.createdAt,
    )

    fun bookmarkToEntity(ref: AyahRef, createdAt: Long): QuranBookmarkEntity = QuranBookmarkEntity(
        globalAyahNumber = ref.globalAyahNumber,
        surahNumber = ref.surahNumber,
        ayahNumberInSurah = ref.ayahNumberInSurah,
        createdAt = createdAt,
    )

    fun progressToDomain(entity: QuranReadingProgressEntity): ReadingProgress = ReadingProgress(
        ref = AyahRef(
            surahNumber = entity.surahNumber,
            ayahNumberInSurah = entity.ayahNumberInSurah,
            globalAyahNumber = entity.globalAyahNumber,
        ),
        updatedAt = entity.updatedAt,
    )

    fun progressToEntity(ref: AyahRef, updatedAt: Long): QuranReadingProgressEntity =
        QuranReadingProgressEntity(
            id = QuranReadingProgressEntity.SINGLETON_ID,
            surahNumber = ref.surahNumber,
            ayahNumberInSurah = ref.ayahNumberInSurah,
            globalAyahNumber = ref.globalAyahNumber,
            updatedAt = updatedAt,
        )
}

