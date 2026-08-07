package com.zeubicardgames.app.core.effects

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardEffectRegistryTest {
    @Test
    fun `all currently supported wire codes are unique`() {
        assertEquals(KnownCardEffect.entries.size, KnownCardEffect.wireCodes.size)
    }

    @Test
    fun `legacy action and reaction effects resolve deterministically`() {
        assertEquals(KnownCardEffect.SEARCH_PERSONNAGE, KnownCardEffect.from("searchPokemon"))
        assertEquals(KnownCardEffect.REDUCE_40, KnownCardEffect.from("reduce40"))
        assertEquals(KnownCardEffect.SHIELD_40, KnownCardEffect.from("shield40"))
        assertEquals(KnownCardEffect.SWITCH_BEFORE_HIT, KnownCardEffect.from("switchBeforeHit"))
    }

    @Test
    fun `unknown effect never silently becomes another effect`() {
        assertNull(KnownCardEffect.from("futureUnknownEffect"))
        assertTrue(KnownCardEffect.isKnown(null))
    }
}
