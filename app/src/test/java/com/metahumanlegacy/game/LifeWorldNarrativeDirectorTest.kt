package com.metahumanlegacy.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeWorldNarrativeDirectorTest {
    private fun campaign(): Campaign = GameEngine.newCampaign(737373L).copy(
        turn = 25,
        powerFamily = "Énergie",
        control = 55,
        flags = setOf("POWER_REVEALED", "ALIAS_CHOSEN")
    )

    private fun event(kind: String = "MAJOR") = EventNode(
        id = "district_memory_event",
        title = "Le quartier retient son souffle",
        text = "Une nouvelle crise éclate dans les rues que tu connais.",
        choices = listOf(Choice("Intervenir", moral = 1, risk = 4, approach = "CARE")),
        category = "CRISE",
        provocation = "Réagir",
        stakes = 4,
        kind = kind
    )

    private fun deep(district: DistrictLifeState): DeepLifeState = DeepLifeState(
        seed = 737373L,
        lifeSimulation = LifeSimulationState(districts = listOf(district))
    )

    @Test
    fun neutralDistrictLeavesStoryUntouched() {
        val c = campaign()
        val base = event()
        val state = deep(DistrictLifeState(id = "quartier"))

        assertEquals(base, LifeWorldNarrativeDirector.enrich(c, state, base))
    }

    @Test
    fun strongLocalTrustIsRememberedAndUnlocksCommunityChoice() {
        val c = campaign()
        val enriched = LifeWorldNarrativeDirector.enrich(
            c,
            deep(DistrictLifeState(id = "quartier", safety = 78, localTrust = 60, criminalControl = 14)),
            event()
        )

        assertTrue(enriched.text.contains("MÉMOIRE DU QUARTIER"))
        assertTrue(enriched.text.contains("confiance locale", ignoreCase = true))
        assertTrue(enriched.choices.any { it.flag == "life_world_local_trust" })
    }

    @Test
    fun entrenchedCrimeCreatesPersistentNetworkResponse() {
        val c = campaign()
        val enriched = LifeWorldNarrativeDirector.enrich(
            c,
            deep(DistrictLifeState(id = "quartier", safety = 30, criminalControl = 68, localTrust = 8, mediaHeat = 55)),
            event()
        )

        assertTrue(enriched.text.contains("réseaux criminels", ignoreCase = true))
        assertTrue(enriched.text.contains("médias", ignoreCase = true))
        val response = enriched.choices.first { it.flag == "life_world_criminal_network" }
        assertTrue(response.deferredHook)
        assertTrue(response.identityDelta > 0)
    }

    @Test
    fun formativeAndAwakeningNeverReadFutureDistrictMemory() {
        val c = campaign()
        val state = deep(DistrictLifeState(id = "quartier", safety = 90, localTrust = 90, criminalControl = 80, mediaHeat = 90))

        assertEquals(event("FORMATIVE"), LifeWorldNarrativeDirector.enrich(c, state, event("FORMATIVE")))
        assertEquals(event("AWAKENING"), LifeWorldNarrativeDirector.enrich(c, state, event("AWAKENING")))
    }

    @Test
    fun noDistrictStateCannotInventWorldMemory() {
        val c = campaign()
        val base = event()
        val state = DeepLifeState(seed = c.seed, lifeSimulation = LifeSimulationState())
        val enriched = LifeWorldNarrativeDirector.enrich(c, state, base)

        assertEquals(base, enriched)
        assertFalse(enriched.choices.any { it.flag?.startsWith("life_world_") == true })
    }
}
