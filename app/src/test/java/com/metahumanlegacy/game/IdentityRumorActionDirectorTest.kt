package com.metahumanlegacy.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityRumorActionDirectorTest {
    private fun campaign() = GameEngine.newCampaign(515151L).copy(
        turn = 28,
        identityExposure = 58,
        flags = setOf("POWER_REVEALED", "ALIAS_CHOSEN")
    )

    private fun state(threat: Boolean = false) = LifeSimulationState(
        civil = CivilLifeState(freeMoments = 3, stress = 20),
        secretIdentity = IdentitySecretState(
            exposure = 58,
            activeRumors = listOf("Rumeur 1", "Rumeur 2", "Rumeur 3"),
            knownBy = if (threat) mapOf("journalist" to SecretKnowledge.THREATENS) else emptyMap(),
            evidenceIds = listOf("ev1", "ev2", "ev3")
        ),
        districts = listOf(DistrictLifeState("quartier", mediaHeat = 52))
    )

    @Test
    fun rumorContainmentConsumesTimeAndReducesExposureAndHeat() {
        val c = campaign()
        val before = state()
        val result = IdentityRumorActionDirector.perform(c, before, IdentityRumorActionDirector.action())
        assertNotNull(result)
        val after = result!!.state

        assertEquals(2, after.civil.freeMoments)
        assertTrue(after.civil.stress > before.civil.stress)
        assertTrue(after.secretIdentity.exposure < before.secretIdentity.exposure)
        assertTrue(after.districts.first().mediaHeat < before.districts.first().mediaHeat)
        assertTrue(after.secretIdentity.activeRumors.size < before.secretIdentity.activeRumors.size)
    }

    @Test
    fun availableActionsSurfacesRumorContainmentWhenPressureIsReal() {
        val actions = LifeSimulationDirector.availableActions(campaign(), state())
        val action = actions.firstOrNull { it.targetId == "identity_rumor" }

        assertNotNull(action)
        assertEquals(LifeActionType.INVESTIGATE, action!!.type)
        assertTrue(action.label.contains("rumeur", ignoreCase = true))
    }

    @Test
    fun containmentKeepsNormalEightyEntryLifeHistoryWindow() {
        val before = state().copy(actionLog = (1..40).map { "Action $it" })
        val after = IdentityRumorActionDirector.perform(campaign(), before, IdentityRumorActionDirector.action())!!.state

        assertEquals(41, after.actionLog.size)
        assertEquals("Action 1", after.actionLog.first())
        assertTrue(after.actionLog.last().contains("rumeurs contenues", ignoreCase = true))
    }

    @Test
    fun existingEvidenceIsNotErasedByRumorContainment() {
        val c = campaign()
        val before = state()
        val after = IdentityRumorActionDirector.perform(c, before, IdentityRumorActionDirector.action())!!.state
        assertEquals(before.secretIdentity.evidenceIds, after.secretIdentity.evidenceIds)
    }

    @Test
    fun threateningKnowerMakesContainmentLessEffectiveAndMoreStressful() {
        val c = campaign()
        val normal = IdentityRumorActionDirector.perform(c, state(false), IdentityRumorActionDirector.action())!!.state
        val threatened = IdentityRumorActionDirector.perform(c, state(true), IdentityRumorActionDirector.action())!!.state

        assertTrue(threatened.secretIdentity.exposure >= normal.secretIdentity.exposure)
        assertTrue(threatened.civil.stress > normal.civil.stress)
    }

    @Test
    fun actionIsUnavailableBeforePowerReveal() {
        val formative = campaign().copy(flags = emptySet())
        assertFalse(IdentityRumorActionDirector.available(formative, state()))
        assertFalse(LifeSimulationDirector.availableActions(formative, state()).any { IdentityRumorActionDirector.handles(it) })
    }

    @Test
    fun actionIsUnavailableWithoutPressure() {
        val quiet = LifeSimulationState(civil = CivilLifeState(freeMoments = 3))
        assertFalse(IdentityRumorActionDirector.available(campaign(), quiet))
        assertFalse(LifeSimulationDirector.availableActions(campaign(), quiet).any { IdentityRumorActionDirector.handles(it) })
    }

    @Test
    fun bridgePropagatesContainmentToAuthoritativeCampaign() {
        val c = campaign()
        val before = state()
        val actionResult = IdentityRumorActionDirector.perform(c, before, IdentityRumorActionDirector.action())!!
        val world = UltimateStore.fallback(c)
        val bridged = LifeWorldStateBridge.afterLifeAction(c, world, before, actionResult.state)

        assertTrue(bridged.campaign.identityExposure < c.identityExposure)
    }
}
