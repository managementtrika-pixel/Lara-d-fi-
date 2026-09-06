package com.metahumanlegacy.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReleaseReadinessTest {
    @Before
    fun installNarrativeBundle() {
        NarrativeCodec.installAssetParts { path -> File("src/main/assets/" + path).readBytes() }
    }

    @Test
    fun authoredNarrativeGraphIsCompleteAndInternallyConsistent() {
        val prologue = NarrativeCodec.prologue()
        val awakening = NarrativeCodec.awakening()
        val foundation = NarrativeCodec.foundation()
        val beats = NarrativeCodec.beats()
        val endings = NarrativeCodec.endings()

        assertEquals(10, prologue.size)
        assertEquals((8..17).toList(), prologue.map { it.age })
        assertEquals(18, awakening.age)
        assertEquals(5, foundation.size)
        assertEquals(250, beats.size)
        assertEquals(50, beats.map { it.arc }.toSet().size)
        assertEquals(1000, beats.sumOf { it.choices.size })
        assertEquals(200, endings.values.sumOf { it.size })

        val allIds = prologue.map { it.id } + awakening.id + foundation.map { it.id } + beats.map { it.id }
        assertEquals("Every authored node id must be unique", allIds.size, allIds.toSet().size)

        val expectedRoutes = setOf("CARE", "ORDER", "TRUTH", "ASCEND")
        beats.groupBy { it.arc }.forEach { entry ->
            val arc = entry.key
            val arcBeats = entry.value
            assertEquals(arc + " must contain five authored stages", setOf(1, 2, 3, 4, 5), arcBeats.map { it.stage }.toSet())
            assertTrue(arc + " must have four endings", endings[arc]?.keys == expectedRoutes)
            arcBeats.forEach { beat ->
                assertTrue(beat.id + " age window must be valid", beat.minAge >= 18 && beat.maxAge >= beat.minAge)
                assertEquals(beat.id + " must expose all four route families", expectedRoutes, beat.choices.map { it.approach }.toSet())
                assertEquals(beat.id + " must have four decisions", 4, beat.choices.size)
            }
        }

        assertEquals(1064, GameEngine.constatCount())
    }

    @Test
    fun childhoodExactlySpansEightToSeventeenAndAwakensAtEighteen() {
        var c = GameEngine.newCampaign(801718L)
        repeat(10) { index ->
            assertEquals(8 + index, c.age)
            assertFalse(c.powerRevealed)
            val event = GameEngine.event(c)
            assertEquals("FORMATIVE", event.kind)
            assertEquals(index + 1, event.threadStage)
            c = GameEngine.resolve(c, event, event.choices[index % event.choices.size]).campaign
        }

        assertEquals(18, c.age)
        assertTrue(c.powerResolved)
        assertFalse(c.powerRevealed)

        val awakening = GameEngine.event(c)
        assertEquals("AWAKENING", awakening.kind)
        c = GameEngine.resolve(c, awakening, awakening.choices.first()).campaign

        assertEquals(18, c.age)
        assertTrue(c.powerRevealed)
        assertTrue(c.needsAlias)
    }

    @Test
    fun contrastingRoutesSurviveLongDeterministicLivesWithoutDeadlock() {
        val routes = listOf("CARE", "ORDER", "TRUTH", "ASCEND")
        var completedRuns = 0
        val powers = linkedSetOf<String>()

        routes.forEachIndexed { routeIndex, route ->
            repeat(6) { seedIndex ->
                var c = GameEngine.newCampaign(10_000L + routeIndex * 1_000L + seedIndex)
                var guard = 0
                var previousTurn = -1
                var previousAge = 7

                while (!c.finished && guard < 230) {
                    assertTrue("turn must never go backwards", c.turn >= previousTurn)
                    assertTrue("age must never go backwards", c.age >= previousAge)
                    assertStateBounds(c)

                    val event = GameEngine.event(c)
                    assertTrue("event " + event.id + " must always have a decision", event.choices.isNotEmpty())

                    val choice = when {
                        event.kind == "FORMATIVE" -> event.choices[(guard + seedIndex + routeIndex) % event.choices.size]
                        else -> event.choices.firstOrNull { it.approach == route }
                            ?: event.choices[(guard + routeIndex) % event.choices.size]
                    }

                    previousTurn = c.turn
                    previousAge = c.age
                    c = GameEngine.resolve(c, event, choice).campaign

                    if (c.needsAlias) c = GameEngine.setAlias(c, "Legacy-" + (routeIndex + 1) + "-" + seedIndex)
                    guard++
                }

                assertTrue("simulation must terminate by the safety guard", c.finished)
                assertStateBounds(c)
                assertTrue(c.timeline.size <= 180)
                if (c.health > 0) assertTrue(c.turn >= 196)
                if (c.powerResolved) powers += c.powerFamily
                completedRuns++
            }
        }

        assertEquals(24, completedRuns)
        assertTrue("the power resolver should produce varied destinies", powers.size >= 6)
    }

    @Test
    fun scopeThresholdsRemainOrderedAndReachWorld() {
        val base = GameEngine.newCampaign(777L).copy(
            powerFamily = "Énergie",
            flags = setOf("POWER_REVEALED", "ALIAS_CHOSEN")
        )
        assertEquals(Scope.STREET, base.copy(influence = 0).scope)
        assertEquals(Scope.DISTRICT, base.copy(influence = 75).scope)
        assertEquals(Scope.CITY, base.copy(influence = 180).scope)
        assertEquals(Scope.REGION, base.copy(influence = 340).scope)
        assertEquals(Scope.COUNTRY, base.copy(influence = 560).scope)
        assertEquals(Scope.WORLD, base.copy(influence = 900).scope)
    }

    private fun assertStateBounds(c: Campaign) {
        assertTrue(c.morality in -100..100)
        assertTrue(c.opinion in -100..100)
        assertTrue(c.fear in 0..100)
        assertTrue(c.power in 0..100)
        assertTrue(c.control in 0..100)
        assertTrue(c.health in 0..100)
        assertTrue(c.identityExposure in 0..100)
        assertTrue(c.familyBond in 0..100)
        assertTrue(c.scope in Scope.entries)
    }
}
