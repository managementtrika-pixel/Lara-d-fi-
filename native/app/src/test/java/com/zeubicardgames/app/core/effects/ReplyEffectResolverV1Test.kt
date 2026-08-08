package com.zeubicardgames.app.core.effects

import com.zeubicardgames.app.core.model.Attack
import com.zeubicardgames.app.core.model.CardDefinition
import com.zeubicardgames.app.core.model.CardVariant
import com.zeubicardgames.app.core.model.Rarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplyEffectResolverV1Test {
    private val armor = card("cod", "025", "Plaque d’armure", "reduce40")
    private val clone = card("ninja", "026", "C’était un clone", "shield40")
    private val mine = card("cod", "027", "Mine surprise", "counter30")
    private val construction = card("emerald", "027", "Construction surprise", "counter30")
    private val lastChance = card("cod", "028", "Dernière chance", "survive10")
    private val will = card("emerald", "028", "Volonté inébranlable", "survive10")
    private val unsupported = card("cod", "026", "Flash !", "reduce30Lock")
    private val resolver = ReplyEffectResolverV1(
        listOf(armor, clone, mine, construction, lastChance, will, unsupported)
    )

    @Test
    fun `damage reduction replies subtract forty and never go below zero`() {
        listOf(armor, clone).forEach { reply ->
            val output = resolver.resolve(
                reply.canonicalId,
                ReplyWindowV1.BEFORE_DAMAGE,
                ReplyResolutionInputV1(
                    incomingDamage = 70,
                    defenderMaxHp = 100,
                    defenderDamageBeforeAttack = 10,
                ),
            )
            assertTrue(output.success)
            assertEquals(30, output.incomingDamage)
            assertEquals(40, output.defenderDamage)

            val tiny = resolver.resolve(
                reply.canonicalId,
                ReplyWindowV1.BEFORE_DAMAGE,
                ReplyResolutionInputV1(incomingDamage = 20, defenderMaxHp = 100),
            )
            assertEquals(0, tiny.incomingDamage)
            assertEquals(0, tiny.defenderDamage)
        }
    }

    @Test
    fun `counter replies deal thirty to attacker only after attack`() {
        listOf(mine, construction).forEach { reply ->
            val wrongTiming = resolver.resolve(
                reply.canonicalId,
                ReplyWindowV1.BEFORE_DAMAGE,
                ReplyResolutionInputV1(attackerDamageBeforeReply = 15),
            )
            assertFalse(wrongTiming.success)
            assertEquals(15, wrongTiming.attackerDamage)

            val output = resolver.resolve(
                reply.canonicalId,
                ReplyWindowV1.AFTER_ATTACK,
                ReplyResolutionInputV1(attackerDamageBeforeReply = 15),
            )
            assertTrue(output.success)
            assertEquals(45, output.attackerDamage)
        }
    }

    @Test
    fun `survival replies trigger only on imminent ko and leave ten hp`() {
        listOf(lastChance, will).forEach { reply ->
            val notKo = resolver.resolve(
                reply.canonicalId,
                ReplyWindowV1.BEFORE_KO,
                ReplyResolutionInputV1(
                    incomingDamage = 30,
                    defenderMaxHp = 100,
                    defenderDamageBeforeAttack = 20,
                ),
            )
            assertFalse(notKo.success)

            val output = resolver.resolve(
                reply.canonicalId,
                ReplyWindowV1.BEFORE_KO,
                ReplyResolutionInputV1(
                    incomingDamage = 80,
                    defenderMaxHp = 100,
                    defenderDamageBeforeAttack = 30,
                ),
            )
            assertTrue(output.success)
            assertTrue(output.preventsKo)
            assertEquals(90, output.defenderDamage)
            assertEquals(10, output.hpRemainingAfterPrevention)
        }
    }

    @Test
    fun `unsupported verified status is not enough to invent an executor`() {
        assertNull(resolver.supportedWindow(unsupported.canonicalId))
        val output = resolver.resolve(
            unsupported.canonicalId,
            ReplyWindowV1.BEFORE_DAMAGE,
            ReplyResolutionInputV1(incomingDamage = 100),
        )
        assertFalse(output.success)
        assertEquals(100, output.incomingDamage)
    }

    private fun card(setId: String, number: String, name: String, effect: String) = CardDefinition(
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
        attacks = listOf<Attack>(),
        effect = effect,
        variants = listOf(CardVariant("v1", "", "")),
    )
}
