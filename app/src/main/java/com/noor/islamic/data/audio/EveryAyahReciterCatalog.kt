package com.noor.islamic.data.audio

import com.noor.islamic.domain.model.quran.AyahRef
import com.noor.islamic.domain.model.quran.Reciter
import com.noor.islamic.domain.model.quran.RecitationStyle
import com.noor.islamic.domain.model.quran.ReciterNameKey
import java.util.Locale

/**
 * Read-only catalog of the recitations Noor offers. The folder ids are the ones
 * published by the audio source, so reciter selection is a pure lookup and no URL
 * is ever constructed from user input.
 */
interface ReciterCatalog {
    val reciters: List<Reciter>
    val defaultReciter: Reciter
    fun byId(id: String?): Reciter?

    /** Playable item for one ayah; the only place recitations are turned into media. */
    fun itemFor(reciter: Reciter, ref: AyahRef): AudioEngineItem
}

/**
 * Per-ayah MP3 recitations streamed from EveryAyah, the public "Verse By Verse Quran
 * MP3" project (`https://everyayah.com/data/<reciter folder>/<sss><aaa>.mp3`).
 *
 * Audio is streamed on demand and is never downloaded, so it is explicitly *not*
 * offline audio; the UI states this. The reciter ids below are the exact folder
 * names published in the source's own `recitations.js` index.
 */
object EveryAyahReciterCatalog : ReciterCatalog {

    const val AUDIO_HOST = "https://everyayah.com/data"

    override val reciters: List<Reciter> = listOf(
        Reciter("Alafasy_128kbps", ReciterNameKey.ALAFASY, RecitationStyle.MURATTAL),
        Reciter("Abdul_Basit_Murattal_192kbps", ReciterNameKey.ABDUL_BASIT, RecitationStyle.MURATTAL),
        Reciter("Abdul_Basit_Mujawwad_128kbps", ReciterNameKey.ABDUL_BASIT, RecitationStyle.MUJAWWAD),
        Reciter("Husary_128kbps", ReciterNameKey.HUSARY, RecitationStyle.MURATTAL),
        Reciter("Minshawy_Murattal_128kbps", ReciterNameKey.MINSHAWY, RecitationStyle.MURATTAL),
        Reciter("Abdurrahmaan_As-Sudais_192kbps", ReciterNameKey.AS_SUDAIS, RecitationStyle.MURATTAL),
        Reciter("Muhammad_Ayyoub_128kbps", ReciterNameKey.AYYOUB, RecitationStyle.MURATTAL),
        Reciter("Yasser_Ad-Dussary_128kbps", ReciterNameKey.AD_DUSSARY, RecitationStyle.MURATTAL),
    )

    override val defaultReciter: Reciter = reciters.first()

    override fun byId(id: String?): Reciter? = reciters.firstOrNull { it.id == id }

    /** Per-ayah stream URL, e.g. `002255.mp3` for Al-Baqarah 255. */
    fun ayahAudioUrl(reciter: Reciter, surahNumber: Int, ayahNumberInSurah: Int): String =
        "$AUDIO_HOST/${reciter.id}/${segment(surahNumber)}${segment(ayahNumberInSurah)}.mp3"

    override fun itemFor(reciter: Reciter, ref: AyahRef): AudioEngineItem = AudioEngineItem(
        mediaId = mediaIdOf(ref),
        uri = ayahAudioUrl(reciter, ref.surahNumber, ref.ayahNumberInSurah),
    )

    fun mediaIdOf(ref: AyahRef): String = "${ref.surahNumber}:${ref.ayahNumberInSurah}"

    /** Locale-independent zero padding so Arabic locales cannot emit non-ASCII digits. */
    private fun segment(number: Int): String = String.format(Locale.ROOT, "%03d", number)
}
