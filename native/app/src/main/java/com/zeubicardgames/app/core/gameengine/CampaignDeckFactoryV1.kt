package com.zeubicardgames.app.core.gameengine

import com.zeubicardgames.app.core.deck.DeckRules
import com.zeubicardgames.app.core.model.CampaignOpponent
import com.zeubicardgames.app.core.model.CardDefinition
import com.zeubicardgames.app.core.model.CardType
import com.zeubicardgames.app.core.model.EvolutionStage

object CampaignDeckFactoryV1 {
    fun build(opponent: CampaignOpponent, catalog: List<CardDefinition>): List<String> {
        val setCards = catalog.filter { it.setId == opponent.extensionId }
        require(setCards.isNotEmpty()) { "Extension IA introuvable : ${opponent.extensionId}" }

        val byId = setCards.associateBy { it.canonicalId }
        val boss = opponent.bossCardId?.let(byId::get)
            ?: setCards.firstOrNull { it.name == opponent.bossCardName }
        val chosen = mutableListOf<String>()
        val counts = mutableMapOf<String, Int>()

        fun add(card: CardDefinition, copies: Int = 1) {
            repeat(copies) {
                if (chosen.size >= DeckRules.DECK_SIZE) return@repeat
                val current = counts[card.canonicalId] ?: 0
                if (current < DeckRules.MAX_COPIES_PER_CARD) {
                    chosen += card.canonicalId
                    counts[card.canonicalId] = current + 1
                }
            }
        }

        fun lineage(card: CardDefinition): List<CardDefinition> {
            val reversed = mutableListOf<CardDefinition>()
            var current: CardDefinition? = card
            val visited = mutableSetOf<String>()
            while (current != null && visited.add(current.canonicalId)) {
                reversed += current
                current = current.evolvesFromId?.let(byId::get)
            }
            return reversed.asReversed()
        }

        boss?.let { lineage(it).forEach { card -> add(card, copies = 2) } }

        val bases = setCards
            .filter { it.type == CardType.PERSONNAGE && it.evolutionStage == EvolutionStage.BASE }
            .sortedWith(
                compareByDescending<CardDefinition> { card -> boss?.let { card.canonicalId in lineage(it).map(CardDefinition::canonicalId) } == true }
                    .thenByDescending { it.hp }
                    .thenBy { it.number },
            )
        bases.forEach { card ->
            if (chosen.count { id -> byId[id]?.evolutionStage == EvolutionStage.BASE } < 10) add(card, copies = 2)
        }

        val selectedBaseIds = chosen.mapNotNull(byId::get)
            .filter { it.evolutionStage == EvolutionStage.BASE }
            .mapTo(mutableSetOf(), CardDefinition::canonicalId)

        val compatibleEvolutions = setCards
            .filter { it.type == CardType.PERSONNAGE && it.evolutionStage != EvolutionStage.BASE }
            .filter { evolution ->
                var current = evolution
                var predecessor = current.evolvesFromId?.let(byId::get)
                while (predecessor != null && predecessor.evolutionStage != EvolutionStage.BASE) {
                    current = predecessor
                    predecessor = current.evolvesFromId?.let(byId::get)
                }
                predecessor?.canonicalId in selectedBaseIds
            }
            .sortedWith(compareByDescending<CardDefinition> { it.hp }.thenBy { it.number })
        compatibleEvolutions.forEach { add(it) }

        setCards
            .filter { it.type == CardType.ACTION || it.type == CardType.REPLIQUE }
            .sortedWith(compareByDescending<CardDefinition> { it.rarity.rank }.thenBy { it.number })
            .forEach { add(it) }

        setCards.sortedBy { it.number }.forEach { add(it) }
        setCards.sortedBy { it.number }.forEach { add(it) }

        require(chosen.size == DeckRules.DECK_SIZE) {
            "Impossible de générer ${DeckRules.DECK_SIZE} cartes pour ${opponent.name}."
        }
        val validation = DeckRules.validate(chosen, catalog)
        require(validation.isValid) { "Deck IA invalide : ${validation.errors.joinToString()}" }
        return chosen
    }
}
