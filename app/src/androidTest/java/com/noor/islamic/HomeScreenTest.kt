package com.noor.islamic

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.noor.islamic.presentation.home.HomeScreen
import com.noor.islamic.presentation.prayer.PrayerViewModel
import com.noor.islamic.ui.theme.NoorTheme
import org.junit.Rule
import org.junit.Test

/**
 * Verifies the Home dashboard renders its core content (greeting, quick
 * access tiles and the prayer section). Location is intentionally NOT
 * granted, so the non-intrusive "enable prayer times" card is shown.
 */
@OptIn(ExperimentalTestApi::class)
class HomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun home_rendersGreetingAndQuickAccess() {
        val prayerViewModel = PrayerViewModel(
            permissionChecker = NotGrantedPermissionChecker(),
            locationRepository = FakeLocationRepository(),
            prayerRepository = FakePrayerRepository(),
        )

        compose.setContent {
            NoorTheme {
                HomeScreen(prayerViewModel = prayerViewModel, onOpenDestination = {})
            }
        }

        compose.onNodeWithTag("home_greeting").assertIsDisplayed()
        compose.onNodeWithTag("home_prayer_card").assertIsDisplayed()
        compose.onNodeWithTag("quick_quran").assertIsDisplayed()
        compose.onNodeWithTag("quick_athkar").assertIsDisplayed()
        compose.onNodeWithTag("quick_qibla").assertIsDisplayed()
        compose.onNodeWithTag("quick_tasbih").assertIsDisplayed()
    }
}