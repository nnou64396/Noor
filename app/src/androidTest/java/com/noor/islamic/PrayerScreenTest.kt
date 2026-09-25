package com.noor.islamic

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.noor.islamic.presentation.prayer.PrayerScreen
import com.noor.islamic.presentation.prayer.PrayerViewModel
import com.noor.islamic.ui.theme.NoorTheme
import org.junit.Rule
import org.junit.Test

/**
 * Prayer screen rendering. Compiled for device runs; only ran when an
 * emulator/device is available. Two states are covered below.
 */
@OptIn(ExperimentalTestApi::class)
class PrayerScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun prayer_showsPermissionFlowWhenLocationIsNotGranted() {
        val viewModel = PrayerViewModel(
            permissionChecker = NotGrantedPermissionChecker(),
            locationRepository = FakeLocationRepository(),
            prayerRepository = FakePrayerRepository(),
        )

        compose.setContent {
            NoorTheme {
                PrayerScreen(viewModel = viewModel)
            }
        }

        compose.onNodeWithTag("prayer_screen").assertIsDisplayed()
        compose.onNodeWithTag("location_permission_card").assertIsDisplayed()
    }

    @Test
    fun prayer_rendersScheduleWhenGrantedAndLoaded() {
        val viewModel = PrayerViewModel(
            permissionChecker = GrantedPermissionChecker(),
            locationRepository = FakeLocationRepository(),
            prayerRepository = FakePrayerRepository(),
        )

        compose.setContent {
            NoorTheme {
                PrayerScreen(viewModel = viewModel)
            }
        }

        compose.waitForIdle()
        compose.onNodeWithTag("prayer_next_card").assertIsDisplayed()
        compose.onNodeWithTag("prayer_list").assertIsDisplayed()
    }
}