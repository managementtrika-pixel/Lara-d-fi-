package com.zeubicardgames.app.core.deck

import com.zeubicardgames.app.core.model.CardDefinition
import com.zeubicardgames.app.core.model.Rarity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckRulesTest {
    private val base = card("base", "Base PLAYER", "personnage", "base")
    private val action = card("action", "Action PLAYER", "action", "aucune")
    private val catalog = listOf(base, action)

    @Test
    fun `valid deck requires exact size and a base character`() {
        val ids = List(2) { base.canonicalId } + List(18) { action.canonicalId }
        val result = DeckRules.validate(
            ids = ids,
            catalog = catalog,
            ownedQuantities = mapOf(base.canonicalId to 2, action.canonicalId to 18),
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Maximum 2 exemplaires") })
    }

    @Test
    fun `deck with twenty legal distinct copies is valid`() {
        val cards = (1..20).map { index ->
            card(
                id = "c$index",
                name = "Carte $index",
                kind = "personnage",
                stage = "base",
            )
        }
        val ids = cards.map { it.canonicalId }
        val owned = ids.associateWith { 1 }

        val result = DeckRules.validate(ids, cards, owned)

        assertTrue(result.errors.joinToString(), result.isValid)
    }

    @Test
    fun `deck explains when player does not own enough copies`() {
        val cards = (1..19).map { index ->
            card("c$index", "Carte $index", "personnage", "base")
        }
        val repeated = cards.first()
        val ids = cards.map { it.canonicalId } + repeated.canonicalId
        val owned = cards.associate { it.canonicalId to 1 }

        val result = DeckRules.validate(ids, cards, owned)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Cartes insuffisantes") })
        assertTrue(result.errors.any { it.contains(repeated.name) })
    }

    @Test
    fun `deck explains missing base character`() {
        val cards = (1..20).map { index ->
            card("a$index", "Action $index", "action", "aucune")
        }
        val ids = cards.map { it.canonicalId }

        val result = DeckRules.validate(ids, cards)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Personnage en forme initiale") })
    }

    @Test
    fun `deck explains unknown catalog cards`() {
        val cards = (1..19).map { index ->
            card("c$index", "Carte $index", "personnage", "base")
        }
        val ids = cards.map { it.canonicalId } + "CARD_UNKNOWN"

        val result = DeckRules.validate(ids, cards)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("n’existent plus dans le catalogue") })
    }

    private fun card(
        id: String,
        name: String,
        kind: String,
        stage: String,
    ) = CardDefinition(
        canonicalId = id,
        setId = "test",
        number = id,
        name = name,
        kind = kind,
        stage = stage,
        evolvesFrom = null,
        hp = if (kind == "personnage") 100 else 0,
        retreat = 1,
        rarity = Rarity.C,
        attacks = emptyList(),
        effect = null,
        variants = emptyList(),
    )
}
