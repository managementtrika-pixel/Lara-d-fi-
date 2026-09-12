package com.metahumanlegacy.game

import org.junit.Assert.*
import org.junit.Test

class DeepLifeSimulationTest {
    @Test
    fun bootstrapCreatesPeopleWithInnerLives() {
        val c = GameEngine.newCampaign(42L)
        val draft = UltimateCatalog.randomDraft(42L, CharacterBlueprint(
            "Mina", "Vale", "elle", "Vesper", "Centre", "Classe moyenne",
            "Protéger mes proches", "Études scientifiques", "Prudent"
        ))
        val u = UltimateStore.create(c, draft)
        val d = DeepLifeDirector.bootstrap(c, u)
        assertTrue(d.relationships.size >= 5)
        assertTrue(d.relationships.any { it.core.values.isNotEmpty() && it.core.fears.isNotEmpty() })
        assertTrue(d.relationships.any { it.memories.isNotEmpty() })
        assertTrue(d.personality.isNotEmpty())
    }

    @Test
    fun aChoiceCreatesMemoryPersonalityAndPerception() {
        val c = GameEngine.newCampaign(100L)
        val u = UltimateStore.create(c, UltimateCatalog.randomDraft(100L, GameEngine.randomBlueprint(100L)))
        val d = DeepLifeDirector.bootstrap(c, u)
        val event = EventNode("T", "Quelqu'un a besoin de toi", "", emptyList(), "FAMILLE", "", 3)
        val choice = Choice("Rester", moral = 2, opinion = 2, approach = "CARE", relationDelta = 3, impact = 2, risk = 1)
        val after = c.copy(turn = 1, opinion = 2, morality = 2)
        val next = DeepLifeDirector.afterChoice(c, after, event, choice, d)
        assertEquals(1, next.memories.size)
        assertTrue((next.personality["EMPATHIQUE"] ?: 0) > (d.personality["EMPATHIQUE"] ?: 0))
        assertTrue(next.perception.civilians > d.perception.civilians)
        assertTrue(next.drama.tension >= d.drama.tension)
    }

    @Test
    fun powersMapToDifferentMechanicalArchitectures() {
        assertEquals(PowerArchitecture.MENTAL, DeepLifeDirector.architectureFor("Télépathie"))
        assertEquals(PowerArchitecture.TECH, DeepLifeDirector.architectureFor("Drones liés"))
        assertEquals(PowerArchitecture.BODY, DeepLifeDirector.architectureFor("Force"))
        assertEquals(PowerArchitecture.MOBILITY, DeepLifeDirector.architectureFor("Vitesse"))
        assertEquals(PowerArchitecture.OCCULT, DeepLifeDirector.architectureFor("Magie symbolique"))
        assertEquals(PowerArchitecture.COSMIC, DeepLifeDirector.architectureFor("Énergie cosmique"))
        assertEquals(PowerArchitecture.MATTER, DeepLifeDirector.architectureFor("Cristal"))
        assertEquals(PowerArchitecture.ADAPTIVE, DeepLifeDirector.architectureFor("Résistance adaptative"))
        assertEquals(PowerArchitecture.PROJECTOR, DeepLifeDirector.architectureFor("Électricité"))
    }

    @Test
    fun socialReachIsNotRawInfluenceAnymore() {
        val c = GameEngine.newCampaign(7L).copy(
            flags = setOf("POWER_REVEALED"), powerFamily = "Force",
            influence = 40, prestige = 120, opinion = 80, mediaStanding = 60, governmentStanding = 50
        )
        assertTrue(c.activeReach > c.influence)
        assertTrue(c.scope >= Scope.DISTRICT)
    }

    @Test
    fun powerDirectorAddsArchitectureSpecificChoice() {
        val base = EventNode("CR", "Crise", "Une crise majeure.", listOf(Choice("Attendre")), "CRISE", "", 4)
        val mental = GameEngine.newCampaign(8L).copy(flags = setOf("POWER_REVEALED"), powerFamily = "Télépathie")
        val body = mental.copy(powerFamily = "Force")
        val mentalEvent = PowerGameplayDirector.enrich(mental, base)
        val bodyEvent = PowerGameplayDirector.enrich(body, base)
        assertTrue(mentalEvent.choices.any { it.label.contains("mentale") })
        assertTrue(bodyEvent.choices.any { it.label.contains("Encaisser") })
        assertNotEquals(mentalEvent.choices.last().label, bodyEvent.choices.last().label)
    }

    @Test
    fun criticalFailureCanBecomePersistentLifeHistoryOnce() {
        val before = GameEngine.newCampaign(9L).copy(
            turn = 20, flags = setOf("POWER_REVEALED"), powerFamily = "Force", health = 20
        )
        val u = UltimateStore.create(before, UltimateCatalog.randomDraft(9L, GameEngine.randomBlueprint(9L)))
        val d = DeepLifeDirector.bootstrap(before, u)
        val dead = before.copy(health = 0, turn = 21)
        val event = EventNode("LETHAL", "Le viaduc", "", emptyList(), "CRISE", "", 5)
        val choice = Choice("Tenir", risk = 9, approach = "CARE")
        val update = GenerationalDirector.afterChoice(dead, u, d, event, choice)
        assertTrue(update.campaign.health > 0)
        assertTrue("V2_CRITICAL_SURVIVAL" in update.campaign.flags)
        assertTrue(update.deep.injuries.any { it.chronic && it.severity == 10 })
        assertTrue(update.ultimate.debt > u.debt)
    }

    @Test
    fun twoHundredDeterministicLivesKeepV2StateBounded() {
        repeat(200) { index ->
            var c = GameEngine.newCampaign(10_000L + index)
            val u = UltimateStore.create(c, UltimateCatalog.randomDraft(c.seed, GameEngine.randomBlueprint(c.seed)))
            var d = DeepLifeDirector.bootstrap(c, u)
            repeat(18) {
                if (c.finished) return@repeat
                val event = GameEngine.event(c)
                val choice = event.choices.firstOrNull() ?: return@repeat
                val resolved = GameEngine.resolve(c, event, choice)
                val update = DeepLifeRuntime.afterChoice(c, resolved.campaign, event, choice, d)
                c = resolved.campaign
                d = update.state
                assertTrue(d.memories.size <= 120)
                assertTrue(d.opportunities.size <= 10)
                assertTrue(d.deferred.size <= 48)
                assertTrue(d.drama.tension in 0..100)
                assertTrue(d.drama.recoveryNeed in 0..100)
            }
        }
    }
}
