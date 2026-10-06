package com.metahumanlegacy.game

import java.io.File
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FinalGameRegressionTest {
    @Before fun loadContent() {
        NarrativeCodec.installAssetParts { File("src/main/assets/$it").readBytes() }
    }
    private fun session(seed: Long): FinalSession {
        val c = GameEngine.newCampaign(seed)
        val u = UltimateStore.create(c, UltimateCatalog.randomDraft(seed, GameEngine.randomBlueprint(seed)))
        return FinalSession(c, u, AnnualActionState.fresh(c), DeepLifeDirector.bootstrap(c, u))
    }
    @Test fun repeatedTapAndOutdatedEventCannotAdvanceAnotherYear() {
        val s = session(50501)
        val event = FinalGameRules.event(s.campaign, s.ultimate, s.annual, s.deep)
        val next = FinalGameRuntime.resolve(s, event, event.choices.first())!!
        assertEquals(s.campaign.turn + 1, next.campaign.turn)
        assertNull(FinalGameRuntime.resolve(next, event, event.choices.first()))
        assertNull(FinalGameRuntime.resolve(next.copy(outcome = null), event, event.choices.first()))
        assertFalse(FinalGameRules.canChoose(next.campaign, s.campaign, null))
    }
    @Test fun multipleStoryScenesInOneCalendarYearDoNotRefillFreeTime() {
        val c = GameEngine.newCampaign(50502).copy(turn = 16)
        val spent = AnnualActionState.fresh(c).copy(used = 3, usedIds = setOf("rest"), discipline = 12)
        val sameYear = spent.synced(c.copy(turn = 17))
        assertEquals(c.age, c.copy(turn = 17).age)
        assertEquals(0, sameYear.remaining)
        assertEquals(setOf("rest"), sameYear.usedIds)
        val nextYear = sameYear.synced(c.copy(turn = 20))
        assertEquals(3, nextYear.remaining)
        assertEquals(12, nextYear.discipline)
    }
    @Test fun lifeActionsAndCareerActionsConsumeOneSharedBudget() {
        var s = FinalSessionPersistence.normalized(session(50503))
        val card = AnnualActionEngine.available(s.campaign, s.annual).first()
        val classic = AnnualActionEngine.perform(s.campaign, s.annual, card)!!
        val life = s.deep.lifeSimulation!!
        s = FinalSessionPersistence.normalized(s.copy(campaign = classic.campaign, annual = classic.state,
            deep = s.deep.copy(lifeSimulation = life.copy(civil = life.civil.copy(freeMoments = life.civil.freeMoments - 1)))))
        assertEquals(2, s.annual.remaining)
        val rest = LifeSimulationDirector.perform(s.campaign, s.deep.lifeSimulation!!, LifeAction(LifeActionType.REST, label = "Repos"))
        s = FinalSessionPersistence.normalized(s.copy(annual = s.annual.copy(used = s.annual.used + 1),
            deep = s.deep.copy(lifeSimulation = rest.state)))
        assertEquals(1, s.annual.remaining)
        assertEquals(1, s.deep.lifeSimulation!!.civil.freeMoments)
    }
    @Test fun completeLivesKeepFormationAwakeningConsequencesAndAReachableEnding() {
        for (seed in 50510L..50517L) {
            var s = session(seed)
            repeat(256) {
                if (s.campaign.finished) return@repeat
                if (s.campaign.needsAlias) s = s.copy(campaign = GameEngine.setAlias(s.campaign, "Aster"))
                val c = s.campaign
                if (c.turn < 10) {
                    assertEquals(8 + c.turn, c.age)
                    assertFalse(c.powerRevealed)
                }
                val normalized = FinalSessionPersistence.normalized(s)
                val event = FinalGameRules.event(normalized.campaign, normalized.ultimate, normalized.annual, normalized.deep)
                assertTrue("Empty event at ${c.turn}", event.choices.isNotEmpty())
                val index = ((seed + c.turn) % event.choices.size).toInt()
                s = FinalGameRuntime.resolve(normalized, event, event.choices[index])!!
                assertFalse(s.outcome.isNullOrBlank())
                assertEquals(c.turn + 1, s.campaign.turn)
                assertEquals(s.annual.remaining, s.deep.lifeSimulation!!.civil.freeMoments)
                if (s.campaign.turn == 11) assertTrue(s.campaign.powerRevealed)
                s = s.copy(outcome = null)
            }
            assertTrue("Unfinished seed $seed", s.campaign.finished)
            assertTrue(GameEngine.legacyTitle(s.campaign).isNotBlank())
        }
    }
}
