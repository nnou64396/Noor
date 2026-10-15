package com.noor.islamic

import android.annotation.SuppressLint
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.noor.islamic.data.local.athkar.AthkarCorpus
import com.noor.islamic.data.local.athkar.AthkarCorpusLoader
import com.noor.islamic.data.local.userdata.AthkarPracticeRepository
import com.noor.islamic.domain.model.athkar.Dhikr
import com.noor.islamic.domain.model.athkar.DhikrCategory
import com.noor.islamic.domain.model.athkar.DhikrCount
import com.noor.islamic.domain.repository.AthkarProvenance
import com.noor.islamic.presentation.athkar.AthkarCategoryScreen
import com.noor.islamic.presentation.athkar.AthkarCategoryViewModel
import com.noor.islamic.presentation.athkar.AthkarScreen
import com.noor.islamic.presentation.athkar.AthkarViewModel
import com.noor.islamic.ui.theme.NoorTheme
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test

/**
 * Athkar screen rendering. Compiled for device runs; only runs when an
 * emulator/device is available. The Arabic below is a layout fixture, not content.
 *
 * The ViewModel is constructed inline in each `setContent` block on purpose: these
 * tests assert on a specific, pre-seeded repository, and a test rule has no
 * configuration-change lifecycle to survive, which is the only thing
 * `viewModel()` buys over direct construction.
 */
@OptIn(ExperimentalTestApi::class)
@SuppressLint("ViewModelConstructorInComposable")
class AthkarScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun athkar_showsThePendingStateWhenNoContentIsBundled() {
        val repository = AthkarPracticeRepository(
            corpusSource = FixedCorpus(AthkarCorpus.empty(note = "nothing bundled")),
            progressDao = RecordingAthkarProgressDao(),
            ioDispatcher = Dispatchers.Unconfined,
        )

        compose.setContent {
            NoorTheme {
                AthkarScreen(viewModel = AthkarViewModel(repository, repository)) {}
            }
        }

