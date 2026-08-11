package com.zeubicardgames.app.core.gameengine

import com.zeubicardgames.app.core.effects.ActionEffectChoiceV1
import com.zeubicardgames.app.core.model.Attack
import com.zeubicardgames.app.core.model.CardDefinition
import com.zeubicardgames.app.core.model.CardVariant
import com.zeubicardgames.app.core.model.Rarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchEngineActionIntegrationTest {
    private val base = card("dbz", "001", "Base", "personnage", "base", null, hp = 100)
    private val senzu = card("dbz", "019", "Haricot Senzu", "action", "base", "heal90")
    private val unverified = card("cod", "019", "UAV en ligne", "action", "base", "searchPokemon")
    private val unsupportedReply = card("cod", "026", "Flash !", "replique", "base", "reduce30Lock")
    private val catalog = listOf(base, senzu, unverified, unsupportedReply)

    @Test
    fun `verified action resolves through match engine and is consumed`() {
        val engine = MatchEngineV1(catalog, 1)
        val fighter = CharacterInPlay("p1", base.canonicalId, damage = 90)
        val state = matchState(
            hand = listOf(senzu.canonicalId),
            active = fighter,
        )

        val next = engine.apply(
            state,
            MatchCommandV1.UseAction(
                senzu.canonicalId,
                ActionEffectChoiceV1(targetInstanceId = fighter.instanceId),
            ),
        )

        assertEquals(10, next.player.active!!.damage)
        assertFalse(senzu.canonicalId in next.player.hand)
        assertTrue(senzu.canonicalId in next.player.discard)
        assertEquals(MatchEventType.ACTION_USED, next.events.last().type)
    }

    @Test
    fun `invalid action choice is rejected and card remains in hand`() {
        val engine = MatchEngineV1(catalog, 2)
        val state = matchState(
            hand = listOf(senzu.canonicalId),
            active = CharacterInPlay("p1", base.canonicalId, damage = 90),
        )

        val next = engine.apply(state, MatchCommandV1.UseAction(senzu.canonicalId))

        assertTrue(senzu.canonicalId in next.player.hand)
        assertTrue(next.player.discard.isEmpty())
        assertEquals(MatchEventType.COMMAND_REJECTED, next.events.last().type)
    }

    @Test
    fun `unverified action is rejected instead of pretending to resolve`() {
        val engine = MatchEngineV1(catalog, 3)
        val state = matchState(
            hand = listOf(unverified.canonicalId),
            active = CharacterInPlay("p1", base.canonicalId),
        )

        val next = engine.apply(state, MatchCommandV1.UseAction(unverified.canonicalId))

        assertTrue(unverified.canonicalId in next.player.hand)
        assertTrue(next.player.discard.isEmpty())
        assertEquals(MatchEventType.COMMAND_REJECTED, next.events.last().type)
    }

    @Test
    fun `unsupported reply cannot be armed before its trigger resolver exists`() {
        val engine = MatchEngineV1(catalog, 4)
        val state = matchState(
            hand = listOf(unsupportedReply.canonicalId),
            active = CharacterInPlay("p1", base.canonicalId),
        )

        val next = engine.apply(state, MatchCommandV1.ArmReply(unsupportedReply.canonicalId))

        assertTrue(unsupportedReply.canonicalId in next.player.hand)
        assertTrue(next.player.supportSlots.isEmpty())
        assertEquals(MatchEventType.COMMAND_REJECTED, next.events.last().type)
    }

    private fun matchState(hand: List<String>, active: CharacterInPlay) = MatchStateV1(
        seed = 1,
        phase = MatchPhase.MAIN,
        activeSide = MatchSide.PLAYER,
        player = SideState(deck = emptyList(), hand = hand, active = active),
        opponent = SideState(deck = emptyList(), active = CharacterInPlay("o1", base.canonicalId)),
        playerSetupComplete = true,
        opponentSetupComplete = true,
    )

    private fun card(
        setId: String,
        number: String,
        name: String,
        kind: String,
        stage: String,
        effect: String?,
        hp: Int = 0,
    ) = CardDefinition(
        canonicalId = "$setId:$number:${name.lowercase().replace(' ', '_')}",
        setId = setId,
        number = number,
        name = name,
        kind = kind,
        stage = stage,
        evolvesFrom = null,
        hp = hp,
        retreat = 1,
        rarity = Rarity.C,
        attacks = if (kind == "personnage") listOf(Attack("Attaque", 20, 1)) else emptyList(),
        effect = effect,
        variants = listOf(CardVariant("v1", "", "")),
    )
}
