package com.metahumanlegacy.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeSimulationDirectorTest {
    private fun campaign(age: Int = 22, power: Boolean = true) = Campaign(seed = 42L, age = age, powerRevealed = power)

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

    @Test fun patrolChangesTheTargetDistrict() {
        val c = campaign()
        val state = LifeSimulationDirector.bootstrap(c, DeepLifeState(seed = c.seed))
        val before = state.districts.first { it.id == "quartier" }
        val result = LifeSimulationDirector.perform(c, state, LifeAction(LifeActionType.PATROL, "quartier", "Patrouiller"))
        val after = result.state.districts.first { it.id == "quartier" }
        assertTrue(after.safety > before.safety)
        assertTrue(after.criminalControl < before.criminalControl)
    }

    @Test fun revealingIdentityPersistsForThatPerson() {
        val c = campaign()
        val deep = DeepLifeState(seed = c.seed, relationships = listOf(DeepRelationship("ami", "Ami", "ami")))
        val state = LifeSimulationDirector.bootstrap(c, deep)
        val result = LifeSimulationDirector.perform(c, state, LifeAction(LifeActionType.REVEAL_IDENTITY, "ami", "Révéler"))
        assertEquals(SecretKnowledge.KNOWS, result.state.relationshipLives.first().secretKnowledge)
    }

    @Test fun noFreeActionsAfterCalendarIsFull() {
        val c = campaign()
        val state = LifeSimulationDirector.bootstrap(c, DeepLifeState(seed = c.seed)).copy(civil = CivilLifeState(freeMoments = 0))
        val result = LifeSimulationDirector.perform(c, state, LifeAction(LifeActionType.WORK, label = "Travailler"))
        assertEquals(0, result.state.civil.freeMoments)
        assertEquals("Plus de temps", result.headline)
    }
}
