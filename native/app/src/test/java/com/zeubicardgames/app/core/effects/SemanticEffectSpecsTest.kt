package com.zeubicardgames.app.core.effects

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

class SemanticEffectSpecsTest {
    @Test
    fun `haricot senzu heals 80 despite legacy heal90 wire code`() {
        val spec = SemanticEffectRegistry.find(card("dbz", "019", "heal90"))
        assertNotNull(spec)
        assertEquals(EffectVerification.VERIFIED, spec!!.verification)
        assertEquals(
            EffectOperation.Heal(80, EffectTarget.OWN_CHARACTER),
            spec.steps.single().operation,
        )
    }

    @Test
    fun `confirmed defensive cards share semantic operations without depending on wire code`() {
        val clone = SemanticEffectRegistry.find(card("ninja", "026", "shield40"))!!
        val armor = SemanticEffectRegistry.find(card("cod", "025", "reduce40"))!!
        assertEquals(EffectOperation.ReduceIncomingDamage(40), clone.steps.single().operation)
        assertEquals(EffectOperation.ReduceIncomingDamage(40), armor.steps.single().operation)
    }

    @Test
    fun `counter and survive effects use their verified timings`() {
        val mine = SemanticEffectRegistry.find(card("cod", "027", "counter30"))!!
        assertEquals(EffectTiming.AFTER_ATTACK, mine.steps.single().timing)
        assertEquals(EffectOperation.DealDamage(30, EffectTarget.ATTACKER), mine.steps.single().operation)

        val lastChance = SemanticEffectRegistry.find(card("cod", "028", "survive10"))!!
        assertEquals(EffectTiming.BEFORE_KO, lastChance.steps.single().timing)
        assertEquals(EffectOperation.PreventKo(10), lastChance.steps.single().operation)
    }

    @Test
    fun `mission rang s is draw three then discard one`() {
        val spec = SemanticEffectRegistry.find(card("ninja", "024", "draw3discard1"))!!
        assertEquals(2, spec.steps.size)
        assertEquals(EffectOperation.Draw(3), spec.steps[0].operation)
        assertEquals(EffectOperation.DiscardFromHand(1), spec.steps[1].operation)
    }

    @Test
    fun `energy in deck legacy cards are blocked as explicit rule conflicts`() {
        val capsule = SemanticEffectRegistry.find(card("dbz", "021", "effectEnergy"))!!
        val chakra = SemanticEffectRegistry.find(card("ninja", "019", "effectEnergy"))!!
        assertEquals(EffectVerification.RULE_CONFLICT, capsule.verification)
        assertEquals(EffectVerification.RULE_CONFLICT, chakra.verification)
        assertFalse(SemanticEffectRegistry.isExecutable(card("dbz", "021", "effectEnergy")))
    }

    @Test
    fun `unverified card remains unavailable instead of receiving guessed behavior`() {
        val unknown = card("cod", "019", "searchPokemon")
        assertNull(SemanticEffectRegistry.find(unknown))
        assertFalse(SemanticEffectRegistry.isExecutable(unknown))
    }

    @Test
    fun `registry exposes only explicitly verified or conflicted cards`() {
        assertTrue("dbz:019" in SemanticEffectRegistry.verifiedCardKeys)
        assertTrue("ninja:026" in SemanticEffectRegistry.verifiedCardKeys)
        assertTrue("emerald:027" in SemanticEffectRegistry.verifiedCardKeys)
        assertTrue("dbz:021" in SemanticEffectRegistry.conflictCardKeys)
    }

    private fun card(setId: String, number: String, wire: String) = CardDefinition(
        canonicalId = "$setId:$number:test",
        setId = setId,
        number = number,
        name = "Test",
        kind = "action",
        stage = "base",
        evolvesFrom = null,
        hp = 0,
        retreat = 0,
        rarity = Rarity.C,
        attacks = listOf<Attack>(),
        effect = wire,
        variants = listOf(CardVariant("v1", "", "")),
    )
}
