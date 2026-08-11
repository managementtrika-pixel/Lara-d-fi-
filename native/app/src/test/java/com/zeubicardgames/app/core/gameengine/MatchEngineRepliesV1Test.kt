package com.zeubicardgames.app.core.gameengine

import com.zeubicardgames.app.core.effects.ReplyWindowV1
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

class MatchEngineRepliesV1Test {
    private val attacker70 = character("test", "001", "Attaquant 70", hp = 120, damage = 70)
    private val attacker120 = character("test", "002", "Attaquant 120", hp = 120, damage = 120)
    private val fragileAttacker = character("test", "003", "Attaquant fragile", hp = 30, damage = 40)
    private val defender = character("test", "004", "Défenseur", hp = 100, damage = 20)

    private val armor = reply("cod", "025", "Plaque d’armure", "reduce40")
    private val clone = reply("ninja", "026", "C’était un clone", "shield40")
    private val mine = reply("cod", "027", "Mine surprise", "counter30")
    private val lastChance = reply("cod", "028", "Dernière chance", "survive10")
    private val unsupported = reply("cod", "026", "Flash !", "reduce30Lock")

    private val catalog = listOf(
        attacker70,
        attacker120,
        fragileAttacker,
        defender,
        armor,
        clone,
        mine,
        lastChance,
        unsupported,
    )

    @Test
    fun `supported reply moves from hand to a support slot and unsupported reply stays in hand`() {
        val playerDeck = List(10) { attacker70.canonicalId } + List(10) { armor.canonicalId }
        val opponentDeck = List(20) { defender.canonicalId }
        val seed = findSeed(playerDeck, opponentDeck) {
            attacker70.canonicalId in it.player.hand && armor.canonicalId in it.player.hand
        }
        val engine = MatchEngineV1(catalog, seed)
        var state = readyMatch(engine, playerDeck, opponentDeck, attacker70.canonicalId, defender.canonicalId)

        state = engine.apply(state, MatchCommandV1.ArmReply(armor.canonicalId))
        assertTrue(armor.canonicalId in state.player.supportSlots)
        assertFalse(armor.canonicalId in state.player.hand)
        assertEquals(MatchEventType.REPLY_ARMED, state.events.last().type)

        val unsupportedDeck = List(10) { attacker70.canonicalId } + List(10) { unsupported.canonicalId }
        val unsupportedSeed = findSeed(unsupportedDeck, opponentDeck) {
            attacker70.canonicalId in it.player.hand && unsupported.canonicalId in it.player.hand
        }
        val unsupportedEngine = MatchEngineV1(catalog, unsupportedSeed)
        var unsupportedState = readyMatch(
            unsupportedEngine,
            unsupportedDeck,
            opponentDeck,
            attacker70.canonicalId,
            defender.canonicalId,
        )
        unsupportedState = unsupportedEngine.apply(
            unsupportedState,
            MatchCommandV1.ArmReply(unsupported.canonicalId),
        )
        assertTrue(unsupported.canonicalId in unsupportedState.player.hand)
        assertFalse(unsupported.canonicalId in unsupportedState.player.supportSlots)
        assertEquals(MatchEventType.COMMAND_REJECTED, unsupportedState.events.last().type)
    }

    @Test
    fun `support zone accepts at most three armed replies`() {
        val playerDeck = listOf(attacker70.canonicalId) + List(19) { armor.canonicalId }
        val opponentDeck = List(20) { defender.canonicalId }
        val seed = findSeed(playerDeck, opponentDeck) {
            attacker70.canonicalId in it.player.hand &&
                it.player.hand.count { id -> id == armor.canonicalId } >= 4
        }
        val engine = MatchEngineV1(catalog, seed)
        var state = readyMatch(engine, playerDeck, opponentDeck, attacker70.canonicalId, defender.canonicalId)

        repeat(3) {
            state = engine.apply(state, MatchCommandV1.ArmReply(armor.canonicalId))
        }
        assertEquals(3, state.player.supportSlots.size)

        val rejected = engine.apply(state, MatchCommandV1.ArmReply(armor.canonicalId))
        assertEquals(3, rejected.player.supportSlots.size)
        assertTrue(armor.canonicalId in rejected.player.hand)
        assertEquals(MatchEventType.COMMAND_REJECTED, rejected.events.last().type)
    }

