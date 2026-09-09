package com.metahumanlegacy.game

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.fetchSemanticsNode
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PixelContinuityUiTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val pixelAvatarMatcher = SemanticsMatcher("pixel avatar") { node ->
        node.config.getOrNull(SemanticsProperties.ContentDescription)
            ?.any { it.startsWith("Pixel avatar|") } == true
    }

    @Test
    fun creatorAvatarRemainsTheSameAvatarAfterStartingLife() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        clearCampaignV4(context)
        context.getSharedPreferences("mhl_ultimate_state_v1", Context.MODE_PRIVATE).edit().clear().commit()

        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitForIdle()
            compose.onNodeWithText("Commencer une vie").performClick()
            compose.waitForIdle()

            val creatorKey = compose.onNode(pixelAvatarMatcher).fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription]
                .first { it.startsWith("Pixel avatar|") }

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

            val runtimeKey = compose.onNode(pixelAvatarMatcher).fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription]
                .first { it.startsWith("Pixel avatar|") }

            assertEquals(creatorKey, runtimeKey)
        }
    }
}
