package com.metahumanlegacy.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedTimeBudgetTest {
    private fun campaign(ageTurn: Int): Campaign = GameEngine.newCampaign(808080L).copy(
        turn = ageTurn,
        powerFamily = "Énergie",
        flags = setOf("POWER_REVEALED", "ALIAS_CHOSEN")
    )

    @Test
    fun lifeSimulationUsesSameAnnualBudgetAtEveryAge() {
        listOf(0, 5, 10, 16, 60, 200).forEach { turn ->
            val c = campaign(turn)
            val deep = DeepLifeState(seed = c.seed)
            val life = LifeSimulationDirector.bootstrap(c, deep)
            assertEquals("turn=$turn age=${c.age}", ANNUAL_ACTION_LIMIT, life.civil.freeMoments)
        }
    }

    @Test
    fun exactlyThreeLifeActionsConsumeTheWholeYearBudget() {
        val c = campaign(20)
        var state = LifeSimulationDirector.bootstrap(c, DeepLifeState(seed = c.seed))
        repeat(ANNUAL_ACTION_LIMIT) {
            state = LifeSimulationDirector.perform(c, state, LifeAction(LifeActionType.REST, label = "Récupérer")).state
        }
        assertEquals(0, state.civil.freeMoments)
    }

    @Test
    fun fourthLifeActionIsRejectedWithoutMutatingState() {
        val c = campaign(20)
        var state = LifeSimulationDirector.bootstrap(c, DeepLifeState(seed = c.seed))
        repeat(ANNUAL_ACTION_LIMIT) {
            state = LifeSimulationDirector.perform(c, state, LifeAction(LifeActionType.REST, label = "Récupérer")).state
        }
        val blocked = LifeSimulationDirector.perform(c, state, LifeAction(LifeActionType.WORK, label = "Travailler"))
        assertSame(state, blocked.state)
        assertEquals("Plus de temps", blocked.headline)
        assertTrue(blocked.detail.contains("année"))
    }

    @Test
    fun newYearRestoresExactlyThreeMoments() {
        val before = campaign(20)
        val deep = DeepLifeState(seed = before.seed)
        var state = LifeSimulationDirector.bootstrap(before, deep)
        state = LifeSimulationDirector.perform(before, state, LifeAction(LifeActionType.REST, label = "Récupérer")).state
        assertEquals(ANNUAL_ACTION_LIMIT - 1, state.civil.freeMoments)

        val later = before.copy(turn = before.turn + 4)
        val synced = LifeSimulationDirector.synced(later, deep, state)
        assertEquals(ANNUAL_ACTION_LIMIT, synced.civil.freeMoments)
    }
}
