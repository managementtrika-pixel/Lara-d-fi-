package com.metahumanlegacy.game

import java.io.File
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GameEngineTest {
    @Before fun installNarrativeBundle() {
        NarrativeCodec.installAssetParts { path -> File("src/main/assets/$path").readBytes() }
    }

    @Test fun nobodyPackLoadsExactStructure() {
        val s = GameEngine.catalogStats()
        assertEquals(10, s.prologue)
        assertEquals(5, s.foundation)
        assertEquals(250, s.majorBeats)
        assertEquals(1000, s.majorChoices)
        assertEquals(50, s.arcs)
        assertEquals(200, s.endings)
        assertEquals(1064, GameEngine.constatCount())
    }

    @Test fun authoredConstatReplacesGenericImmediateSummary() {
        val c = GameEngine.newCampaign(
            42L,
            CharacterBlueprint(
                "Ava", "Vale", "elle", "Vesper", "Centre",
                "Classe moyenne", "Justice", "Études scientifiques", "Curieux"
            )
        )
        val e = GameEngine.event(c)
        val r = GameEngine.resolve(c, e, e.choices[0])
        assertTrue(r.outcome.startsWith("Un détail que tu oublieras presque\n\n"))
        assertTrue(r.outcome.contains("retourner dans la fumée"))
        assertFalse(r.outcome.contains("Tu ne sais pas encore ce que cette décision construit"))
        assertEquals("Rester du bon côté de soi", NarrativeCodec.constat("A01_TUNNEL_CHILD_S1_C1")?.title)
    }

    @Test fun careerStartsAsNobodyWithNoBuildChosen() {
        val c = GameEngine.newCampaign(42L, CharacterBlueprint("Ava", "Vale", "elle", "Vesper", "Centre", "Classe moyenne", "Justice", "Études scientifiques", "Curieux"))
        assertEquals(18, c.age)
        assertTrue(c.alias.isBlank())
        assertEquals("Non révélé", c.powerFamily)
        assertEquals("Inconnue", c.weakness)
        assertEquals(0, c.power)
        assertEquals(0, c.prestige)
        assertFalse(c.powerRevealed)
        assertEquals("PF01_NOBODY", GameEngine.event(c).id)
    }

    @Test fun tenHumanChoicesResolveButDoNotRevealPower() {
        var c = GameEngine.newCampaign(77L)
        repeat(10) { index ->
            val e = GameEngine.event(c)
            assertEquals(4, e.choices.size)
            assertEquals("FORMATIVE", e.kind)
            c = GameEngine.resolve(c, e, e.choices[index % 4]).campaign
        }
        assertEquals(28, c.age)
        assertTrue(c.powerResolved)
        assertFalse(c.powerRevealed)
        assertTrue(c.alias.isBlank())
        assertTrue(c.affinityScores.isNotEmpty())
        assertEquals("AWAKENING_00_FIRST_IMPOSSIBLE", GameEngine.event(c).id)
    }

    @Test fun awakeningRevealsExistingPowerAndOnlyThenAllowsAlias() {
        var c = GameEngine.newCampaign(91L)
        repeat(10) {
            val e = GameEngine.event(c)
            c = GameEngine.resolve(c, e, e.choices[0]).campaign
        }
        val resolvedPower = c.powerFamily
        val awakening = GameEngine.event(c)
        c = GameEngine.resolve(c, awakening, awakening.choices[3]).campaign
        assertEquals(resolvedPower, c.powerFamily)
        assertTrue(c.powerRevealed)
        assertTrue("POWER_REVEALED" in c.flags)
        assertTrue(c.needsAlias)
        val named = GameEngine.setAlias(c, "Aster")
        assertEquals("Aster", named.alias)
        assertFalse(named.needsAlias)
    }

    @Test fun sameSeedAndSameFormativeLifeProduceSamePower() {
        fun life(seed: Long): Campaign {
            var c = GameEngine.newCampaign(seed)
            val pattern = listOf(0, 2, 1, 3, 2, 0, 1, 3, 2, 1)
            repeat(10) { i ->
                val e = GameEngine.event(c)
                c = GameEngine.resolve(c, e, e.choices[pattern[i]]).campaign
            }
            return c
        }
        val a = life(1234L)
        val b = life(1234L)
        assertEquals(a.powerFamily, b.powerFamily)
        assertEquals(a.weakness, b.weakness)
        assertEquals(a.powerSignature, b.powerSignature)
        assertEquals(a.affinityScores, b.affinityScores)
    }

    @Test fun differentLivesActuallyChangeFormationVectors() {
        fun life(choiceIndex: Int): Campaign {
            var c = GameEngine.newCampaign(555L)
            repeat(10) {
                val e = GameEngine.event(c)
                c = GameEngine.resolve(c, e, e.choices[choiceIndex]).campaign
            }
            return c
        }
        val a = life(0)
        val b = life(3)
        assertNotEquals(a.affinityScores, b.affinityScores)
        assertTrue(a.powerFamily != b.powerFamily || a.weakness != b.weakness || a.expressionScores != b.expressionScores)
    }

    @Test fun majorArcsCannotStartBeforePowerReveal() {
        var c = GameEngine.newCampaign(808L)
        repeat(10) {
            val e = GameEngine.event(c)
            c = GameEngine.resolve(c, e, e.choices[0]).campaign
        }
        assertFalse(c.powerRevealed)
        assertEquals("AWAKENING", GameEngine.event(c).kind)
    }

    @Test fun startedArcContinuesEvenIfEntryScopeWouldNoLongerQualify() {
        var c = GameEngine.newCampaign(999L).copy(
            turn = 40, alias = "Test", powerFamily = "Énergie", weakness = "Surcharge", power = 50,
            influence = 500, flags = setOf("POWER_REVEALED", "FORMATIVE_DECADE_COMPLETE", "ALIAS_CHOSEN")
        )
        val start = GameEngine.event(c)
        assertEquals("MAJOR", start.kind)
        c = GameEngine.resolve(c, start, start.choices[0]).campaign.copy(influence = 0)
        var foundContinuation = false
        repeat(12) {
            val e = GameEngine.event(c)
            if (e.threadId == start.threadId && e.threadStage >= 2) foundContinuation = true
            c = GameEngine.resolve(c, e, e.choices[0]).campaign
        }
        assertTrue(foundContinuation)
    }

    @Test fun longCareerNeverStarves() {
        var c = GameEngine.newCampaign(12345L)
        repeat(10) { i ->
            val e = GameEngine.event(c)
            c = GameEngine.resolve(c, e, e.choices[i % e.choices.size]).campaign
        }
        var e = GameEngine.event(c)
        c = GameEngine.resolve(c, e, e.choices[1]).campaign
        c = GameEngine.setAlias(c, "Vector")
        repeat(5) { i ->
            e = GameEngine.event(c)
            c = GameEngine.resolve(c, e, e.choices[i % e.choices.size]).campaign
        }
        val seen = linkedSetOf<String>()
        repeat(120) { i ->
            if (!c.finished) {
                e = GameEngine.event(c)
                assertTrue(e.choices.isNotEmpty())
                seen += e.id
                c = GameEngine.resolve(c, e, e.choices[i % e.choices.size]).campaign
            }
        }
        assertTrue(seen.size > 35)
        assertTrue(c.timeline.size > 100)
    }
}
