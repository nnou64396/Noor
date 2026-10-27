package dz.techvision.noor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import dz.techvision.noor.presentation.qibla.QiblaScreen
import dz.techvision.noor.ui.theme.NoorTheme
import org.junit.Rule
import org.junit.Test

/**
 * Qibla screen renders its root and — with location not granted — the
 * shared permission card. Compiled for device runs only.
 */
@OptIn(ExperimentalTestApi::class)
class QiblaScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun qibla_rendersRootAndPermissionCard() {
        compose.setContent {
            NoorTheme {
                QiblaScreen()
            }
        }

        compose.onNodeWithTag("qibla_screen").assertIsDisplayed()
        compose.onNodeWithTag("location_permission_card").assertIsDisplayed()
    }
}
