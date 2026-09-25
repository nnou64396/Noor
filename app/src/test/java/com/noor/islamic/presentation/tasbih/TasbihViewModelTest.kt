package com.noor.islamic.presentation.tasbih

import com.noor.islamic.domain.model.tasbih.TasbihSession
import com.noor.islamic.domain.repository.TasbihRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The counter's transitions. Because the state is shared with
 * `SharingStarted.WhileSubscribed`, every test collects [TasbihViewModel.uiState]
 * so the upstream is actually active.
 */
class TasbihViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun increment_countsUpToTheTarget() = runTest(dispatcher) {
        val repository = FakeTasbihRepository()
        val viewModel = TasbihViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect { } }

        repeat(3) { viewModel.increment() }
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.count)
        assertEquals(3, repository.session.totalCount)
    }

    @Test
    fun increment_stopsAtTheTargetAndKeepsTheLifetimeTotal() = runTest(dispatcher) {
        val repository = FakeTasbihRepository()
        val viewModel = TasbihViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect { } }

        repeat(TasbihSession.DEFAULT_TARGET + 5) { viewModel.increment() }
        advanceUntilIdle()

        assertEquals(TasbihSession.DEFAULT_TARGET, viewModel.uiState.value.count)
        assertTrue(viewModel.uiState.value.isComplete)
        // Only the counts that actually advanced are remembered.
        assertEquals(TasbihSession.DEFAULT_TARGET, repository.session.totalCount)
    }

    @Test
    fun rapidTaps_areEachCounted() = runTest(dispatcher) {
        val repository = FakeTasbihRepository()
        val viewModel = TasbihViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect { } }

        // No suspension point between the taps: each must be applied to the result
        // of the previous one rather than to the same starting value.
        repeat(7) { viewModel.increment() }
        advanceUntilIdle()

        assertEquals(7, viewModel.uiState.value.count)
        assertEquals(7, repository.session.totalCount)
    }

    @Test
    fun decrement_undoesOneCountAndStopsAtZero() = runTest(dispatcher) {
        val repository = FakeTasbihRepository(TasbihSession(count = 2, target = 33, totalCount = 5))
        val viewModel = TasbihViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.decrement()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.count)

        viewModel.decrement()
        viewModel.decrement()
        viewModel.decrement()
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.count)
        // Undo corrects the round, it does not rewrite history.
        assertEquals(5, viewModel.uiState.value.session.totalCount)
        assertFalse(viewModel.uiState.value.canDecrement)
    }

    @Test
    fun reset_startsTheRoundAgainButKeepsTheLifetimeTotal() = runTest(dispatcher) {
        val repository = FakeTasbihRepository(TasbihSession(count = 20, target = 33, totalCount = 20))
        val viewModel = TasbihViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.reset()
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.count)
        assertEquals(20, viewModel.uiState.value.session.totalCount)
    }

    @Test
    fun setTarget_keepsProgressAlreadyMade() = runTest(dispatcher) {
        val repository = FakeTasbihRepository(TasbihSession(count = 20, target = 33))
        val viewModel = TasbihViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.setTarget(99)
        advanceUntilIdle()

        assertEquals(99, viewModel.uiState.value.target)
        assertEquals(20, viewModel.uiState.value.count)
    }

    @Test
    fun setTarget_clampsProgressWhenTheTargetIsLowered() = runTest(dispatcher) {
        val repository = FakeTasbihRepository(TasbihSession(count = 90, target = 99))
        val viewModel = TasbihViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.setTarget(33)
        advanceUntilIdle()

        assertEquals(33, viewModel.uiState.value.target)
        assertEquals(33, viewModel.uiState.value.count)
        assertTrue(viewModel.uiState.value.isComplete)
    }

    @Test
    fun setTarget_clampsAnOutOfRangeRequest() = runTest(dispatcher) {
        val repository = FakeTasbihRepository()
        val viewModel = TasbihViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.setTarget(0)
        advanceUntilIdle()
        assertEquals(TasbihSession.MIN_TARGET, viewModel.uiState.value.target)

        viewModel.setTarget(1_000_000)
        advanceUntilIdle()
        assertEquals(TasbihSession.MAX_TARGET, viewModel.uiState.value.target)
    }

    @Test
    fun presets_areOfferedAsTargets() = runTest(dispatcher) {
        val viewModel = TasbihViewModel(FakeTasbihRepository())
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertEquals(listOf(33, 99, 100), viewModel.uiState.value.presets)
    }

    @Test
    fun aTapBeforeTheStoredSessionLoads_isNotLost() = runTest(dispatcher) {
        // Nothing has been read from storage yet, as when the database query is
        // slower than the user's first tap.
        val repository = FakeTasbihRepository(initial = null)
        val viewModel = TasbihViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.increment()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.count)

        // The slow read now lands; it must not overwrite what the user just did.
        repository.emit(TasbihSession(count = 10, target = 33))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.count)
    }

    @Test
    fun aRestoredSessionIsSeededFromStorage() = runTest(dispatcher) {
        val repository = FakeTasbihRepository(TasbihSession(count = 12, target = 33, totalCount = 40))
        val viewModel = TasbihViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertEquals(12, viewModel.uiState.value.count)
        assertEquals(40, viewModel.uiState.value.session.totalCount)
    }

    @Test
    fun fraction_tracksProgressAndStaysInRange() = runTest(dispatcher) {
        val viewModel = TasbihViewModel(FakeTasbihRepository())
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertEquals(0f, viewModel.uiState.value.fraction, 0.0001f)

        viewModel.setTarget(4)
        repeat(2) { viewModel.increment() }
        advanceUntilIdle()

        assertEquals(0.5f, viewModel.uiState.value.fraction, 0.0001f)
        assertEquals(2, viewModel.uiState.value.remaining)
    }

    @Test
    fun everyAcceptedSession_isPersistedLocally() = runTest(dispatcher) {
        val repository = FakeTasbihRepository()
        val viewModel = TasbihViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.increment()
        viewModel.setTarget(99)
        viewModel.increment()
        advanceUntilIdle()

        assertEquals(repository.session, viewModel.uiState.value.session)
        assertEquals(3, repository.saves.value)
    }

    @Test
    fun aNoOpTap_isNotWritten() = runTest(dispatcher) {
        val repository = FakeTasbihRepository()
        val viewModel = TasbihViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.decrement()
        viewModel.reset()
        advanceUntilIdle()

        assertEquals(0, repository.saves.value)
    }

    private class FakeTasbihRepository(
        initial: TasbihSession? = TasbihSession.Empty,
    ) : TasbihRepository {
        private val stored = MutableStateFlow(initial)
        val saves = MutableStateFlow(0)

        /** Null while storage has not answered yet. */
        val session: TasbihSession get() = stored.value ?: TasbihSession.Empty

        override fun observeSession(): Flow<TasbihSession> =
            stored.map { it ?: TasbihSession.Empty }

        override suspend fun save(session: TasbihSession) {
            saves.value = saves.value + 1
            emit(session)
        }

        fun emit(value: TasbihSession) {
            stored.value = value
        }
    }
}
