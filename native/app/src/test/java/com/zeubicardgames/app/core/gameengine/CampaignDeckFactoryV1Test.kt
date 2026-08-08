package com.zeubicardgames.app.core.gameengine

import com.zeubicardgames.app.core.deck.DeckRules
import com.zeubicardgames.app.core.model.Attack
import com.zeubicardgames.app.core.model.CampaignOpponent
import com.zeubicardgames.app.core.model.CardDefinition
import com.zeubicardgames.app.core.model.CardType
import com.zeubicardgames.app.core.model.CardVariant
import com.zeubicardgames.app.core.model.Difficulty
import com.zeubicardgames.app.core.model.EvolutionStage
import com.zeubicardgames.app.core.model.Rarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CampaignDeckFactoryV1Test {
    private val bases = (1..6).map { index -> character(index, "Base $index", "base", null, 80 + index) }
    private val evoOne = character(7, "Evo 1", "evo1", bases[0].canonicalId, 140)
    private val boss = character(8, "Boss", "evo2", evoOne.canonicalId, 200)
    private val supports = (9..14).map { index -> support(index, "Support $index") }
    private val catalog = bases + evoOne + boss + supports
    private val opponent = CampaignOpponent(
        id = "test_1",
        name = "Boss Test",
        extensionId = "test",
        difficulty = Difficulty.NORMAL,
        rewardCoins = 100,
        description = "Test",
        bossCardName = boss.name,
        bossCardId = boss.canonicalId,
    )

    @Test
    fun `generated ai deck is valid and contains the boss lineage`() {
        val deck = CampaignDeckFactoryV1.build(opponent, catalog)
        val validation = DeckRules.validate(deck, catalog)
        val byId = catalog.associateBy { it.canonicalId }

        assertEquals(DeckRules.DECK_SIZE, deck.size)
        assertTrue(validation.isValid)
        assertTrue(boss.canonicalId in deck)
        assertTrue(evoOne.canonicalId in deck)
        assertTrue(bases[0].canonicalId in deck)
        assertTrue(deck.groupingBy { it }.eachCount().values.all { it <= DeckRules.MAX_COPIES_PER_CARD })
        assertTrue(deck.mapNotNull(byId::get).count {
            it.type == CardType.PERSONNAGE && it.evolutionStage == EvolutionStage.BASE
        } >= 8)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown extension is rejected`() {
        CampaignDeckFactoryV1.build(opponent.copy(extensionId = "missing"), catalog)
    }

    private fun character(index: Int, name: String, stage: String, from: String?, hp: Int) = CardDefinition(
        canonicalId = "test:${index.toString().padStart(3, '0')}:$index",
        setId = "test",
        number = index.toString().padStart(3, '0'),
        name = name,
        kind = "personnage",
        stage = stage,
        evolvesFrom = null,
        hp = hp,
        retreat = 1,
        rarity = Rarity.C,
        attacks = listOf(Attack("Attaque", 30, 1)),
        effect = null,
        variants = listOf(CardVariant("v$index", "", "")),
        evolvesFromId = from,
    )

    private fun support(index: Int, name: String) = CardDefinition(
        canonicalId = "test:${index.toString().padStart(3, '0')}:$index",
        setId = "test",
        number = index.toString().padStart(3, '0'),
        name = name,
        kind = "action",
        stage = "base",
        evolvesFrom = null,
        hp = 0,
        retreat = 0,
        rarity = Rarity.U,
        attacks = emptyList(),
        effect = "heal90",
        variants = listOf(CardVariant("v$index", "", "")),
    )
}
