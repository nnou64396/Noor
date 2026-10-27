package dz.techvision.noor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dz.techvision.noor.data.local.userdata.TasbihPracticeRepository
import dz.techvision.noor.presentation.tasbih.TasbihScreen
import dz.techvision.noor.presentation.tasbih.TasbihViewModel
import dz.techvision.noor.ui.theme.NoorTheme
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Tasbih counter rendering and the tap target. Compiled for device runs; only
 * runs when an emulator/device is available.
 *
 * The assertions read the ViewModel state rather than the drawn text, so they
 * check that a tap on the real UI reaches the real counter without depending on
 * the exact wording of a localized label.
 */
@OptIn(ExperimentalTestApi::class)
class TasbihScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun tasbih_showsTheCounterAndTargets() {
        compose.setContent {
            NoorTheme {
                TasbihScreen(viewModel = viewModel())
            }
        }

        compose.waitForIdle()
        compose.onNodeWithTag("tasbih_progress").assertIsDisplayed()
        compose.onNodeWithTag("tasbih_count").assertIsDisplayed()
        compose.onNodeWithTag("tasbih_target_33").assertIsDisplayed()
        compose.onNodeWithTag("tasbih_target_99").assertIsDisplayed()
        compose.onNodeWithTag("tasbih_tap_area").assertIsDisplayed()
    }

    @Test
    fun tasbih_tappingTheRingCountsUp() {
        val viewModel = viewModel()
        compose.setContent { NoorTheme { TasbihScreen(viewModel = viewModel) } }

        compose.waitForIdle()
        repeat(4) {
            compose.onNodeWithTag("tasbih_tap_area").performClick()
            compose.waitForIdle()
        }

        assertEquals(4, viewModel.uiState.value.count)
    }

    @Test
    fun tasbih_undoStepsBackByOne() {
        val viewModel = viewModel()
        compose.setContent { NoorTheme { TasbihScreen(viewModel = viewModel) } }

        compose.waitForIdle()
        repeat(3) {
            compose.onNodeWithTag("tasbih_tap_area").performClick()
            compose.waitForIdle()
        }
        compose.onNodeWithTag("tasbih_undo").performClick()
        compose.waitForIdle()

        assertEquals(2, viewModel.uiState.value.count)
    }

    @Test
    fun tasbih_theCountStopsAtTheTarget() {
        val viewModel = viewModel()
        compose.setContent { NoorTheme { TasbihScreen(viewModel = viewModel) } }

        compose.waitForIdle()
        compose.onNodeWithTag("tasbih_target_33").performClick()
        compose.waitForIdle()
        repeat(36) {
            compose.onNodeWithTag("tasbih_tap_area").performClick()
            compose.waitForIdle()
        }

        assertEquals(33, viewModel.uiState.value.count)
    }

    @Test
    fun tasbih_choosingATargetKeepsTheCount() {
        val viewModel = viewModel()
        compose.setContent { NoorTheme { TasbihScreen(viewModel = viewModel) } }

        compose.waitForIdle()
        repeat(5) {
            compose.onNodeWithTag("tasbih_tap_area").performClick()
            compose.waitForIdle()
        }
        compose.onNodeWithTag("tasbih_target_99").performClick()
        compose.waitForIdle()

        val state = viewModel.uiState.value
        assertEquals(99, state.target)
        assertEquals(5, state.count)
    }

    @Test
    fun tasbih_aCustomTargetCanBeEnteredAndSaved() {
        val viewModel = viewModel()
        compose.setContent { NoorTheme { TasbihScreen(viewModel = viewModel) } }

        compose.waitForIdle()
        compose.onNodeWithTag("tasbih_target_custom").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("tasbih_target_input").performTextInput("21")
        compose.onNodeWithTag("tasbih_target_confirm").performClick()
        compose.waitForIdle()

        assertEquals(21, viewModel.uiState.value.target)
    }

    @Test
    fun tasbih_resetStartsTheRoundAgainButKeepsTheTotal() {
        val viewModel = viewModel()
        compose.setContent { NoorTheme { TasbihScreen(viewModel = viewModel) } }

        compose.waitForIdle()
        repeat(7) {
            compose.onNodeWithTag("tasbih_tap_area").performClick()
            compose.waitForIdle()
        }
        compose.onNodeWithTag("tasbih_reset").performClick()
        compose.waitForIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.count)
        assertEquals(7, state.session.totalCount)
    }

    private fun viewModel() = TasbihViewModel(
        TasbihPracticeRepository(
            tasbihDao = InMemoryTasbihDao(),
            ioDispatcher = Dispatchers.Unconfined,
        ),
    )
}

