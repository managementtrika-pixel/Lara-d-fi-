package com.zeubicardgames.app.core.booster

import com.zeubicardgames.app.core.model.CardDefinition
import com.zeubicardgames.app.core.model.CardVariant
import com.zeubicardgames.app.core.model.Rarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PackGeneratorTest {
    @Test
    fun `same seed produces same booster`() {
        val pool = listOf(
            card("c1", Rarity.C),
            card("c2", Rarity.C),
            card("r1", Rarity.R),
        )

        val first = PackGenerator.generate(pool, seed = 42L).map { it.canonicalId }
        val second = PackGenerator.generate(pool, seed = 42L).map { it.canonicalId }

        assertEquals(first, second)
    }

    @Test
    fun `booster contains five cards by default`() {
        val pool = listOf(card("c1", Rarity.C), card("r1", Rarity.R))

        assertEquals(5, PackGenerator.generate(pool, seed = 7L).size)
    }

    @Test
    fun `booster guarantees at least rare when rare exists`() {
        val pool = listOf(
            card("c1", Rarity.C),
            card("c2", Rarity.C),
            card("r1", Rarity.R),
        )

        repeat(100) { seed ->
            val pulls = PackGenerator.generate(pool, seed = seed.toLong())
            assertTrue(pulls.any { it.rarity.rank >= Rarity.R.rank })
        }
    }

    private fun card(id: String, rarity: Rarity) = CardDefinition(
        canonicalId = id,
        setId = "test",
        number = id,
        name = id,
        kind = "pokemon",
        stage = "base",
        evolvesFrom = null,
        hp = 100,
        retreat = 1,
        rarity = rarity,
        attacks = emptyList(),
        effect = null,
        variants = listOf(CardVariant("$id-v1", "full.webp", "thumb.webp")),
    )
}
