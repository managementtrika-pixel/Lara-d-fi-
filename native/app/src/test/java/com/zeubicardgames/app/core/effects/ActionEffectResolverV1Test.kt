package com.zeubicardgames.app.core.effects

import com.zeubicardgames.app.core.gameengine.CharacterInPlay
import com.zeubicardgames.app.core.gameengine.MatchPhase
import com.zeubicardgames.app.core.gameengine.MatchSide
import com.zeubicardgames.app.core.gameengine.MatchStateV1
import com.zeubicardgames.app.core.gameengine.SideState
import com.zeubicardgames.app.core.model.Attack
import com.zeubicardgames.app.core.model.CardDefinition
import com.zeubicardgames.app.core.model.CardVariant
import com.zeubicardgames.app.core.model.Rarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionEffectResolverV1Test {
    private val base = card("ninja", "001", "Base", "personnage", "base", null)
    private val evo = card("ninja", "002", "Evo", "personnage", "evo1", null)
    private val senzu = card("dbz", "019", "Haricot Senzu", "action", "base", "heal90")
    private val scroll = card("ninja", "021", "Parchemin interdit", "action", "base", "searchEvolution")
    private val concentration = card("ninja", "023", "Concentration du chakra", "action", "base", "moveEnergy")
    private val unverified = card("cod", "019", "UAV en ligne", "action", "base", "searchPokemon")
    private val catalog = listOf(base, evo, senzu, scroll, concentration, unverified)
    private val resolver = ActionEffectResolverV1(catalog)

    @Test
    fun `senzu heals 80 and is consumed only after a valid target`() {
        val fighter = CharacterInPlay("p1", base.canonicalId, damage = 90)
        val initial = state(
            player = SideState(deck = emptyList(), hand = listOf(senzu.canonicalId), active = fighter),
        )

        val missingTarget = resolver.resolve(initial, MatchSide.PLAYER, senzu.canonicalId)
        assertFalse(missingTarget.success)
        assertEquals(listOf(senzu.canonicalId), missingTarget.state.player.hand)
        assertTrue(missingTarget.state.player.discard.isEmpty())

        val result = resolver.resolve(
            initial,
            MatchSide.PLAYER,
            senzu.canonicalId,
            ActionEffectChoiceV1(targetInstanceId = "p1"),
        )
        assertTrue(result.success)
        assertEquals(10, result.state.player.active!!.damage)
        assertFalse(senzu.canonicalId in result.state.player.hand)
        assertTrue(senzu.canonicalId in result.state.player.discard)
    }

    @Test
    fun `parchemin searches only an evolution actually present in deck`() {
        val initial = state(
            player = SideState(
                deck = listOf(base.canonicalId, evo.canonicalId),
                hand = listOf(scroll.canonicalId),
                active = CharacterInPlay("p1", base.canonicalId),
            ),
        )

        val invalid = resolver.resolve(
            initial,
            MatchSide.PLAYER,
            scroll.canonicalId,
            ActionEffectChoiceV1(selectedDeckCardId = base.canonicalId),
        )
        assertFalse(invalid.success)
        assertEquals(initial.player, invalid.state.player)

        val valid = resolver.resolve(
            initial,
            MatchSide.PLAYER,
            scroll.canonicalId,
            ActionEffectChoiceV1(selectedDeckCardId = evo.canonicalId),
        )
        assertTrue(valid.success)
        assertTrue(evo.canonicalId in valid.state.player.hand)
        assertFalse(evo.canonicalId in valid.state.player.deck)
        assertTrue(scroll.canonicalId in valid.state.player.discard)
    }

    @Test
    fun `concentration moves one or two resources and never invents energy`() {
        val active = CharacterInPlay("p1", base.canonicalId, resources = 2)
        val reserve = CharacterInPlay("p2", base.canonicalId, resources = 0)
        val initial = state(
            player = SideState(
                deck = emptyList(),
                hand = listOf(concentration.canonicalId),
                active = active,
                reserves = listOf(reserve),
            ),
        )

        val tooMany = resolver.resolve(
            initial,
            MatchSide.PLAYER,
            concentration.canonicalId,
            ActionEffectChoiceV1(fromInstanceId = "p1", toInstanceId = "p2", resourceCount = 3),
        )
        assertFalse(tooMany.success)
        assertEquals(2, tooMany.state.player.active!!.resources)
        assertEquals(0, tooMany.state.player.reserves.first().resources)
        assertTrue(concentration.canonicalId in tooMany.state.player.hand)

        val valid = resolver.resolve(
            initial,
            MatchSide.PLAYER,
            concentration.canonicalId,
            ActionEffectChoiceV1(fromInstanceId = "p1", toInstanceId = "p2", resourceCount = 2),
        )
        assertTrue(valid.success)
        assertEquals(0, valid.state.player.active!!.resources)
        assertEquals(2, valid.state.player.reserves.first().resources)
        assertTrue(concentration.canonicalId in valid.state.player.discard)
    }

    @Test
    fun `unverified action is rejected without consuming it`() {
        val initial = state(
            player = SideState(
                deck = emptyList(),
                hand = listOf(unverified.canonicalId),
                active = CharacterInPlay("p1", base.canonicalId),
            ),
        )
        val result = resolver.resolve(initial, MatchSide.PLAYER, unverified.canonicalId)
        assertFalse(result.success)
        assertTrue(unverified.canonicalId in result.state.player.hand)
        assertTrue(result.state.player.discard.isEmpty())
    }

    private fun state(player: SideState) = MatchStateV1(
        seed = 1,
        phase = MatchPhase.MAIN,
        activeSide = MatchSide.PLAYER,
        player = player,
        opponent = SideState(deck = emptyList()),
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
    ) = CardDefinition(
        canonicalId = "$setId:$number:${name.lowercase().replace(' ', '_')}",
        setId = setId,
        number = number,
        name = name,
        kind = kind,
        stage = stage,
        evolvesFrom = null,
        hp = if (kind == "personnage") 100 else 0,
        retreat = 1,
        rarity = Rarity.C,
        attacks = if (kind == "personnage") listOf(Attack("Attaque", 20, 1)) else emptyList(),
        effect = effect,
        variants = listOf(CardVariant("v1", "", "")),
    )
}
