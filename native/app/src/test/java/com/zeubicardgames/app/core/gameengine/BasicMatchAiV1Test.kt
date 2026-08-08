package com.zeubicardgames.app.core.gameengine

import com.zeubicardgames.app.core.model.Attack
import com.zeubicardgames.app.core.model.CardDefinition
import com.zeubicardgames.app.core.model.CardVariant
import com.zeubicardgames.app.core.model.Rarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BasicMatchAiV1Test {
    private val playerBase = card("set:001:player", "PLAYER", 120, listOf(Attack("Frappe", 40, 1)))
    private val aiBase = card("set:002:ai", "IA", 100, listOf(Attack("Tir", 30, 1)))
    private val weakAi = card("set:003:weak", "IA faible", 30, listOf(Attack("Petit tir", 10, 1)))
    private val catalog = listOf(playerBase, aiBase, weakAi)

    @Test
    fun `ai completes its setup with active and reserves`() {
        val engine = MatchEngineV1(catalog, 101)
        val ai = BasicMatchAiV1(catalog, engine)
        var state = engine.start(List(20) { playerBase.canonicalId }, List(20) { aiBase.canonicalId })

        state = engine.apply(state, MatchCommandV1.PlayCharacter(playerBase.canonicalId, CharacterZone.ACTIVE))
        state = engine.apply(state, MatchCommandV1.CompleteSetup)
        state = ai.playUntilPlayerDecision(state)

        assertEquals(MatchPhase.MAIN, state.phase)
        assertEquals(MatchSide.PLAYER, state.activeSide)
        assertNotNull(state.opponent.active)
        assertEquals(MATCH_MAX_RESERVES, state.opponent.reserves.size)
        assertTrue(state.opponentSetupComplete)
    }

    @Test
    fun `ai attaches energy attacks and returns control to player`() {
        val engine = MatchEngineV1(catalog, 102)
        val ai = BasicMatchAiV1(catalog, engine)
        var state = readyMatch(engine, ai)

        state = engine.apply(state, MatchCommandV1.EndTurn)
        assertEquals(MatchSide.OPPONENT, state.activeSide)
        state = ai.playUntilPlayerDecision(state)

        assertEquals(MatchSide.PLAYER, state.activeSide)
        assertEquals(MatchPhase.MAIN, state.phase)
        assertEquals(30, state.player.active!!.damage)
        assertTrue(state.events.any { it.type == MatchEventType.RESOURCE_ATTACHED && it.side == MatchSide.OPPONENT })
        assertTrue(state.events.any { it.type == MatchEventType.ATTACK_DECLARED && it.side == MatchSide.OPPONENT })
    }

    @Test
    fun `ai does nothing while player has control`() {
        val engine = MatchEngineV1(catalog, 103)
        val ai = BasicMatchAiV1(catalog, engine)
        val state = readyMatch(engine, ai)

        assertEquals(state, ai.playUntilPlayerDecision(state))
    }

    @Test
    fun `ai resolves its pending promotion before playing its turn`() {
        val opponentDeck = List(10) { weakAi.canonicalId } + List(10) { aiBase.canonicalId }
        val seed = (0L..5_000L).first { candidate ->
            val opening = MatchEngineV1(catalog, candidate).start(List(20) { playerBase.canonicalId }, opponentDeck)
            weakAi.canonicalId in opening.opponent.hand && aiBase.canonicalId in opening.opponent.hand
        }
        val engine = MatchEngineV1(catalog, seed)
        val ai = BasicMatchAiV1(catalog, engine)
        var state = engine.start(List(20) { playerBase.canonicalId }, opponentDeck)
        state = engine.apply(state, MatchCommandV1.PlayCharacter(playerBase.canonicalId, CharacterZone.ACTIVE))
        state = engine.apply(state, MatchCommandV1.CompleteSetup)
        state = engine.apply(state, MatchCommandV1.PlayCharacter(weakAi.canonicalId, CharacterZone.ACTIVE))
        state = engine.apply(state, MatchCommandV1.PlayCharacter(aiBase.canonicalId, CharacterZone.RESERVE_1))
        state = engine.apply(state, MatchCommandV1.CompleteSetup)
        val active = state.player.active!!.instanceId
        state = engine.apply(state, MatchCommandV1.AttachResource(active))
        state = engine.apply(state, MatchCommandV1.Attack(0))

        assertEquals(MatchPhase.PROMOTION, state.phase)
        assertEquals(MatchSide.OPPONENT, state.pendingPromotionSide)

        state = ai.playUntilPlayerDecision(state)

        assertTrue(state.events.any { it.type == MatchEventType.CHARACTER_PROMOTED && it.side == MatchSide.OPPONENT })
        assertNotNull(state.opponent.active)
        assertEquals(MatchSide.PLAYER, state.activeSide)
    }

    private fun readyMatch(engine: MatchEngineV1, ai: BasicMatchAiV1): MatchStateV1 {
        var state = engine.start(List(20) { playerBase.canonicalId }, List(20) { aiBase.canonicalId })
        state = engine.apply(state, MatchCommandV1.PlayCharacter(playerBase.canonicalId, CharacterZone.ACTIVE))
        state = engine.apply(state, MatchCommandV1.CompleteSetup)
        return ai.playUntilPlayerDecision(state)
    }

    private fun card(id: String, name: String, hp: Int, attacks: List<Attack>) = CardDefinition(
        canonicalId = id,
        setId = "set",
        number = id.substringAfter(":").substringBefore(":"),
        name = name,
        kind = "personnage",
        stage = "base",
        evolvesFrom = null,
        hp = hp,
        retreat = 1,
        rarity = Rarity.C,
        attacks = attacks,
        effect = null,
        variants = listOf(CardVariant("$id:v1", "", "")),
    )
}
