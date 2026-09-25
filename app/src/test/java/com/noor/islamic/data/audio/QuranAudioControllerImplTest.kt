package com.noor.islamic.data.audio

import com.noor.islamic.domain.audio.QuranAudioQueue
import com.noor.islamic.domain.model.quran.AyahAudioPosition
import com.noor.islamic.domain.model.quran.AyahRef
import com.noor.islamic.domain.model.quran.QuranAudioError
import com.noor.islamic.domain.model.quran.RecitationStyle
import com.noor.islamic.domain.model.quran.Reciter
import com.noor.islamic.domain.model.quran.ReciterNameKey
import com.noor.islamic.domain.repository.QuranAudioRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The playback state machine is exercised against a fake engine, so the queue, repeat,
 * resume and failure policy are all covered without Media3 or a device.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuranAudioControllerImplTest {

    private val alafasy = Reciter("Alafasy_128kbps", ReciterNameKey.ALAFASY, RecitationStyle.MURATTAL)
    private val husary = Reciter("Husary_128kbps", ReciterNameKey.HUSARY, RecitationStyle.MURATTAL)

    private val surahAyahs = listOf(
        AyahRef(surahNumber = 112, ayahNumberInSurah = 1, globalAyahNumber = 6_231),
        AyahRef(surahNumber = 112, ayahNumberInSurah = 2, globalAyahNumber = 6_232),
        AyahRef(surahNumber = 112, ayahNumberInSurah = 3, globalAyahNumber = 6_233),
    )

    @Test
    fun initialState_usesTheDefaultReciterWithNothingQueued() = runTest {
        val fixture = fixture()

        val state = fixture.controller.state.value
        assertEquals(alafasy, state.reciter)
        assertNull(state.currentAyah)
        assertFalse(state.isPlaying)
        assertFalse(state.isActive)
    }

    @Test
    fun play_buildsTheQueueAndExposesTheCurrentAyah() = runTest {
        val fixture = fixture()

        fixture.controller.play(QuranAudioQueue(alafasy, surahAyahs, startIndex = 1))

        val state = fixture.controller.state.value
        assertEquals(6_232, state.currentAyah?.globalAyahNumber)
        assertTrue(state.isPlaying)
        assertEquals(3, fixture.engine.items.size)
        assertEquals(1, fixture.engine.currentIndex)
    }

    @Test
    fun invalidQueue_isIgnored() = runTest {
        val fixture = fixture()

        fixture.controller.play(QuranAudioQueue(alafasy, emptyList(), startIndex = 0))

        assertNull(fixture.controller.state.value.currentAyah)
        assertTrue(fixture.engine.items.isEmpty())
    }

    @Test
    fun buffering_isSurfacedToTheUi() = runTest {
        val fixture = fixture()
        fixture.controller.play(QuranAudioQueue(alafasy, surahAyahs, startIndex = 0))

        fixture.engine.setBuffering(true)
        fixture.engine.notifyChanged()

        assertTrue(fixture.controller.state.value.isBuffering)
    }

    @Test
    fun networkFailure_isReportedAndStaysNonFatal() = runTest {
        val fixture = fixture()
        fixture.controller.play(QuranAudioQueue(alafasy, surahAyahs, startIndex = 0))

        fixture.engine.setFailure(AudioEngineFailure.NetworkUnavailable)
        fixture.engine.notifyChanged()

        val state = fixture.controller.state.value
        assertEquals(QuranAudioError.NETWORK, state.error)
        assertFalse(state.isPlaying)
        // The ayah stays selected so the reader can offer a retry.
        assertEquals(6_231, state.currentAyah?.globalAyahNumber)
    }

    @Test
    fun sourceUnavailable_isMappedToItsOwnError() = runTest {
        val fixture = fixture()
        fixture.controller.play(QuranAudioQueue(alafasy, surahAyahs, startIndex = 0))

        fixture.engine.setFailure(AudioEngineFailure.SourceUnavailable)
        fixture.engine.notifyChanged()

        assertEquals(
            QuranAudioError.SOURCE_UNAVAILABLE,
            fixture.controller.state.value.error,
        )
    }

    @Test
    fun skipToNext_movesForwardAndStopsAtTheEndWithoutRepeat() = runTest {
        val fixture = fixture()
        fixture.controller.play(QuranAudioQueue(alafasy, surahAyahs, startIndex = 0))

        fixture.controller.skipToNext()
        assertEquals(6_232, fixture.controller.state.value.currentAyah?.globalAyahNumber)

        fixture.controller.skipToNext()
        assertEquals(6_233, fixture.controller.state.value.currentAyah?.globalAyahNumber)

        fixture.controller.skipToNext()

        assertNull(fixture.controller.state.value.currentAyah)
        assertTrue(fixture.engine.wasStopped)
    }

    @Test
    fun repeat_wrapsFromTheLastAyahBackToTheFirst() = runTest {
        val fixture = fixture()
        fixture.controller.play(QuranAudioQueue(alafasy, surahAyahs, startIndex = 2))
        fixture.controller.setRepeatCurrentSurah(true)
        runCurrent()

        fixture.controller.skipToNext()

        assertEquals(6_231, fixture.controller.state.value.currentAyah?.globalAyahNumber)
        assertTrue(fixture.controller.state.value.repeatCurrentSurah)
    }

    @Test
    fun repeatFlag_isPersistedToTheDevice() = runTest {
        val fixture = fixture()

        fixture.controller.setRepeatCurrentSurah(true)
        runCurrent()

        assertTrue(fixture.repository.repeatValue.value)
    }

    @Test
    fun skipToPrevious_restartsTheCurrentAyahWhenFarEnoughIn() = runTest {
        val fixture = fixture()
        fixture.controller.play(QuranAudioQueue(alafasy, surahAyahs, startIndex = 2))
        fixture.engine.position = 4_000L

        fixture.controller.skipToPrevious()
        assertEquals(2, fixture.engine.currentIndex)

        fixture.engine.position = 0L
        fixture.controller.skipToPrevious()
        assertEquals(1, fixture.engine.currentIndex)
    }

    @Test
    fun pause_storesAResumePointForTheCurrentAyah() = runTest {
        val fixture = fixture()
        fixture.controller.play(QuranAudioQueue(alafasy, surahAyahs, startIndex = 1))
        fixture.engine.position = 2_500L
        // The published position is refreshed by the polling tick, not by the fake.
        advanceTimeBy(TICK_MILLIS)
        runCurrent()

        fixture.controller.pause()
        runCurrent()

        val resume = fixture.controller.state.value.resumePoint
        assertEquals(6_232, resume?.ref?.globalAyahNumber)
        assertEquals(2_500L, resume?.positionMs)
        assertFalse(fixture.controller.state.value.isPlaying)
        assertEquals(resume, fixture.repository.savedPositions.value.lastOrNull())
    }

    @Test
    fun resume_isIgnoredWhenNothingIsLoaded() = runTest {
        val fixture = fixture()

        fixture.controller.resume()

        assertFalse(fixture.controller.state.value.isPlaying)
    }

    @Test
    fun storedResumePoint_isLoadedOnStartup() = runTest {
        val fixture = fixture(storedResume = AyahAudioPosition(surahAyahs[1], 1_500L))

        val resume = fixture.controller.state.value.resumePoint
        assertEquals(6_232, resume?.ref?.globalAyahNumber)
        assertEquals(1_500L, resume?.positionMs)
    }

    @Test
    fun stop_clearsPlaybackButKeepsReciterAndStoredPosition() = runTest {
        val fixture = fixture()
        fixture.controller.play(QuranAudioQueue(alafasy, surahAyahs, startIndex = 0))
        fixture.engine.position = 2_000L
        advanceTimeBy(TICK_MILLIS)
        runCurrent()

        fixture.controller.stop()

        val state = fixture.controller.state.value
        assertNull(state.currentAyah)
        assertFalse(state.isPlaying)
        assertEquals(0L, state.positionMs)
        assertEquals(alafasy, state.reciter)
        assertTrue(fixture.engine.wasStopped)
        assertEquals(2_000L, state.resumePoint?.positionMs)
    }

    @Test
    fun selectReciter_requeriesTheSameSurahAtTheSamePosition() = runTest {
        val fixture = fixture()
        fixture.controller.play(QuranAudioQueue(alafasy, surahAyahs, startIndex = 1))
        fixture.engine.position = 900L
        advanceTimeBy(TICK_MILLIS)
        runCurrent()

        fixture.controller.selectReciter(husary)
        runCurrent()

        val state = fixture.controller.state.value
        assertEquals("Husary_128kbps", state.reciter.id)
        assertEquals(1, fixture.engine.currentIndex)
        assertEquals(900L, fixture.engine.positionMs)
        assertEquals("Husary_128kbps", fixture.repository.selectedValue.value.id)
        assertTrue(fixture.engine.items.all { it.uri.contains("Husary_128kbps") })
    }

    @Test
    fun progressTicks_arePublishedWhilePlaying() = runTest {
        val fixture = fixture()
        fixture.controller.play(QuranAudioQueue(alafasy, surahAyahs, startIndex = 0))
        fixture.engine.position = 500L

        advanceTimeBy(500)
        runCurrent()
        assertEquals(500L, fixture.controller.state.value.positionMs)

        fixture.engine.position = 1_000L
        advanceTimeBy(500)
        runCurrent()
        assertEquals(1_000L, fixture.controller.state.value.positionMs)
    }

    @Test
    fun positionIsNotPolledWhilePaused() = runTest {
        val fixture = fixture()
        fixture.controller.play(QuranAudioQueue(alafasy, surahAyahs, startIndex = 0))
        fixture.controller.pause()
        fixture.engine.position = 9_000L

        advanceTimeBy(2_000)
        runCurrent()

        assertEquals(0L, fixture.controller.state.value.positionMs)
    }

    @Test
    fun everyAyahUrls_followTheDocumentedPerAyahPattern() {
        val ref = AyahRef(surahNumber = 2, ayahNumberInSurah = 255, globalAyahNumber = 297)

        assertEquals(
            "https://everyayah.com/data/Alafasy_128kbps/002255.mp3",
            EveryAyahReciterCatalog.ayahAudioUrl(alafasy, 2, 255),
        )
        assertEquals("2:255", EveryAyahReciterCatalog.mediaIdOf(ref))
        assertEquals(alafasy, EveryAyahReciterCatalog.byId("Alafasy_128kbps"))
        assertNull(EveryAyahReciterCatalog.byId("not-a-reciter"))
    }

    @Test
    fun everyAyahReciterFolders_areUniqueAndResolvable() {
        val ids = EveryAyahReciterCatalog.reciters.map { it.id }

        assertEquals(ids.size, ids.distinct().size)
        EveryAyahReciterCatalog.reciters.forEach { reciter ->
            assertEquals(reciter, EveryAyahReciterCatalog.byId(reciter.id))
        }
    }

    // --- fixtures -------------------------------------------------------------------------------

    private class Fixture(
        val engine: FakeAudioEngine,
        val repository: FakeQuranAudioRepository,
        val controller: QuranAudioControllerImpl,
    )

    private fun TestScope.fixture(storedResume: AyahAudioPosition? = null): Fixture {
        val engine = FakeAudioEngine()
        val repository = FakeQuranAudioRepository(listOf(alafasy, husary), storedResume)
        val controller = QuranAudioControllerImpl(
            repository = repository,
            engine = engine,
            scope = backgroundScope,
            tickMillis = TICK_MILLIS,
            persistIntervalMillis = PERSIST_INTERVAL_MILLIS,
        )
        // Let the initial collectors deliver the stored reciter/resume values.
        runCurrent()
        return Fixture(engine, repository, controller)
    }

    private companion object {
        const val TICK_MILLIS = 500L
        const val PERSIST_INTERVAL_MILLIS = 5_000L
    }
}