    @Test
    fun `before damage reply reduces incoming damage and is discarded only after successful resolution`() {
        val prepared = prepareDefenderReply(attacker70, armor)
        val engine = prepared.first
        var state = prepared.second

        state = engine.apply(state, MatchCommandV1.Attack(0))
        assertEquals(MatchPhase.RESOLVING, state.phase)
        assertEquals(ReplyWindowV1.BEFORE_DAMAGE, state.pendingReaction?.window)
        assertEquals(0, state.opponent.active!!.damage)

        state = engine.apply(state, MatchCommandV1.ResolveReaction(armor.canonicalId))
        assertEquals(30, state.opponent.active!!.damage)
        assertTrue(armor.canonicalId in state.opponent.discard)
        assertFalse(armor.canonicalId in state.opponent.supportSlots)
        assertNull(state.pendingReaction)
        assertNull(state.pendingAttack)
        assertEquals(MatchSide.OPPONENT, state.activeSide)
        assertTrue(state.events.any { it.type == MatchEventType.REPLY_TRIGGERED && it.cardId == armor.canonicalId })
        assertTrue(state.events.any { it.type == MatchEventType.DAMAGE_CALCULATED })
        assertTrue(state.events.any { it.type == MatchEventType.EFFECT_APPLIED })
    }

    @Test
    fun `before ko survival reply prevents point and leaves exactly ten hp`() {
        val prepared = prepareDefenderReply(attacker120, lastChance)
        val engine = prepared.first
        var state = prepared.second

        state = engine.apply(state, MatchCommandV1.Attack(0))
        assertEquals(ReplyWindowV1.BEFORE_KO, state.pendingReaction?.window)
        assertEquals(0, state.player.points)

        state = engine.apply(state, MatchCommandV1.ResolveReaction(lastChance.canonicalId))
        assertEquals(90, state.opponent.active!!.damage)
        assertEquals(10, state.opponent.active!!.hpLeft(catalog.associateBy { it.canonicalId }))
        assertEquals(0, state.player.points)
        assertTrue(lastChance.canonicalId in state.opponent.discard)
        assertFalse(state.events.any { it.type == MatchEventType.CARD_KNOCKED_OUT })
        assertEquals(MatchSide.OPPONENT, state.activeSide)
    }

    @Test
    fun `after attack counter deals thirty to attacker and then ends turn`() {
        val prepared = prepareDefenderReply(attacker70, mine)
        val engine = prepared.first
        var state = prepared.second

        state = engine.apply(state, MatchCommandV1.Attack(0))
        assertEquals(70, state.opponent.active!!.damage)
        assertEquals(ReplyWindowV1.AFTER_ATTACK, state.pendingReaction?.window)

        state = engine.apply(state, MatchCommandV1.ResolveReaction(mine.canonicalId))
        assertEquals(30, state.player.active!!.damage)
        assertTrue(mine.canonicalId in state.opponent.discard)
        assertNull(state.pendingReaction)
        assertNull(state.pendingAttack)
        assertEquals(MatchSide.OPPONENT, state.activeSide)
    }

    @Test
    fun `reply from another window cannot be consumed`() {
        val playerDeck = List(20) { attacker120.canonicalId }
        val opponentDeck = listOf(defender.canonicalId) +
            List(9) { armor.canonicalId } +
            List(10) { lastChance.canonicalId }
        val seed = findSeed(playerDeck, opponentDeck) {
            defender.canonicalId in it.opponent.hand &&
                armor.canonicalId in it.opponent.hand &&
                lastChance.canonicalId in it.opponent.hand
        }
        val engine = MatchEngineV1(catalog, seed)
        var state = readyMatch(engine, playerDeck, opponentDeck, attacker120.canonicalId, defender.canonicalId)
        val playerActive = state.player.active!!.instanceId
        state = engine.apply(state, MatchCommandV1.AttachResource(playerActive))
        state = engine.apply(state, MatchCommandV1.EndTurn)
        state = engine.apply(state, MatchCommandV1.ArmReply(armor.canonicalId))
        state = engine.apply(state, MatchCommandV1.ArmReply(lastChance.canonicalId))
        state = engine.apply(state, MatchCommandV1.EndTurn)

        state = engine.apply(state, MatchCommandV1.Attack(0))
        assertEquals(ReplyWindowV1.BEFORE_DAMAGE, state.pendingReaction?.window)

        val wrong = engine.apply(state, MatchCommandV1.ResolveReaction(lastChance.canonicalId))
        assertEquals(MatchEventType.COMMAND_REJECTED, wrong.events.last().type)
        assertTrue(lastChance.canonicalId in wrong.opponent.supportSlots)
        assertTrue(armor.canonicalId in wrong.opponent.supportSlots)

        state = engine.apply(wrong, MatchCommandV1.ResolveReaction())
        assertEquals(ReplyWindowV1.BEFORE_KO, state.pendingReaction?.window)
        state = engine.apply(state, MatchCommandV1.ResolveReaction(lastChance.canonicalId))
        assertEquals(90, state.opponent.active!!.damage)
        assertTrue(armor.canonicalId in state.opponent.supportSlots)
    }

