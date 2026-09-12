package com.metahumanlegacy.game

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LaunchSmokeTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun clearActiveLife() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("legacy", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("mhl_ultimate_session_v1", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun mainActivityLaunchesAndCreatesAWindow() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(activity.isFinishing)
                assertNotNull(activity.window.decorView)
            }
        }
    }

    @Test
    fun homeRendersAndPlayerCanReachTheRealCharacterCreator() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("METAHUMAN").assertIsDisplayed()
            compose.onNodeWithText("LEGACY").assertIsDisplayed()
            compose.onNodeWithText("COMMENCER UNE VIE", ignoreCase = true).assertIsDisplayed().performClick()
            compose.onNodeWithText("QUI ES-TU ?").assertIsDisplayed()
            compose.onNodeWithText("ALÉATOIRE").assertIsDisplayed()
        }
    }
}