private class FakeQuranAudioRepository(
    override val reciters: List<Reciter>,
    storedResume: AyahAudioPosition?,
) : QuranAudioRepository {

    val selectedValue = MutableStateFlow(reciters.first())
    val repeatValue = MutableStateFlow(false)
    val savedPositions = MutableStateFlow<List<AyahAudioPosition>>(emptyList())

    private val resumeValue = MutableStateFlow(storedResume)

    override val selectedReciter: Flow<Reciter> = selectedValue.asStateFlow()

    override val repeatCurrentSurah: Flow<Boolean> = repeatValue.asStateFlow()

    override val resumePosition: Flow<AyahAudioPosition?> = resumeValue.asStateFlow()

    override suspend fun selectReciter(reciter: Reciter) {
        selectedValue.value = reciter
    }

    override suspend fun setRepeatCurrentSurah(enabled: Boolean) {
        repeatValue.value = enabled
    }

    override suspend fun saveResumePosition(position: AyahAudioPosition) {
        savedPositions.value = savedPositions.value + position
        resumeValue.value = position
    }

    override suspend fun clearResumePosition() {
        resumeValue.value = null
    }
}

/** Minimal in-memory player that records what the controller asked for. */
private class FakeAudioEngine : AudioEngine {

    var items: List<AudioEngineItem> = emptyList()
        private set
    override var currentIndex: Int = 0
        private set
    var position: Long = 0L
    var wasStopped: Boolean = false
        private set

