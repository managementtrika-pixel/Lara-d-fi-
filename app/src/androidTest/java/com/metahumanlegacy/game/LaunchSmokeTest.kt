package com.metahumanlegacy.game

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LaunchSmokeTest {
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
            onView(withText("METAHUMAN")).check(matches(isDisplayed()))
            onView(withText("LEGACY")).check(matches(isDisplayed()))
            onView(withText("COMMENCER UNE VIE")).check(matches(isDisplayed())).perform(click())
            onView(withText("QUI ES-TU ?")).check(matches(isDisplayed()))
            onView(withText("ALÉATOIRE")).check(matches(isDisplayed()))
        }
    }
}
