package com.zeubicardgames.app.core.deck

import com.zeubicardgames.app.core.model.CardDefinition
import com.zeubicardgames.app.core.model.CardType
import com.zeubicardgames.app.core.model.EvolutionStage

data class DeckValidationResult(val errors: List<String>) {
    val isValid: Boolean get() = errors.isEmpty()
}

object DeckRules {
    const val DECK_SIZE = 20
    const val MAX_COPIES_PER_CARD = 2

    fun validate(
        ids: List<String>,
        catalog: List<CardDefinition>,
        ownedQuantities: Map<String, Int>? = null,
    ): DeckValidationResult {
        val errors = mutableListOf<String>()
        val byId = catalog.associateBy { it.canonicalId }
        val counts = ids.groupingBy { it }.eachCount()

        if (ids.size != DECK_SIZE) {
            errors += "Le deck contient ${ids.size}/$DECK_SIZE cartes."
        }

        val unknownIds = counts.keys.filterNot(byId::containsKey)
        if (unknownIds.isNotEmpty()) {
            errors += "${unknownIds.size} carte(s) du deck n’existent plus dans le catalogue."
        }

        val overLimit = counts.filterValues { it > MAX_COPIES_PER_CARD }
        if (overLimit.isNotEmpty()) {
            val names = overLimit.keys.map { byId[it]?.name ?: it }.sorted()
            errors += "Maximum $MAX_COPIES_PER_CARD exemplaires dépassé : ${names.joinToString()}."
        }

        val hasBaseCharacter = ids.any { id ->
            byId[id]?.let { card ->
                card.type == CardType.PERSONNAGE && card.evolutionStage == EvolutionStage.BASE
            } == true
        }
        if (!hasBaseCharacter) {
            errors += "Ajoute au moins un Personnage en forme initiale."
        }

        if (ownedQuantities != null) {
            val insufficient = counts.mapNotNull { (id, required) ->
                val owned = ownedQuantities[id] ?: 0
                if (required > owned) {
                    val name = byId[id]?.name ?: id
                    "$name ($required requis, $owned possédé)"
                } else null
            }
            if (insufficient.isNotEmpty()) {
                errors += "Cartes insuffisantes dans la collection : ${insufficient.joinToString()}."
            }
        }

        return DeckValidationResult(errors.distinct())
    }
}