    private var playing: Boolean = false
    private var buffering: Boolean = false
    private var currentFailure: AudioEngineFailure? = null
    private var listener: AudioEngineListener? = null

    override val state: AudioEngineState
        get() = when {
            currentFailure != null || buffering -> AudioEngineState.BUFFERING
            playing -> AudioEngineState.READY
            else -> AudioEngineState.IDLE
        }

    override val isPlaying: Boolean get() = playing
    override val positionMs: Long get() = position
    override val durationMs: Long = 7_000L
    override val failure: AudioEngineFailure? get() = currentFailure

    fun setBuffering(value: Boolean) {
        buffering = value
    }

    fun setFailure(value: AudioEngineFailure) {
        currentFailure = value
        playing = false
    }

    fun notifyChanged() = listener?.onEngineChanged()

    override fun setQueue(items: List<AudioEngineItem>) {
        this.items = items
        currentIndex = 0
        wasStopped = false
    }

    override fun prepare() = Unit

    override fun play() {
        playing = true
    }

    override fun pause() {
        playing = false
    }

    override fun stopAndClear() {
        playing = false
        wasStopped = true
        position = 0L
        currentFailure = null
        buffering = false
    }

    override fun seekTo(index: Int, positionMs: Long) {
        currentIndex = index
        position = positionMs
        notifyChanged()
    }

    override fun setListener(listener: AudioEngineListener?) {
        this.listener = listener
    }

    override fun release() {
        listener = null
    }
}
