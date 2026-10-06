package com.metahumanlegacy.game

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinalJourneyTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Before fun reset() {
        listOf("legacy", "mhl_ultimate_session_v1", "mhl_deep_legacy_v2").forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
        }
        MetahumanMotionPreferences.save(context, MetahumanMotionSettings(reduceMotion = true, haptics = false))
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot(useUnmergedTree = true).captureToImage().asAndroidBitmap()
        val dir = File(context.getExternalFilesDir(null), "final-preview").apply { mkdirs() }
        val file = File(dir, "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        // The Android test runner removes application storage when it finishes.
        // Copy captures to the emulator's public test folder before that cleanup.
        val shell = InstrumentationRegistry.getInstrumentation().uiAutomation
        shell.executeShellCommand("mkdir -p /sdcard/Download/metahuman-final-preview").use { descriptor ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
        }
        val destination = "/sdcard/Download/metahuman-final-preview/$name.png"
        // UiAutomation executes argv directly; generated Android paths contain no spaces.
        shell.executeShellCommand("cp ${file.absolutePath} $destination").use { descriptor ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
        }
        val size = shell.executeShellCommand("wc -c $destination").use { descriptor ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes().toString(Charsets.UTF_8) }
        }.trim().substringBefore(' ').toLong()
        assertEquals("Device screenshot was not preserved", file.length(), size)
    }
    private fun chooseFirst() {
        compose.onNodeWithTag("final_choice_1").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("final_continue").performScrollTo().assertIsDisplayed().performClick()
    }
    @Test fun createAwakenResumeActAndExploreTheActualApp() {
        var seed = 0L
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.onNodeWithText("LEGACY").assertIsDisplayed()
            capture("01-home")
            compose.onNodeWithText("Commencer une vie", ignoreCase = true).performClick()
            compose.onNodeWithText("QUI ES-TU ?").assertIsDisplayed()
            capture("02-creator")
            compose.onNodeWithTag("creator_next").performClick()
            compose.onNodeWithText("TA SILHOUETTE").assertIsDisplayed()
            scenario.recreate()
            compose.onNodeWithText("TA SILHOUETTE").assertIsDisplayed()
            repeat(5) { compose.onNodeWithTag("creator_next").performClick() }
            val created = FinalSessionPersistence.load(context)!!
            seed = created.campaign.seed
            assertEquals(8, created.campaign.age)
            assertFalse(created.campaign.powerRevealed)
            capture("03-childhood")
            compose.onNodeWithTag("final_choice_1").performScrollTo().performTouchInput { doubleClick(center) }
            compose.onNodeWithTag("final_continue").performScrollTo().assertIsDisplayed()
            assertEquals(1, FinalSessionPersistence.load(context)!!.campaign.turn)
            scenario.recreate()
            compose.onNodeWithTag("final_continue").performScrollTo().assertIsDisplayed().performClick()
            repeat(9) { chooseFirst() }
            assertEquals(10, FinalSessionPersistence.load(context)!!.campaign.turn)
            assertFalse(FinalSessionPersistence.load(context)!!.campaign.powerRevealed)
            capture("04-awakening")
            compose.onNodeWithTag("final_choice_1").performScrollTo().performClick()
            compose.onNodeWithTag("final_continue").performScrollTo().assertIsDisplayed()
            val awakened = FinalSessionPersistence.load(context)!!
            assertEquals(11, awakened.campaign.turn)
            assertTrue(awakened.campaign.needsAlias)
            assertFalse(awakened.outcome.isNullOrBlank())
        }
        ActivityScenario.launch(MainActivity::class.java).use { resumed ->
            compose.onNodeWithText("Continuer cette vie", ignoreCase = true).performClick()
            compose.onNodeWithTag("final_continue").performScrollTo().assertIsDisplayed()
            assertEquals(seed, FinalSessionPersistence.load(context)!!.campaign.seed)
            capture("05-unread-consequence")
            compose.onNodeWithTag("final_continue").performClick()
            compose.onNodeWithText("Construire une identité", ignoreCase = true).assertIsDisplayed()
            Espresso.pressBack()
            compose.onNodeWithText("Continuer cette vie", ignoreCase = true).performClick()
            compose.onNodeWithText("Construire une identité", ignoreCase = true).assertIsDisplayed()
            compose.onNodeWithText("Alias / nom de terrain").performScrollTo().performTextReplacement("Aster")
            resumed.recreate()
            compose.onNodeWithText("Aster").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("PRENDRE CETTE IDENTITÉ").performScrollTo().performClick()
            assertEquals("Aster", FinalSessionPersistence.load(context)!!.campaign.alias)
            capture("06-adult")
            compose.onNodeWithContentDescription("Agir 3").performClick()
            compose.onNodeWithText("TA VIE, PAS JUSTE TA LÉGENDE").assertIsDisplayed()
            val before = FinalSessionPersistence.load(context)!!
            val rest = LifeSimulationDirector.availableActions(before.campaign, before.deep.lifeSimulation!!)
                .first { action -> action.type == LifeActionType.REST }
            compose.onNodeWithText(rest.label, ignoreCase = true).performScrollTo().performClick()
            assertEquals(2, FinalSessionPersistence.load(context)!!.annual.remaining)
            assertEquals(2, FinalSessionPersistence.load(context)!!.deep.lifeSimulation!!.civil.freeMoments)
            capture("07-actions")
            Espresso.pressBack()
            compose.onNodeWithContentDescription("Ville").performClick()
            capture("08-city")
            Espresso.pressBack()
            compose.onNodeWithContentDescription("Moi").performClick()
            capture("09-character")
            Espresso.pressBack()
            compose.onNodeWithContentDescription("Réglages").performClick()
            compose.onNodeWithText("Grand").performScrollTo().performClick()
            compose.onNodeWithText("EXPORTER MA SAUVEGARDE").performScrollTo().assertIsDisplayed()
            capture("10-settings-backup")
            Espresso.pressBack()
            compose.onNodeWithTag("final_choice_1").performScrollTo().assertIsDisplayed()
            assertEquals(11, FinalSessionPersistence.load(context)!!.campaign.turn)
        }
    }
    @Test fun lastConsequenceIsReadBeforeLegacyAndArchiveIsPersistent() {
        var c = GameEngine.newCampaign(550590)
        repeat(11) {
            val event = GameEngine.event(c)
            c = GameEngine.resolve(c, event, event.choices.first()).campaign
        }
        c = GameEngine.setAlias(c, "Aster").copy(turn = 256, health = 75)
        val u = UltimateStore.fallback(c)
        val s = FinalSessionPersistence.normalized(FinalSession(c, u, AnnualActionState.fresh(c),
            DeepLifeDirector.bootstrap(c, u), "Une dernière promesse\n\nTu confies la ville à la génération suivante."))
        FinalSessionPersistence.save(context, s)
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("Continuer cette vie", ignoreCase = true).performClick()
            compose.onNodeWithText("Une dernière promesse").assertIsDisplayed()
            compose.onNodeWithTag("final_continue").performScrollTo().performClick()
            capture("11-legacy")
            compose.onNodeWithText("ARCHIVER DANS LE HALL").performScrollTo().performClick()
            assertNull(FinalSessionPersistence.load(context))
            assertEquals(c.seed, DeepLegacyArchive.load(context).single().seed)
            compose.onNodeWithText("Hall of Legacies  ·  1", ignoreCase = true).performClick()
            capture("12-hall")
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("Hall of Legacies  ·  1", ignoreCase = true).assertIsDisplayed()
            assertEquals(c.seed, DeepLegacyArchive.load(context).single().seed)
        }
    }
}
