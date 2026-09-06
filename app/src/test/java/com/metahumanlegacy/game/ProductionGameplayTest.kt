package com.metahumanlegacy.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProductionGameplayTest {
    @Before
    fun installNarrativeBundle() {
        NarrativeCodec.installAssetParts { path -> File("src/main/assets/$path").readBytes() }
    }

    private fun formativeLife(seed: Long, pattern: IntArray): Campaign {
        var c = GameEngine.newCampaign(seed)
        repeat(10) { turn ->
            assertEquals(8 + turn, c.age)
            val event = GameEngine.event(c)
            assertEquals("FORMATIVE", event.kind)
            assertFalse(c.powerRevealed)
            assertEquals("Non révélé", c.powerFamily)
            val choice = event.choices[pattern[turn % pattern.size] % event.choices.size]
            val resolved = GameEngine.resolve(c, event, choice)
            assertTrue(resolved.outcome.isNotBlank())
            c = resolved.campaign
        }
        assertEquals(18, c.age)
        assertTrue(c.powerResolved)
        assertFalse(c.powerRevealed)
        assertEquals("AWAKENING", GameEngine.event(c).kind)
        return c
    }

    @Test
    fun fourContrastingLivesStayPlayableAndReachCorrectAwakening() {
        val lives = listOf(
            formativeLife(202601L, intArrayOf(0, 0, 1, 0)),
            formativeLife(202602L, intArrayOf(1, 2, 1, 2)),
            formativeLife(202603L, intArrayOf(2, 1, 3, 1)),
            formativeLife(202604L, intArrayOf(3, 3, 2, 3))
        )

        lives.forEachIndexed { index, beforeAwakening ->
            val awakening = GameEngine.event(beforeAwakening)
            assertEquals("AWAKENING", awakening.kind)
            val chosen = awakening.choices[index % awakening.choices.size]
            val after = GameEngine.resolve(beforeAwakening, awakening, chosen).campaign
            assertTrue(after.powerRevealed)
            assertTrue(after.needsAlias)
            assertTrue(after.powerFamily.isNotBlank())
            assertTrue(after.weakness.isNotBlank())
            assertNotNull(powerVisualProfile(after.powerFamily))
        }

        val formationFingerprints = lives.map {
            Triple(it.affinityScores, it.expressionScores, it.costScores)
        }.toSet()
        assertTrue("Contrasting lives must not collapse to one formation vector", formationFingerprints.size >= 2)
    }

    @Test
    fun powerVisualProfileIsStableForSameResolvedPower() {
        val c = formativeLife(424242L, intArrayOf(0, 2, 1, 3, 2))
        val a = powerVisualProfile(c.powerFamily)
        val b = powerVisualProfile(c.powerFamily)
        assertEquals(a, b)
    }
}
