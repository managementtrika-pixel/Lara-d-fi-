package com.metahumanlegacy.game

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinalPersistenceTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Before fun reset() {
        context.getSharedPreferences("legacy", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("mhl_ultimate_session_v1", Context.MODE_PRIVATE).edit().clear().commit()
    }
    private fun session(): FinalSession {
        val c = GameEngine.newCampaign(550501L)
        val d = UltimateCatalog.randomDraft(c.seed, GameEngine.randomBlueprint(c.seed)).copy(facialHair = "Aucune")
        val u = UltimateStore.create(c, d)
        return FinalSessionPersistence.normalized(FinalSession(c, u, AnnualActionState.fresh(c), DeepLifeDirector.bootstrap(c, u)))
    }
    @Test fun checkpointKeepsEveryLayerAndUnreadConsequenceTogether() {
        val s = session()
        val e = FinalGameRules.event(s.campaign, s.ultimate, s.annual, s.deep)
        val next = FinalGameRuntime.resolve(s, e, e.choices.first())!!
        FinalSessionPersistence.save(context, next)
        assertEquals(next, FinalSessionPersistence.load(context))
        assertEquals(next.campaign, loadCampaignV4(context))
        assertFalse(FinalSessionPersistence.load(context)!!.outcome.isNullOrBlank())
    }
    @Test fun legacy45SaveMigratesWithItsPendingConsequence() {
        val s = session()
        saveCampaignV4(context, s.campaign)
        UltimateStore.save(context, s.ultimate)
        AnnualActionPersistence.save(context, s.annual)
        DeepLifePersistence.save(context, s.deep)
        context.getSharedPreferences("mhl_ultimate_session_v1", Context.MODE_PRIVATE).edit()
            .putString("outcome_${s.campaign.seed}", "La promesse\n\nQuelqu'un s'en souvient.").commit()
        val migrated = FinalSessionPersistence.load(context)!!
        assertEquals(s.campaign, migrated.campaign)
        assertEquals(s.ultimate, migrated.ultimate)
        assertEquals("La promesse\n\nQuelqu'un s'en souvient.", migrated.outcome)
        assertNotNull(context.getSharedPreferences("legacy", Context.MODE_PRIVATE).getString(FinalSessionPersistence.ACTIVE, null))
    }
    @Test fun damagedCheckpointFallsBackToPreviousCompleteLife() {
        val s = session()
        FinalSessionPersistence.save(context, s)
        val e = FinalGameRules.event(s.campaign, s.ultimate, s.annual, s.deep)
        FinalSessionPersistence.save(context, FinalGameRuntime.resolve(s, e, e.choices.first())!!)
        context.getSharedPreferences("legacy", Context.MODE_PRIVATE).edit().putString(FinalSessionPersistence.ACTIVE, "broken").commit()
        assertEquals(s, FinalSessionPersistence.load(context))
    }
    @Test fun backupRoundTripAndForeignSeedValidationProtectExistingLife() {
        val s = session().copy(outcome = "Un souvenir\n\nUn texte avec | ; et des accents : éveil.")
        FinalSessionPersistence.save(context, s)
        val raw = FinalBackupStore.export(context)
        val parsed = FinalBackupStore.decode(raw)
        assertEquals(s, parsed.session)
        clearCampaignV4(context)
        FinalBackupStore.restore(context, parsed)
        assertEquals(s, FinalSessionPersistence.load(context))
        val foreign = JSONObject(raw)
        val session = foreign.getJSONObject("session")
        val other = s.ultimate.copy(seed = s.campaign.seed + 1)
        session.put("ultimate", UltimateStore.encode(other))
        assertTrue(runCatching { FinalBackupStore.decode(foreign.toString()) }.isFailure)
        assertEquals(s, FinalSessionPersistence.load(context))
    }
    @Test fun draftRoundTripPreservesNamesAndEveryVisualChoice() {
        val b = GameEngine.randomBlueprint(5055).copy(firstName = "Nora | étoile", lastName = "De Vesper")
        val draft = UltimateCatalog.randomDraft(5055, b)
        assertEquals(draft, FinalDraftCodec.decode(FinalDraftCodec.encode(draft)))
    }
}
