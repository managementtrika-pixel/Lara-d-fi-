package com.zeubicardgames.app.core.gameengine

import com.zeubicardgames.app.core.model.Attack
import com.zeubicardgames.app.core.model.CardDefinition
import com.zeubicardgames.app.core.model.CardVariant
import com.zeubicardgames.app.core.model.Rarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchEngineV1Test {
    private val baseA = card("set:001:base_a", "Base A", "base", hp = 100, attacks = listOf(Attack("Frappe", 40, 1)))
    private val evoA = card("set:002:evo_a", "Evo A", "evo1", hp = 160, evolvesFromId = baseA.canonicalId, attacks = listOf(Attack("Impact", 80, 2)))
    private val baseB = card("set:003:base_b", "Base B", "base", hp = 90, attacks = listOf(Attack("Tir", 30, 1)))
    private val weak = card("set:004:weak", "Faible", "base", hp = 30, attacks = listOf(Attack("Petit coup", 10, 0)))
    private val action = card("set:019:action", "Action", "base", hp = 0, kind = "action", effect = "heal90", attacks = emptyList())
    private val reply = card("set:025:reply", "Réplique", "base", hp = 0, kind = "replique", effect = "shield40", attacks = emptyList())
    private val catalog = listOf(baseA, evoA, baseB, weak, action, reply)

    @Test
    fun `same seed creates same five card initial hands`() {
        val deck = deckOf(baseA.canonicalId, baseB.canonicalId, weak.canonicalId, evoA.canonicalId, action.canonicalId, reply.canonicalId)
        val a = MatchEngineV1(catalog, 42).start(deck, deck)
        val b = MatchEngineV1(catalog, 42).start(deck, deck)

        assertEquals(a.player.hand, b.player.hand)
        assertEquals(5, MATCH_STARTING_HAND)
        assertEquals(MATCH_STARTING_HAND, a.player.hand.size)
        assertEquals(MATCH_STARTING_HAND, a.opponent.hand.size)
        assertEquals(MatchPhase.SETUP, a.phase)
        assertTrue(a.events.any { it.type == MatchEventType.MATCH_STARTED })
        assertFalse(a.events.any { it.type == MatchEventType.TURN_STARTED })
    }

    @Test
    fun `only a base character can be played directly`() {
        val deck = List(20) { evoA.canonicalId }
        val engine = MatchEngineV1(catalog, 1)
        val state = engine.start(deck, deck)

        val rejected = engine.apply(state, MatchCommandV1.PlayCharacter(evoA.canonicalId, CharacterZone.ACTIVE))
        assertNull(rejected.player.active)
        assertEquals(MatchEventType.COMMAND_REJECTED, rejected.events.last().type)
    }

    @Test
    fun `both sides must finish setup before turn one`() {
        val deck = List(20) { baseA.canonicalId }
        val engine = MatchEngineV1(catalog, 2)
        var state = engine.start(deck, deck)

        state = engine.apply(state, MatchCommandV1.CompleteSetup)
        assertEquals(MatchEventType.COMMAND_REJECTED, state.events.last().type)
        assertFalse(state.playerSetupComplete)

        state = engine.apply(state, MatchCommandV1.PlayCharacter(baseA.canonicalId, CharacterZone.ACTIVE))
        state = engine.apply(state, MatchCommandV1.CompleteSetup)
        assertTrue(state.playerSetupComplete)
        assertEquals(MatchSide.OPPONENT, state.activeSide)
        assertEquals(MatchPhase.SETUP, state.phase)

        state = engine.apply(state, MatchCommandV1.PlayCharacter(baseA.canonicalId, CharacterZone.ACTIVE))
        state = engine.apply(state, MatchCommandV1.CompleteSetup)
        assertTrue(state.opponentSetupComplete)
        assertEquals(MatchSide.PLAYER, state.activeSide)
        assertEquals(MatchPhase.MAIN, state.phase)
        assertEquals(MatchEventType.TURN_STARTED, state.events.last().type)
    }

    @Test
    fun `resource can be attached only once per turn`() {
        val deck = List(20) { baseA.canonicalId }
        val engine = MatchEngineV1(catalog, 3)
        var state = readyMatch(engine, deck, deck, baseA.canonicalId, baseA.canonicalId)
        val activeId = state.player.active!!.instanceId

        state = engine.apply(state, MatchCommandV1.AttachResource(activeId))
        assertEquals(1, state.player.active!!.resources)
        state = engine.apply(state, MatchCommandV1.AttachResource(activeId))
        assertEquals(1, state.player.active!!.resources)
        assertEquals(MatchEventType.COMMAND_REJECTED, state.events.last().type)
    }

    @Test
    fun `retreat pays the active cost and can happen only once per turn`() {
        val deck = List(20) { baseA.canonicalId }
        val engine = MatchEngineV1(catalog, 5)
        var state = engine.start(deck, deck)
        state = engine.apply(state, MatchCommandV1.PlayCharacter(baseA.canonicalId, CharacterZone.ACTIVE))
        state = engine.apply(state, MatchCommandV1.PlayCharacter(baseA.canonicalId, CharacterZone.RESERVE_1))
        val oldActiveId = state.player.active!!.instanceId
        val reserveId = state.player.reserves.first().instanceId
        state = engine.apply(state, MatchCommandV1.CompleteSetup)
        state = engine.apply(state, MatchCommandV1.PlayCharacter(baseA.canonicalId, CharacterZone.ACTIVE))
        state = engine.apply(state, MatchCommandV1.CompleteSetup)

        val withoutEnergy = engine.apply(state, MatchCommandV1.Retreat(reserveId))
        assertEquals(oldActiveId, withoutEnergy.player.active!!.instanceId)
        assertEquals(MatchEventType.COMMAND_REJECTED, withoutEnergy.events.last().type)

        state = engine.apply(state, MatchCommandV1.AttachResource(oldActiveId))
        state = engine.apply(state, MatchCommandV1.Retreat(reserveId))
        assertEquals(reserveId, state.player.active!!.instanceId)
        val retired = state.player.reserves.first { it.instanceId == oldActiveId }
        assertEquals(0, retired.resources)
        assertTrue(state.flags.retreatUsed)
        assertTrue(state.events.any { it.type == MatchEventType.CHARACTER_RETREATED })

        val second = engine.apply(state, MatchCommandV1.Retreat(oldActiveId))
        assertEquals(reserveId, second.player.active!!.instanceId)
        assertEquals(MatchEventType.COMMAND_REJECTED, second.events.last().type)
    }

    @Test
    fun `evolution is forbidden on turn one and preserves the stack later`() {
        val playerDeck = List(10) { baseA.canonicalId } + List(10) { evoA.canonicalId }
        val opponentDeck = List(20) { baseB.canonicalId }
        val seed = findSeed(playerDeck, opponentDeck) { state ->
            baseA.canonicalId in state.player.hand && evoA.canonicalId in state.player.hand
        }
        val engine = MatchEngineV1(catalog, seed)
        var state = readyMatch(engine, playerDeck, opponentDeck, baseA.canonicalId, baseB.canonicalId)
        val activeId = state.player.active!!.instanceId

        val turnOne = engine.apply(state, MatchCommandV1.Evolve(evoA.canonicalId, activeId))
        assertEquals(baseA.canonicalId, turnOne.player.active!!.cardId)
        assertEquals(MatchEventType.COMMAND_REJECTED, turnOne.events.last().type)

        state = engine.apply(state, MatchCommandV1.EndTurn)
        state = engine.apply(state, MatchCommandV1.EndTurn)
        state = engine.apply(state, MatchCommandV1.Evolve(evoA.canonicalId, activeId))
        assertEquals(evoA.canonicalId, state.player.active!!.cardId)
        assertEquals(listOf(baseA.canonicalId), state.player.active!!.evolutionStack)
        assertFalse(baseA.canonicalId in state.player.discard)
        assertTrue(state.events.any { it.type == MatchEventType.CARD_EVOLVED })
    }

    @Test
    fun `draw never exceeds the ten card hand limit`() {
        val deck = List(20) { baseA.canonicalId }
        val engine = MatchEngineV1(catalog, 9)
        var state = readyMatch(engine, deck, deck, baseA.canonicalId, baseA.canonicalId)

        repeat(12) { state = engine.apply(state, MatchCommandV1.EndTurn) }
        assertEquals(MATCH_MAX_HAND, state.player.hand.size)
        assertEquals(MATCH_MAX_HAND, state.opponent.hand.size)
        assertEquals(MatchSide.PLAYER, state.activeSide)

        val playerDeckBeforeSkippedDraw = state.player.deck.size
        state = engine.apply(state, MatchCommandV1.EndTurn)
        state = engine.apply(state, MatchCommandV1.EndTurn)

        assertEquals(MATCH_MAX_HAND, state.player.hand.size)
        assertEquals(playerDeckBeforeSkippedDraw, state.player.deck.size)
        assertTrue(state.events.count { it.type == MatchEventType.DRAW_SKIPPED } >= 2)
    }

    @Test
    fun `ko waits for an explicit valid promotion then ends the attackers turn`() {
        val playerDeck = List(20) { baseA.canonicalId }
        val opponentDeck = List(10) { weak.canonicalId } + List(10) { baseB.canonicalId }
        val seed = findSeed(playerDeck, opponentDeck) { state ->
            weak.canonicalId in state.opponent.hand && baseB.canonicalId in state.opponent.hand
        }
        val engine = MatchEngineV1(catalog, seed)
        var state = engine.start(playerDeck, opponentDeck)
        state = engine.apply(state, MatchCommandV1.PlayCharacter(baseA.canonicalId, CharacterZone.ACTIVE))
        state = engine.apply(state, MatchCommandV1.CompleteSetup)
        state = engine.apply(state, MatchCommandV1.PlayCharacter(weak.canonicalId, CharacterZone.ACTIVE))
        state = engine.apply(state, MatchCommandV1.PlayCharacter(baseB.canonicalId, CharacterZone.RESERVE_1))
        val reserveId = state.opponent.reserves.first().instanceId
        state = engine.apply(state, MatchCommandV1.CompleteSetup)

        val playerActive = state.player.active!!.instanceId
        state = engine.apply(state, MatchCommandV1.AttachResource(playerActive))
        state = engine.apply(state, MatchCommandV1.Attack(0))

        assertEquals(1, state.player.points)
        assertNull(state.opponent.active)
        assertEquals(MatchPhase.PROMOTION, state.phase)
        assertEquals(MatchSide.OPPONENT, state.pendingPromotionSide)
        assertEquals(MatchSide.PLAYER, state.activeSide)
        assertTrue(state.events.any { it.type == MatchEventType.PROMOTION_REQUIRED })

        val wrong = engine.apply(state, MatchCommandV1.Promote("absent"))
        assertEquals(MatchPhase.PROMOTION, wrong.phase)
        assertNull(wrong.opponent.active)
        assertEquals(MatchEventType.COMMAND_REJECTED, wrong.events.last().type)

        state = engine.apply(state, MatchCommandV1.Promote(reserveId))
        assertNotNull(state.opponent.active)
        assertEquals(baseB.canonicalId, state.opponent.active!!.cardId)
        assertNull(state.pendingPromotionSide)
        assertEquals(MatchPhase.MAIN, state.phase)
        assertEquals(MatchSide.OPPONENT, state.activeSide)
        assertEquals(2, state.turnNumber)
        assertTrue(state.events.any { it.type == MatchEventType.CHARACTER_PROMOTED })
    }

    @Test
    fun `ko with no reserve immediately finishes the match`() {
        val playerDeck = List(20) { baseA.canonicalId }
        val opponentDeck = List(20) { weak.canonicalId }
        val engine = MatchEngineV1(catalog, 11)
        var state = readyMatch(engine, playerDeck, opponentDeck, baseA.canonicalId, weak.canonicalId)
        val playerActive = state.player.active!!.instanceId

        state = engine.apply(state, MatchCommandV1.AttachResource(playerActive))
        state = engine.apply(state, MatchCommandV1.Attack(0))

        assertEquals(MatchPhase.FINISHED, state.phase)
        assertEquals(MatchSide.PLAYER, state.winner)
        assertNull(state.opponent.active)
        assertTrue(state.events.any { it.type == MatchEventType.MATCH_FINISHED })
    }

    private fun readyMatch(
        engine: MatchEngineV1,
        playerDeck: List<String>,
        opponentDeck: List<String>,
        playerBase: String,
        opponentBase: String,
    ): MatchStateV1 {
        var state = engine.start(playerDeck, opponentDeck)
        state = engine.apply(state, MatchCommandV1.PlayCharacter(playerBase, CharacterZone.ACTIVE))
        state = engine.apply(state, MatchCommandV1.CompleteSetup)
        state = engine.apply(state, MatchCommandV1.PlayCharacter(opponentBase, CharacterZone.ACTIVE))
        state = engine.apply(state, MatchCommandV1.CompleteSetup)
        return state
    }

    private fun findSeed(
        playerDeck: List<String>,
        opponentDeck: List<String>,
        predicate: (MatchStateV1) -> Boolean,
    ): Long = (0L..5_000L).first { seed ->
        predicate(MatchEngineV1(catalog, seed).start(playerDeck, opponentDeck))
    }

    private fun deckOf(vararg ids: String): List<String> = buildList {
        while (size < 20) add(ids[size % ids.size])
    }

    private fun card(
        id: String,
        name: String,
        stage: String,
        hp: Int,
        kind: String = "personnage",
        evolvesFromId: String? = null,
        effect: String? = null,
        attacks: List<Attack>,
    ) = CardDefinition(
        canonicalId = id,
        setId = "set",
        number = id.substringAfter(":").substringBefore(":"),
        name = name,
        kind = kind,
        stage = stage,
        evolvesFrom = null,
        hp = hp,
        retreat = 1,
        rarity = Rarity.C,
        attacks = attacks,
        effect = effect,
        variants = listOf(CardVariant("$id:v1", "", "")),
        evolvesFromId = evolvesFromId,
    )
}
