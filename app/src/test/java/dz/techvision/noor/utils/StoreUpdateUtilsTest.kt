package dz.techvision.noor.utils

import android.content.Context
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StoreUpdateUtilsTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun `openStoreListing launches market intent for official app store`() {
        StoreUpdateUtils.openStoreListing(context)

        val shadowApp = shadowOf(RuntimeEnvironment.getApplication())
        val startedIntent: Intent? = shadowApp.nextStartedActivity

        assertNotNull(startedIntent)
        assertEquals(Intent.ACTION_VIEW, startedIntent?.action)
        assertEquals("market://details?id=dz.techvision.noor", startedIntent?.dataString)
    }
}