        compose.waitForIdle()
        compose.onNodeWithTag("athkar_no_content").assertIsDisplayed()
    }

    @Test
    fun athkar_listsTheBundledCategories() {
        val repository = repositoryWithContent()

        compose.setContent {
            NoorTheme {
                AthkarScreen(viewModel = AthkarViewModel(repository, repository)) {}
            }
        }

        compose.waitForIdle()
        compose.onNodeWithTag("athkar_category_morning").assertIsDisplayed()
        compose.onNodeWithTag("athkar_category_evening").assertIsDisplayed()
    }

    @Test
    fun athkar_openingACategory_showsItsAdhkar() {
        val repository = repositoryWithContent()
        val opened = mutableListOf<String>()

        compose.setContent {
            NoorTheme {
                AthkarScreen(
                    viewModel = AthkarViewModel(repository, repository),
                    onOpenCategory = { opened += it },
                )
            }
        }

        compose.waitForIdle()
        compose.onNodeWithTag("athkar_category_morning").performClick()

        assert(opened == listOf("morning")) { "expected the morning slug, got $opened" }
    }

    @Test
    fun category_countsRepetitionsUpToTheRequiredCount() {
        val repository = repositoryWithContent()
        val viewModel = AthkarCategoryViewModel("morning", repository, repository)

        compose.setContent {
            NoorTheme {
                AthkarCategoryScreen(viewModel = viewModel, onBack = {})
            }
        }

        compose.waitForIdle()
        compose.onNodeWithTag("athkar_dhikr_list").assertIsDisplayed()

        // morning.1 needs three repetitions.
        repeat(3) {
            compose.onNodeWithTag("athkar_dhikr_morning.1").performClick()
            compose.waitForIdle()
        }

        compose.onNodeWithTag("athkar_dhikr_progress_morning.1").assertIsDisplayed()
    }

    @Test
    fun category_forAnUnspecifiedCount_showsNoProgressBarButStillCounts() {
        val repository = repositoryWithUnspecifiedContent()
        val viewModel = AthkarCategoryViewModel("morning", repository, repository)

        compose.setContent {
            NoorTheme {
                AthkarCategoryScreen(viewModel = viewModel, onBack = {})
            }
        }

        compose.waitForIdle()

        // Counting still works, so the card must be tappable and must not offer a bar.
        repeat(3) {
            compose.onNodeWithTag("athkar_dhikr_morning.open").performClick()
            compose.waitForIdle()
        }

        compose.onNodeWithTag("athkar_dhikr_morning.open").assertIsDisplayed()
        compose.onNodeWithTag("athkar_dhikr_progress_morning.open").assertDoesNotExist()

        // The category has one prescribed dhikr, so the category bar is still real.
        compose.onNodeWithTag("athkar_category_progress").assertIsDisplayed()
    }

    @Test
    fun category_withOnlyUnspecifiedCounts_showsNoCategoryProgressBar() {
        val repository = repositoryWithOnlyUnspecifiedContent()

        compose.setContent {
            NoorTheme {
                AthkarCategoryScreen(
                    viewModel = AthkarCategoryViewModel("morning", repository, repository),
                    onBack = {},
                )
            }
        }

        compose.waitForIdle()

        compose.onNodeWithTag("athkar_dhikr_list").assertIsDisplayed()
        // No prescribed count anywhere, so no percentage is drawn.
        compose.onNodeWithTag("athkar_category_progress").assertDoesNotExist()
    }

    @Test
    fun category_forAnUnknownSlug_showsNothingToCount() {
        val repository = repositoryWithContent()

        compose.setContent {
            NoorTheme {
                AthkarCategoryScreen(
                    viewModel = AthkarCategoryViewModel("nope", repository, repository),
                    onBack = {},
                )
            }
        }

        compose.waitForIdle()
        compose.onNodeWithTag("athkar_category_unavailable").assertIsDisplayed()
    }

    private fun repositoryWithContent(): AthkarPracticeRepository = AthkarPracticeRepository(
        corpusSource = FixedCorpus(
            AthkarCorpus(
                schemaVersion = 1,
                provenance = AthkarProvenance(
                    corpusId = "test.corpus",
                    title = "Test",
                    isContentBundled = true,
                    bundledDhikrCount = 3,
                ),
                categories = listOf(
                    DhikrCategory("morning", 0, "Morning adhkar"),
                    DhikrCategory("evening", 1, "Evening adhkar"),
                ),
                dhikr = listOf(
                    dhikr("morning.1", "morning", 0, DhikrCount.Prescribed(3)),
                    dhikr("morning.2", "morning", 1, DhikrCount.Prescribed(1)),
                    dhikr("evening.1", "evening", 0, DhikrCount.Prescribed(1)),
                ),
            ),
        ),
        progressDao = RecordingAthkarProgressDao(),
        ioDispatcher = Dispatchers.Unconfined,
    )

    private fun repositoryWithUnspecifiedContent(): AthkarPracticeRepository =
        AthkarPracticeRepository(
            corpusSource = FixedCorpus(
                AthkarCorpus(
                    schemaVersion = 1,
                    provenance = AthkarProvenance(
                        corpusId = "test.corpus",
                        title = "Test",
                        isContentBundled = true,
                        bundledDhikrCount = 2,
                    ),
                    categories = listOf(DhikrCategory("morning", 0, "Morning adhkar")),
                    dhikr = listOf(
                        dhikr("morning.1", "morning", 0, DhikrCount.Prescribed(3)),
                        dhikr("morning.open", "morning", 1, DhikrCount.Unspecified),
                    ),
                ),
            ),
            progressDao = RecordingAthkarProgressDao(),
            ioDispatcher = Dispatchers.Unconfined,
        )

    private fun repositoryWithOnlyUnspecifiedContent(): AthkarPracticeRepository =
        AthkarPracticeRepository(
            corpusSource = FixedCorpus(
                AthkarCorpus(
                    schemaVersion = 1,
                    provenance = AthkarProvenance(
                        corpusId = "test.corpus",
                        title = "Test",
                        isContentBundled = true,
                        bundledDhikrCount = 1,
                    ),
                    categories = listOf(DhikrCategory("morning", 0, "Morning adhkar")),
                    dhikr = listOf(
                        dhikr("morning.open", "morning", 0, DhikrCount.Unspecified),
                    ),
                ),
            ),
            progressDao = RecordingAthkarProgressDao(),
            ioDispatcher = Dispatchers.Unconfined,
        )

    private fun dhikr(id: String, categoryId: String, order: Int, repeatCount: DhikrCount) = Dhikr(
        id = id,
        categoryId = categoryId,
        arabicText = "سُبْحَانَ اللَّهِ",
        translation = null,
        repeatCount = repeatCount,
        reference = "Reference",
        order = order,
    )

    private class FixedCorpus(private val corpus: AthkarCorpus) : AthkarCorpusLoader {
        override suspend fun load(): AthkarCorpus = corpus
    }
}
