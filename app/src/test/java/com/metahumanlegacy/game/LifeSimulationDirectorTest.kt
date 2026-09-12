package com.metahumanlegacy.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeSimulationDirectorTest {
    private fun campaign(age: Int = 22, power: Boolean = true): Campaign {
        val turn = when {
            age <= 17 -> (age - 8).coerceIn(0, 9)
            age == 18 -> 10
            age == 19 -> 12
            age == 20 -> 14
            else -> 16 + (age - 20) * 4
        }
        return Campaign(
            seed = 42L,
            name = "Test Vesper",
            modifier = "STANDARD",
            turn = turn,
            flags = if (power) setOf("POWER_REVEALED") else emptySet()
        )
    }

    @Test fun actionsConsumeRealTime() {
        val c = campaign()
        val state = LifeSimulationDirector.bootstrap(c, DeepLifeState(seed = c.seed))
        val result = LifeSimulationDirector.perform(c, state, LifeAction(LifeActionType.REST, label = "Récupérer"))
        assertEquals(state.civil.freeMoments - 1, result.state.civil.freeMoments)
    }

    @Test fun powerTrainingCreatesProgressAndFatigue() {
        val c = campaign()
        val state = LifeSimulationDirector.bootstrap(c, DeepLifeState(seed = c.seed))
        val result = LifeSimulationDirector.perform(c, state, LifeAction(LifeActionType.TRAIN_POWER, label = "Entraîner"))
        assertTrue(result.state.powerRules.control > state.powerRules.control)
        assertTrue(result.state.powerRules.fatigue > state.powerRules.fatigue)
    }

    @Test fun overloadBlocksTrainingWithoutSpendingTime() {
        val c = campaign()
        val state = LifeSimulationDirector.bootstrap(c, DeepLifeState(seed = c.seed)).copy(
            powerRules = PowerRulesState(overload = 95),
            civil = CivilLifeState(freeMoments = 2)
        )
        val result = LifeSimulationDirector.perform(c, state, LifeAction(LifeActionType.TRAIN_POWER, label = "Entraîner"))
        assertEquals("Corps en surcharge", result.headline)
        assertEquals(2, result.state.civil.freeMoments)
    }

    @Test fun patrolChangesTheTargetDistrict() {
        val c = campaign()
        val state = LifeSimulationDirector.bootstrap(c, DeepLifeState(seed = c.seed))
        val before = state.districts.first { it.id == "quartier" }
        val result = LifeSimulationDirector.perform(c, state, LifeAction(LifeActionType.PATROL, "quartier", "Patrouiller"))
        val after = result.state.districts.first { it.id == "quartier" }
        assertTrue(after.safety > before.safety)
        assertTrue(after.criminalControl < before.criminalControl)
        assertTrue(result.state.secretIdentity.exposure > state.secretIdentity.exposure)
    }

    @Test fun revealingIdentityPersistsForThatPerson() {
        val c = campaign()
        val deep = DeepLifeState(seed = c.seed, relationships = listOf(DeepRelationship("ami", "Ami", "ami")))
        val state = LifeSimulationDirector.bootstrap(c, deep)
        val result = LifeSimulationDirector.perform(c, state, LifeAction(LifeActionType.REVEAL_IDENTITY, "ami", "Révéler"))
        assertEquals(SecretKnowledge.KNOWS, result.state.relationshipLives.first().secretKnowledge)
        assertTrue(LifeSimulationDirector.mergedIntoDeep(deep, result.state).relationships.first().knowsIdentity)
    }

    @Test fun noFreeActionsAfterCalendarIsFull() {
        val c = campaign()
        val state = LifeSimulationDirector.bootstrap(c, DeepLifeState(seed = c.seed)).copy(civil = CivilLifeState(freeMoments = 0))
        val result = LifeSimulationDirector.perform(c, state, LifeAction(LifeActionType.WORK, label = "Travailler"))
        assertEquals(0, result.state.civil.freeMoments)
        assertEquals("Plus de temps", result.headline)
    }

    @Test fun bootstrapUsesCanonicalCampaignAge() {
        val c = campaign(age = 37)
        val state = LifeSimulationDirector.bootstrap(c, DeepLifeState(seed = c.seed))
        assertEquals(37, c.age)
        assertEquals(37, state.calendarYear)
    }

    @Test fun yearlyMomentsResetOnlyWhenAgeChanges() {
        val age22 = campaign(age = 22)
        val deep = DeepLifeState(seed = age22.seed)
        val exhausted = LifeSimulationDirector.bootstrap(age22, deep).copy(civil = CivilLifeState(freeMoments = 0))
        val sameYearLaterTurn = age22.copy(turn = age22.turn + 1)
        assertEquals(22, sameYearLaterTurn.age)
        assertEquals(0, LifeSimulationDirector.synced(sameYearLaterTurn, deep, exhausted).civil.freeMoments)
        val age23 = campaign(age = 23)
        assertTrue(LifeSimulationDirector.synced(age23, deep, exhausted).civil.freeMoments > 0)
    }
}