    @Test
    fun `counter that would ko attacker remains armed until simultaneous ko rule is defined`() {
        val prepared = prepareDefenderReply(fragileAttacker, mine)
        val engine = prepared.first
        var state = prepared.second

        state = engine.apply(state, MatchCommandV1.Attack(0))
        assertEquals(ReplyWindowV1.AFTER_ATTACK, state.pendingReaction?.window)

        val rejected = engine.apply(state, MatchCommandV1.ResolveReaction(mine.canonicalId))
        assertEquals(MatchEventType.COMMAND_REJECTED, rejected.events.last().type)
        assertTrue(mine.canonicalId in rejected.opponent.supportSlots)
        assertFalse(mine.canonicalId in rejected.opponent.discard)
        assertNotNull(rejected.pendingReaction)

        state = engine.apply(rejected, MatchCommandV1.ResolveReaction())
        assertNull(state.pendingReaction)
        assertEquals(MatchSide.OPPONENT, state.activeSide)
        assertEquals(0, state.player.active!!.damage)
    }

    private fun prepareDefenderReply(
        attacker: CardDefinition,
        reply: CardDefinition,
    ): Pair<MatchEngineV1, MatchStateV1> {
        val playerDeck = List(20) { attacker.canonicalId }
        val opponentDeck = listOf(defender.canonicalId) + List(19) { reply.canonicalId }
        val seed = findSeed(playerDeck, opponentDeck) {
            defender.canonicalId in it.opponent.hand && reply.canonicalId in it.opponent.hand
        }
        val engine = MatchEngineV1(catalog, seed)
        var state = readyMatch(engine, playerDeck, opponentDeck, attacker.canonicalId, defender.canonicalId)
        val playerActive = state.player.active!!.instanceId
        state = engine.apply(state, MatchCommandV1.AttachResource(playerActive))
        state = engine.apply(state, MatchCommandV1.EndTurn)
        state = engine.apply(state, MatchCommandV1.ArmReply(reply.canonicalId))
        state = engine.apply(state, MatchCommandV1.EndTurn)
        return engine to state
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
    ): Long = (0L..10_000L).first { seed ->
        predicate(MatchEngineV1(catalog, seed).start(playerDeck, opponentDeck))
    }

    private fun character(
        setId: String,
        number: String,
        name: String,
        hp: Int,
        damage: Int,
    ) = CardDefinition(
        canonicalId = "$setId:$number:${name.lowercase().replace(' ', '_')}",
        setId = setId,
        number = number,
        name = name,
        kind = "personnage",
        stage = "base",
        evolvesFrom = null,
        hp = hp,
        retreat = 1,
        rarity = Rarity.C,
        attacks = listOf(Attack("Attaque", damage, 1)),
        effect = null,
        variants = listOf(CardVariant("v1", "", "")),
    )

    private fun reply(
        setId: String,
        number: String,
        name: String,
        effect: String,
    ) = CardDefinition(
        canonicalId = "$setId:$number:${name.lowercase().replace(' ', '_')}",
        setId = setId,
        number = number,
        name = name,
        kind = "replique",
        stage = "base",
        evolvesFrom = null,
        hp = 0,
        retreat = 0,
        rarity = Rarity.R,
        attacks = emptyList(),
        effect = effect,
        variants = listOf(CardVariant("v1", "", "")),
    )
}
