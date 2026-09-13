package com.metahumanlegacy.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityNarrativeDirectorTest {
    private fun campaign(exposure: Int = 58) = GameEngine.newCampaign(737373L).copy(
        turn = 28,
        identityExposure = exposure,
        flags = setOf("POWER_REVEALED", "ALIAS_CHOSEN")
    )

    private fun event(kind: String = "MAJOR", stakes: Int = 4) = EventNode(
        id = "identity_crisis",
        title = "Des témoins filment l'intervention",
        text = "Le quartier est bouclé et les téléphones sont déjà levés.",
        choices = listOf(Choice("Intervenir immédiatement", power = 2, risk = 4, approach = "CARE")),
        category = "MEDIA_CRISIS",
        provocation = "Agir",
        stakes = stakes,
        kind = kind
    )

    private fun deep(threat: Boolean = false, evidenceCount: Int = 2, lifeExposure: Int = 58): DeepLifeState {
        val knowledge = if (threat) SecretKnowledge.THREATENS else SecretKnowledge.UNAWARE
        return DeepLifeState(
            seed = 737373L,
            relationships = listOf(
                DeepRelationship(
                    id = "journalist",
                    name = "Nia",
                    role = "Journaliste",
                    trust = if (threat) 15 else 55,
                    grudge = if (threat) 70 else 0,
                    knowsIdentity = threat
                )
            ),
            identityEvidence = (0 until evidenceCount).map { index ->
                IdentityEvidence("ev$index", if (index == 0) "VIDÉO" else "TÉMOIGNAGE", 45, "journalist", 20 + index, "Indice $index")
            },
            lifeSimulation = LifeSimulationState(
                relationshipLives = listOf(RelationshipLifeState("journalist", closeness = 40, secretKnowledge = knowledge)),
                districts = listOf(DistrictLifeState("quartier")),
                secretIdentity = IdentitySecretState(exposure = lifeExposure)
            )
        )
    }

    @Test
    fun highIdentityPressureAddsARealStoryChoice() {
        val enriched = IdentityNarrativeDirector.enrich(campaign(), deep(), event())
        val choice = enriched.choices.first { IdentityNarrativeDirector.isIdentityChoice(it) }

        assertTrue(choice.identityDelta < 0)
        assertTrue(choice.power < 0)
        assertEquals("TRUTH", choice.approach)
        assertTrue(enriched.text.contains("identité civile", ignoreCase = true))
    }

    @Test
    fun identityStoryChoiceReducesAuthoritativeExposure() {
        val before = campaign(58)
        val enriched = IdentityNarrativeDirector.enrich(before, deep(), event())
        val choice = enriched.choices.first { IdentityNarrativeDirector.isIdentityChoice(it) }
        val resolved = GameEngine.resolve(before, enriched, choice)

        assertTrue(resolved.campaign.identityExposure < before.identityExposure)
        assertTrue(resolved.outcome.contains("brouilles", ignoreCase = true) || resolved.outcome.contains("pistes", ignoreCase = true))
    }

    @Test
    fun threateningKnowerMakesIdentityChoiceRiskierAndDeferred() {
        val enriched = IdentityNarrativeDirector.enrich(campaign(), deep(threat = true), event())
        val choice = enriched.choices.first { IdentityNarrativeDirector.isIdentityChoice(it) }

        assertTrue(choice.risk >= 5)
        assertTrue(choice.deferredHook)
        assertTrue(choice.label.contains("parle", ignoreCase = true))
    }

    @Test
    fun lowPressureDoesNotInventIdentityChoice() {
        val enriched = IdentityNarrativeDirector.enrich(campaign(12), deep(evidenceCount = 0, lifeExposure = 12), event())
        assertFalse(enriched.choices.any { IdentityNarrativeDirector.isIdentityChoice(it) })
    }

    @Test
    fun formativeAndAwakeningRemainIsolated() {
        val c = campaign()
        val d = deep()
        assertEquals(event("FORMATIVE"), IdentityNarrativeDirector.enrich(c, d, event("FORMATIVE")))
        assertEquals(event("AWAKENING"), IdentityNarrativeDirector.enrich(c, d, event("AWAKENING")))
    }

    @Test
    fun minorSceneDoesNotGetIdentityManagementChoice() {
        val enriched = IdentityNarrativeDirector.enrich(campaign(), deep(), event(stakes = 2))
        assertFalse(enriched.choices.any { IdentityNarrativeDirector.isIdentityChoice(it) })
    }

    @Test
    fun identityResponseSurvivesSevenChoiceCap() {
        val crowded = event().copy(choices = (1..7).map { Choice("Choix $it", risk = it) })
        val enriched = IdentityNarrativeDirector.enrich(campaign(), deep(), crowded)
        assertEquals(7, enriched.choices.size)
        assertTrue(enriched.choices.any { IdentityNarrativeDirector.isIdentityChoice(it) })
    }

    @Test
    fun identityPressureDoesNotEvictCareerScaleResponse() {
        val scope = Choice("Coordonner à l'échelle nationale", flag = "scope_response_country")
        val crowded = event().copy(choices = (1..6).map { Choice("Choix $it") } + scope)
        val enriched = IdentityNarrativeDirector.enrich(campaign(), deep(), crowded)

        assertEquals(7, enriched.choices.size)
        assertTrue(enriched.choices.any { it.flag == "scope_response_country" })
        assertTrue(enriched.choices.any { IdentityNarrativeDirector.isIdentityChoice(it) })
    }
}
