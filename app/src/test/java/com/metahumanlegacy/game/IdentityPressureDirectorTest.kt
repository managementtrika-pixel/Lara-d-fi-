package com.metahumanlegacy.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityPressureDirectorTest {
    private fun campaign(exposure: Int = 50) = GameEngine.newCampaign(919191L).copy(
        turn = 28,
        identityExposure = exposure,
        flags = setOf("POWER_REVEALED", "ALIAS_CHOSEN")
    )

    private fun deep(
        knows: Boolean = false,
        trust: Int = 50,
        affection: Int = 50,
        grudge: Int = 0,
        evidence: List<IdentityEvidence> = emptyList()
    ) = DeepLifeState(
        seed = 919191L,
        relationships = listOf(
            DeepRelationship(
                id = "journalist",
                name = "Nia",
                role = "Journaliste",
                trust = trust,
                affection = affection,
                grudge = grudge,
                knowsIdentity = knows
            )
        ),
        identityEvidence = evidence
    )

    private fun state(knowledge: SecretKnowledge = SecretKnowledge.UNAWARE) = LifeSimulationState(
        relationshipLives = listOf(RelationshipLifeState("journalist", closeness = 55, secretKnowledge = knowledge)),
        districts = listOf(DistrictLifeState("quartier", mediaHeat = 5)),
        secretIdentity = IdentitySecretState(exposure = 10)
    )

    @Test
    fun accumulatedEvidenceCreatesPersistentRumors() {
        val c = campaign(67)
        val d = deep(evidence = listOf(
            IdentityEvidence("video", "VIDÉO", 70, "journalist", 20, "Une vidéo exploitable"),
            IdentityEvidence("power", "SIGNATURE_POUVOIR", 65, "journalist", 24, "Une signature comparable")
        ))

        val synced = IdentityPressureDirector.sync(c, d, state())

        assertEquals(setOf("video", "power"), synced.secretIdentity.evidenceIds.toSet())
        assertTrue(synced.secretIdentity.activeRumors.size >= 3)
        assertTrue(synced.districts.first().mediaHeat >= 33)
    }

    @Test
    fun trustedPersonWhoKnowsBecomesProtector() {
        val synced = IdentityPressureDirector.sync(
            campaign(),
            deep(knows = true, trust = 82, affection = 76),
            state()
        )

        assertEquals(SecretKnowledge.PROTECTS, synced.relationshipLives.first().secretKnowledge)
        assertEquals(SecretKnowledge.PROTECTS, synced.secretIdentity.knownBy["journalist"])
    }

    @Test
    fun hostilePersonWhoKnowsBecomesThreat() {
        val synced = IdentityPressureDirector.sync(
            campaign(),
            deep(knows = true, trust = 20, grudge = 70),
            state()
        )

        assertEquals(SecretKnowledge.THREATENS, synced.relationshipLives.first().secretKnowledge)
        assertEquals(SecretKnowledge.THREATENS, synced.secretIdentity.knownBy["journalist"])
    }

    @Test
    fun protectorAndThreatStatesAreNeverDowngradedToKnows() {
        val protector = IdentityPressureDirector.sync(
            campaign(), deep(knows = true, trust = 40), state(SecretKnowledge.PROTECTS)
        )
        val threat = IdentityPressureDirector.sync(
            campaign(), deep(knows = true, trust = 80, affection = 80), state(SecretKnowledge.THREATENS)
        )

        assertEquals(SecretKnowledge.PROTECTS, protector.relationshipLives.first().secretKnowledge)
        assertEquals(SecretKnowledge.THREATENS, threat.relationshipLives.first().secretKnowledge)
    }

    @Test
    fun persistedStrongKnowledgeRestoresAfterGenericLifeSync() {
        val afterGenericSync = state(SecretKnowledge.KNOWS).copy(
            secretIdentity = IdentitySecretState(
                exposure = 35,
                knownBy = mapOf("journalist" to SecretKnowledge.PROTECTS)
            )
        )
        val restored = IdentityPressureDirector.sync(
            campaign(),
            deep(knows = true, trust = 40),
            afterGenericSync
        )

        assertEquals(SecretKnowledge.PROTECTS, restored.relationshipLives.first().secretKnowledge)
        assertEquals(SecretKnowledge.PROTECTS, restored.secretIdentity.knownBy["journalist"])
    }

    @Test
    fun journalistKnowledgeCreatesCriticalRumorEvenBelowExposureThreshold() {
        val synced = IdentityPressureDirector.sync(
            campaign(35),
            deep(knows = true, trust = 60),
            state()
        )

        assertTrue(synced.secretIdentity.activeRumors.any { it.contains("noms circulent", ignoreCase = true) })
        assertEquals(1, IdentityPressureDirector.knownCount(synced))
    }

    @Test
    fun identityPressureDoesNothingBeforePowerReveal() {
        val c = campaign(80).copy(flags = emptySet())
        val before = state()
        assertEquals(before, IdentityPressureDirector.sync(c, deep(knows = true), before))
    }
}
