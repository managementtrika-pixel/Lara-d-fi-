package com.zeubicardgames.app.core.gameengine

import com.zeubicardgames.app.core.model.Attack
import com.zeubicardgames.app.core.model.CardDefinition
import com.zeubicardgames.app.core.model.CardVariant
import com.zeubicardgames.app.core.model.Rarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    fun `same seed creates same initial hands`() {
        val deck = deckOf(baseA.canonicalId, baseB.canonicalId, weak.canonicalId, evoA.canonicalId, action.canonicalId, reply.canonicalId)
        val a = MatchEngineV1(catalog, 42).start(deck, deck)
        val b = MatchEngineV1(catalog, 42).start(deck, deck)

        assertEquals(a.player.hand, b.player.hand)
        assertEquals(MATCH_STARTING_HAND, a.player.hand.size)
        assertEquals(MATCH_STARTING_HAND, a.opponent.hand.size)
        assertTrue(a.events.any { it.type == MatchEventType.MATCH_STARTED })
    }

    @Test
    fun `only a base character can be played directly`() {
        val deck = List(10) { evoA.canonicalId } + List(10) { baseA.canonicalId }
        val engine = MatchEngineV1(catalog, 1)
        val state = engine.start(deck, deck)
        val evolutionInHand = state.player.hand.firstOrNull { it == evoA.canonicalId }
        assertNotNull(evolutionInHand)

        val rejected = engine.apply(state, MatchCommandV1.PlayCharacter(evolutionInHand!!, CharacterZone.ACTIVE))
        assertEquals(null, rejected.player.active)
        assertEquals(MatchEventType.COMMAND_REJECTED, rejected.events.last().type)
    }

    @Test
    fun `resource can be attached only once per turn`() {
        val deck = List(20) { baseA.canonicalId }
        val engine = MatchEngineV1(catalog, 2)
        var state = engine.start(deck, deck)
        state = engine.apply(state, MatchCommandV1.PlayCharacter(baseA.canonicalId, CharacterZone.ACTIVE))
        val activeId = state.player.active!!.instanceId

        state = engine.apply(state, MatchCommandV1.AttachResource(activeId))
        assertEquals(1, state.player.active!!.resources)
        state = engine.apply(state, MatchCommandV1.AttachResource(activeId))
        assertEquals(1, state.player.active!!.resources)
        assertEquals(MatchEventType.COMMAND_REJECTED, state.events.last().type)
    }

    @Test
    fun `evolution requires the correct lineage and a later turn`() {
        val playerDeck = List(10) { baseA.canonicalId } + List(10) { evoA.canonicalId }
        val opponentDeck = List(20) { baseB.canonicalId }
        val engine = MatchEngineV1(catalog, 4)
        var state = engine.start(playerDeck, opponentDeck)
        state = engine.apply(state, MatchCommandV1.PlayCharacter(baseA.canonicalId, CharacterZone.ACTIVE))
        val activeId = state.player.active!!.instanceId
        val evoId = state.player.hand.firstOrNull { it == evoA.canonicalId }
        if (evoId != null) {
            val sameTurn = engine.apply(state, MatchCommandV1.Evolve(evoId, activeId))
            assertEquals(baseA.canonicalId, sameTurn.player.active!!.cardId)
            assertEquals(MatchEventType.COMMAND_REJECTED, sameTurn.events.last().type)
        }

        state = engine.apply(state, MatchCommandV1.EndTurn)
        state = engine.apply(state, MatchCommandV1.PlayCharacter(baseB.canonicalId, CharacterZone.ACTIVE))
        state = engine.apply(state, MatchCommandV1.EndTurn)
        val evolutionNow = state.player.hand.firstOrNull { it == evoA.canonicalId }
        if (evolutionNow != null) {
            state = engine.apply(state, MatchCommandV1.Evolve(evolutionNow, activeId))
            assertEquals(evoA.canonicalId, state.player.active!!.cardId)
            assertTrue(state.events.any { it.type == MatchEventType.CARD_EVOLVED })
        }
    }

    @Test
    fun `ko awards a point and promotes the first reserve`() {
        val playerDeck = List(20) { baseA.canonicalId }
        val opponentDeck = List(10) { weak.canonicalId } + List(10) { baseB.canonicalId }
        val engine = MatchEngineV1(catalog, 8)
        var state = engine.start(playerDeck, opponentDeck)
        state = engine.apply(state, MatchCommandV1.PlayCharacter(baseA.canonicalId, CharacterZone.ACTIVE))
        val playerActive = state.player.active!!.instanceId
        state = engine.apply(state, MatchCommandV1.AttachResource(playerActive))
        state = engine.apply(state, MatchCommandV1.EndTurn)

        val weakInHand = state.opponent.hand.firstOrNull { it == weak.canonicalId }
        val reserveCard = state.opponent.hand.firstOrNull { it == baseB.canonicalId }
        if (weakInHand != null && reserveCard != null) {
            state = engine.apply(state, MatchCommandV1.PlayCharacter(weakInHand, CharacterZone.ACTIVE))
            state = engine.apply(state, MatchCommandV1.PlayCharacter(reserveCard, CharacterZone.RESERVE_1))
            state = engine.apply(state, MatchCommandV1.EndTurn)
            state = engine.apply(state, MatchCommandV1.Attack(0))

            assertEquals(1, state.player.points)
            assertNotNull(state.opponent.active)
            assertEquals(baseB.canonicalId, state.opponent.active!!.cardId)
            assertTrue(state.events.any { it.type == MatchEventType.CHARACTER_PROMOTED })
        }
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
