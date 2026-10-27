package dz.techvision.noor.data.local.db.quran

import dz.techvision.noor.domain.model.quran.Ayah
import dz.techvision.noor.domain.model.quran.RevelationType
import dz.techvision.noor.domain.model.quran.Surah

/**
 * Pure mappers between the Room entities and the domain models; unit-testable.
 */
object QuranEntityMapper {

    fun toDomain(entity: SurahEntity): Surah = Surah(
        surahNumber = entity.surahNumber,
        nameArabic = entity.nameArabic,
        nameTransliteration = entity.nameTransliteration,
        nameEnglish = entity.nameEnglish,
        ayahCount = entity.ayahCount,
        revelationOrder = entity.revelationOrder,
        revelationType = RevelationType.valueOf(entity.revelationType),
        startPage = entity.startPage,
        basmala = entity.basmala,
    )

    fun toDomain(entity: AyahEntity): Ayah = Ayah(
        globalAyahNumber = entity.globalAyahNumber,
        surahNumber = entity.surahNumber,
        ayahNumberInSurah = entity.ayahNumberInSurah,
        juzNumber = entity.juzNumber,
        pageNumber = entity.pageNumber,
        hasSajdah = entity.hasSajdah,
        textUthmani = entity.textUthmani,
    )
}
