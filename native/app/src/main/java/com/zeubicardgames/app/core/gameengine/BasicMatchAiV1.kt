package com.zeubicardgames.app.core.gameengine

import com.zeubicardgames.app.core.model.CardDefinition
import com.zeubicardgames.app.core.model.CardType
import com.zeubicardgames.app.core.model.EvolutionStage

/**
 * Première IA déterministe de ZeubiCardGames.
 *
 * Elle n'utilise que les informations autorisées : sa main, son terrain et les
 * informations publiques du plateau. Elle ne lit jamais la main du joueur ni
 * l'ordre futur de son deck.
 */
class BasicMatchAiV1(
    cards: List<CardDefinition>,
    private val engine: MatchEngineV1,
) {
    private val catalog = cards.associateBy { it.canonicalId }

    fun playUntilPlayerDecision(initial: MatchStateV1): MatchStateV1 {
        var state = initial

        if (state.phase == MatchPhase.PROMOTION && state.pendingPromotionSide == MatchSide.OPPONENT) {
            val reserve = state.opponent.reserves.maxByOrNull { catalog[it.cardId]?.hp ?: 0 }
                ?: return state
            state = engine.apply(state, MatchCommandV1.Promote(reserve.instanceId))
        }

        if (state.phase == MatchPhase.SETUP && state.activeSide == MatchSide.OPPONENT) {
            state = setup(state)
        }

        if (state.phase != MatchPhase.MAIN || state.activeSide != MatchSide.OPPONENT) {
            return state
        }

        state = fillReserves(state)
        state = evolveOneCharacter(state)

        val active = state.opponent.active
        if (active != null && !state.flags.resourceAttached) {
            state = engine.apply(state, MatchCommandV1.AttachResource(active.instanceId))
        }

        val currentActive = state.opponent.active
        val definition = currentActive?.let { catalog[it.cardId] }
        val affordableAttack = definition?.attacks
            ?.withIndex()
            ?.filter { (_, attack) -> (currentActive.resources >= attack.cost) }
            ?.maxWithOrNull(compareBy<IndexedValue<com.zeubicardgames.app.core.model.Attack>> { it.value.damage }
                .thenByDescending { -it.value.cost })

        return if (affordableAttack != null) {
            engine.apply(state, MatchCommandV1.Attack(affordableAttack.index))
        } else {
            engine.apply(state, MatchCommandV1.EndTurn)
        }
    }

    private fun setup(initial: MatchStateV1): MatchStateV1 {
        var state = initial
        val baseCards = state.opponent.hand.mapNotNull(catalog::get)
            .filter { it.type == CardType.PERSONNAGE && it.evolutionStage == EvolutionStage.BASE }
            .sortedWith(compareByDescending<CardDefinition> { it.hp }.thenBy { it.number })

        val activeCard = baseCards.firstOrNull() ?: return state
        state = engine.apply(state, MatchCommandV1.PlayCharacter(activeCard.canonicalId, CharacterZone.ACTIVE))

        baseCards.drop(1).take(MATCH_MAX_RESERVES).forEach { card ->
            state = engine.apply(state, MatchCommandV1.PlayCharacter(card.canonicalId, CharacterZone.RESERVE_1))
        }
        return engine.apply(state, MatchCommandV1.CompleteSetup)
    }

    private fun fillReserves(initial: MatchStateV1): MatchStateV1 {
        var state = initial
        while (state.opponent.reserves.size < MATCH_MAX_RESERVES) {
            val card = state.opponent.hand.mapNotNull(catalog::get).firstOrNull {
                it.type == CardType.PERSONNAGE && it.evolutionStage == EvolutionStage.BASE
            } ?: break
            val before = state.events.size
            state = engine.apply(state, MatchCommandV1.PlayCharacter(card.canonicalId, CharacterZone.RESERVE_1))
            if (state.events.size == before || state.events.last().type == MatchEventType.COMMAND_REJECTED) break
        }
        return state
    }

    private fun evolveOneCharacter(initial: MatchStateV1): MatchStateV1 {
        if (initial.turnNumber == 1) return initial
        val targets = listOfNotNull(initial.opponent.active) + initial.opponent.reserves
        val options = initial.opponent.hand.mapNotNull(catalog::get)
            .filter { it.type == CardType.PERSONNAGE && it.evolutionStage != EvolutionStage.BASE }
            .mapNotNull { evolution ->
                val target = targets.firstOrNull { it.cardId == evolution.evolvesFromId } ?: return@mapNotNull null
                evolution to target
            }
            .sortedByDescending { (evolution, _) -> evolution.hp }

        for ((evolution, target) in options) {
            val next = engine.apply(
                initial,
                MatchCommandV1.Evolve(evolution.canonicalId, target.instanceId),
            )
            if (next.events.lastOrNull()?.type != MatchEventType.COMMAND_REJECTED) return next
        }
        return initial
    }
}
