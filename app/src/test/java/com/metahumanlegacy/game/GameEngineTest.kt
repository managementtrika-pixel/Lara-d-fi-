package com.metahumanlegacy.game

import org.junit.Assert.*
import org.junit.Test

class GameEngineTest {
    @Test fun fullConnectedPackIsLoadedWithoutDuplicates() {
        val s = GameEngine.catalogStats()
        assertEquals(1050, s.events)
        assertEquals(3600, s.choices)
        assertEquals(50, s.arcs)
        assertEquals(200, s.epilogues)
        assertEquals(0, s.duplicateIds)
        assertEquals(0, s.duplicateTitles)
        assertEquals(0, s.duplicateTexts)
        assertEquals(3400, GameEngine.debugEffectsCount())
    }

    @Test fun authoredEventsExposeFourSituationSpecificChoices() {
        val e = GameEngine.debugEventById("A01_TUNNEL_CHILD_S1")!!
        assertEquals(4, e.choices.size)
        assertEquals(4, e.choices.map { it.label }.toSet().size)
        assertEquals(setOf("CARE", "ORDER", "TRUTH", "ASCEND"), e.choices.map { it.approach }.toSet())
    }

    @Test fun aDecisionUnlocksTheCorrectLaterRoute() {
        val bp = CharacterBlueprint("Ava","Vale","Halo","elle","Vesper","Centre","Milieu scientifique","Mutation naturelle","Énergie","Surcharge","Protéger les miens","Masque minimal")
        val start = GameEngine.newCampaign(42L, bp)
        val s1 = GameEngine.debugEventById("A01_TUNNEL_CHILD_S1")!!
        val care = s1.choices.first { it.approach == "CARE" }
        val after = GameEngine.resolve(start, s1, care).campaign
        assertTrue("A01_TUNNEL_CHILD_S1_CARE" in after.flags)
        assertTrue(after.threads.any { it.id == "A01_TUNNEL_CHILD" })
        val s2 = GameEngine.debugEventById("A01_TUNNEL_CHILD_S2_FROM_CARE")!!
        assertEquals("A01_TUNNEL_CHILD", s2.threadId)
        assertEquals(2, s2.threadStage)
    }

    @Test fun extendedEffectsAreReallyApplied() {
        val bp = CharacterBlueprint("Ava","Vale","Halo","elle","Vesper","Centre","Milieu scientifique","Mutation naturelle","Énergie","Surcharge","Protéger les miens","Masque minimal")
        val c = GameEngine.newCampaign(91L, bp)
        val e = GameEngine.debugEventById("A01_TUNNEL_CHILD_S1")!!
        val truth = e.choices.first { it.approach == "TRUTH" }
        assertTrue(truth.identityDelta > 0)
        val next = GameEngine.resolve(c, e, truth).campaign
        assertTrue(next.identityExposure > c.identityExposure)
        assertTrue(next.influence > c.influence)
    }

    @Test fun majorChoicesCarryMoreWeight() {
        val crisis = GameEngine.debugEventById("A01_TUNNEL_CHILD_S4_FROM_CARE")!!
        assertTrue(crisis.stakes >= 2)
        val c = GameEngine.newCampaign(333L)
        val choice = crisis.choices.first { it.approach == "CARE" }
        val next = GameEngine.resolve(c, crisis, choice).campaign
        assertTrue(kotlin.math.abs(next.morality - c.morality) >= kotlin.math.abs(choice.moral))
    }

    @Test fun sameSeedAndChoiceStayDeterministic() {
        val a = GameEngine.newCampaign(999L)
        val b = GameEngine.newCampaign(999L)
        val ea = GameEngine.event(a)
        val eb = GameEngine.event(b)
        assertEquals(ea.id, eb.id)
        val ra = GameEngine.resolve(a, ea, ea.choices[2])
        val rb = GameEngine.resolve(b, eb, eb.choices[2])
        assertEquals(ra.campaign, rb.campaign)
        assertEquals(ra.outcome, rb.outcome)
    }

    @Test fun longCareerKeepsNarrativeVarietyAndNeverStarves() {
        var c = GameEngine.newCampaign(12345L)
        val seen = linkedSetOf<String>()
        repeat(140) { turn ->
            if (!c.finished) {
                val e = GameEngine.event(c)
                assertTrue(e.choices.isNotEmpty())
                seen += e.id
                c = GameEngine.resolve(c, e, e.choices[turn % e.choices.size]).campaign
            }
        }
        assertTrue("only ${seen.size} unique events", seen.size > 80)
        assertTrue(c.timeline.size >= 100)
    }
}
