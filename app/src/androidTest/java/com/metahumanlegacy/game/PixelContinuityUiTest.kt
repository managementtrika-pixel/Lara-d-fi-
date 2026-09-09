package com.metahumanlegacy.game

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PixelContinuityUiTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun creatorPixelRendererRemainsActiveAfterStartingLife() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        clearCampaignV4(context)
        context.getSharedPreferences("mhl_ultimate_state_v1", Context.MODE_PRIVATE).edit().clear().commit()

        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitForIdle()
            compose.onNodeWithText("Commencer une vie").performClick()
            compose.waitForIdle()

            compose.onNodeWithContentDescription("Pixel avatar|", substring = true).assertExists()

            listOf(
                "CHOISIR MA SILHOUETTE",
                "CRÉER MON LOOK",
                "RACONTER MA VIE",
                "CHOISIR MA VILLE",
                "VOIR MON IDENTITÉ",
                "COMMENCER À 8 ANS"
            ).forEach { label ->
                compose.onNodeWithText(label).performClick()
                compose.waitForIdle()
            }

            // Regression gate for the exact physical-test bug: gameplay must still use the
            // pixel renderer rather than swapping to the legacy smooth portrait.
            compose.onNodeWithContentDescription("Pixel avatar|", substring = true).assertExists()

            val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            val out = File(context.getExternalFilesDir(null), "pixel-continuity.png")
            FileOutputStream(out).use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
