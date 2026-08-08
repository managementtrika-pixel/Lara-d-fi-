package com.zeubicardgames.app.core.effects

import com.zeubicardgames.app.core.gameengine.CharacterInPlay
import com.zeubicardgames.app.core.gameengine.MatchEvent
import com.zeubicardgames.app.core.gameengine.MatchEventType
import com.zeubicardgames.app.core.gameengine.MatchPhase
import com.zeubicardgames.app.core.gameengine.MatchSide
import com.zeubicardgames.app.core.gameengine.MatchStateV1
import com.zeubicardgames.app.core.gameengine.SideState
import com.zeubicardgames.app.core.model.CardDefinition
import com.zeubicardgames.app.core.model.CardType
import com.zeubicardgames.app.core.model.EvolutionStage

data class ActionEffectChoiceV1(
    val targetInstanceId: String? = null,
    val selectedDeckCardId: String? = null,
    val fromInstanceId: String? = null,
    val toInstanceId: String? = null,
    val resourceCount: Int = 0,
)

data class ActionEffectResultV1(
    val state: MatchStateV1,
    val success: Boolean,
    val reason: String,
)

/**
 * Résolveur V1 volontairement limité aux Actions dont le texte source est
 * suffisamment précis et dont les choix peuvent être représentés sans
 * inventer de règle supplémentaire.
 *
 * Une résolution invalide ne consomme jamais la carte Action.
 */
class ActionEffectResolverV1(cards: List<CardDefinition>) {
    private val catalog = cards.associateBy(CardDefinition::canonicalId)

    fun resolve(
        state: MatchStateV1,
        side: MatchSide,
        actionCardId: String,
        choice: ActionEffectChoiceV1 = ActionEffectChoiceV1(),
    ): ActionEffectResultV1 {
        if (state.phase != MatchPhase.MAIN) return failure(state, "Une Action se joue pendant la phase principale.")
        if (state.activeSide != side) return failure(state, "Ce n’est pas le tour de ce camp.")

        val action = catalog[actionCardId] ?: return failure(state, "Action inconnue.")
        if (action.type != CardType.ACTION) return failure(state, "Cette carte n’est pas une Action.")
        val owner = sideState(state, side)
        if (actionCardId !in owner.hand) return failure(state, "Cette Action n’est pas dans la main.")

        val semantic = SemanticEffectRegistry.find(action)
            ?: return failure(state, "L’effet de ${action.name} n’est pas encore vérifié.")
        if (semantic.verification == EffectVerification.RULE_CONFLICT) {
            return failure(state, "${action.name} dépend d’une règle encore à normaliser.")
        }
        if (semantic.verification != EffectVerification.VERIFIED) {
            return failure(state, "L’effet de ${action.name} n’est pas exécutable.")
        }

        return when (semantic.cardKey) {
            "dbz:019" -> heal80(state, side, action, choice)
            "ninja:021" -> searchEvolution(state, side, action, choice)
            "ninja:023" -> moveResources(state, side, action, choice)
            else -> failure(
                state,
                "L’effet de ${action.name} est vérifié mais son exécuteur n’est pas encore livré.",
            )
        }
    }

    private fun heal80(
        state: MatchStateV1,
        side: MatchSide,
        action: CardDefinition,
        choice: ActionEffectChoiceV1,
    ): ActionEffectResultV1 {
        val targetId = choice.targetInstanceId
            ?: return failure(state, "Choisis le Personnage à soigner.")
        val owner = sideState(state, side)
        val target = owner.findCharacter(targetId)
            ?: return failure(state, "Le Personnage ciblé ne t’appartient pas.")
        val healed = target.copy(damage = (target.damage - 80).coerceAtLeast(0))
        val updatedOwner = owner.updateCharacter(targetId, healed)
            ?: return failure(state, "Impossible de soigner cette cible.")
        val next = consumeAction(state, side, updatedOwner, action, "${action.name} soigne 80 PV.")
        return success(next, "80 PV soignés.")
    }

    private fun searchEvolution(
        state: MatchStateV1,
        side: MatchSide,
        action: CardDefinition,
        choice: ActionEffectChoiceV1,
    ): ActionEffectResultV1 {
        val selectedId = choice.selectedDeckCardId
            ?: return failure(state, "Choisis une Évolution dans ton deck.")
        val owner = sideState(state, side)
        if (owner.hand.size >= 10) return failure(state, "La main est pleine.")
        if (selectedId !in owner.deck) return failure(state, "Cette carte n’est pas dans ton deck.")
        val selected = catalog[selectedId] ?: return failure(state, "Carte recherchée inconnue.")
        if (selected.type != CardType.PERSONNAGE || selected.evolutionStage == EvolutionStage.BASE) {
            return failure(state, "Parchemin interdit ne peut chercher qu’une Évolution.")
        }

        val updatedOwner = owner.copy(
            deck = owner.deck.removeOne(selectedId),
            hand = owner.hand + selectedId,
        )
        val next = consumeAction(
            state,
            side,
            updatedOwner,
            action,
            "${action.name} ajoute ${selected.name} à la main.",
        )
        return success(next, "${selected.name} ajoutée à la main.")
    }

    private fun moveResources(
        state: MatchStateV1,
        side: MatchSide,
        action: CardDefinition,
        choice: ActionEffectChoiceV1,
    ): ActionEffectResultV1 {
        val fromId = choice.fromInstanceId
            ?: return failure(state, "Choisis le Personnage qui donne les ressources.")
        val toId = choice.toInstanceId
            ?: return failure(state, "Choisis le Personnage qui reçoit les ressources.")
        if (fromId == toId) return failure(state, "Les deux Personnages doivent être différents.")
        if (choice.resourceCount !in 1..2) return failure(state, "Tu peux déplacer 1 ou 2 ressources.")

        val owner = sideState(state, side)
        val from = owner.findCharacter(fromId)
            ?: return failure(state, "Le Personnage source ne t’appartient pas.")
        val to = owner.findCharacter(toId)
            ?: return failure(state, "Le Personnage cible ne t’appartient pas.")
        if (from.resources < choice.resourceCount) {
            return failure(state, "Le Personnage source n’a pas assez de ressources.")
        }

        var updatedOwner = owner.updateCharacter(
            fromId,
            from.copy(resources = from.resources - choice.resourceCount),
        ) ?: return failure(state, "Impossible de déplacer les ressources.")
        updatedOwner = updatedOwner.updateCharacter(
            toId,
            to.copy(resources = to.resources + choice.resourceCount),
        ) ?: return failure(state, "Impossible de déplacer les ressources.")

        val next = consumeAction(
            state,
            side,
            updatedOwner,
            action,
            "${action.name} déplace ${choice.resourceCount} ressource(s).",
        )
        return success(next, "${choice.resourceCount} ressource(s) déplacée(s).")
    }

    private fun consumeAction(
        state: MatchStateV1,
        side: MatchSide,
        ownerAfterEffect: SideState,
        action: CardDefinition,
        message: String,
    ): MatchStateV1 {
        val consumed = ownerAfterEffect.copy(
            hand = ownerAfterEffect.hand.removeOne(action.canonicalId),
            discard = ownerAfterEffect.discard + action.canonicalId,
        )
        val replaced = replaceSide(state, side, consumed)
        return replaced.copy(
            events = replaced.events + MatchEvent(
                index = replaced.events.size,
                type = MatchEventType.ACTION_USED,
                side = side,
                cardId = action.canonicalId,
                message = message,
            )
        )
    }

    private fun sideState(state: MatchStateV1, side: MatchSide): SideState =
        if (side == MatchSide.PLAYER) state.player else state.opponent

    private fun replaceSide(state: MatchStateV1, side: MatchSide, value: SideState): MatchStateV1 =
        if (side == MatchSide.PLAYER) state.copy(player = value) else state.copy(opponent = value)

    private fun SideState.findCharacter(instanceId: String): CharacterInPlay? =
        active?.takeIf { it.instanceId == instanceId }
            ?: reserves.firstOrNull { it.instanceId == instanceId }

    private fun SideState.updateCharacter(instanceId: String, replacement: CharacterInPlay): SideState? {
        if (active?.instanceId == instanceId) return copy(active = replacement)
        val index = reserves.indexOfFirst { it.instanceId == instanceId }
        if (index < 0) return null
        return copy(reserves = reserves.toMutableList().also { it[index] = replacement })
    }

    private fun List<String>.removeOne(value: String): List<String> {
        val index = indexOf(value)
        if (index < 0) return this
        return toMutableList().also { it.removeAt(index) }
    }

    private fun success(state: MatchStateV1, reason: String) =
        ActionEffectResultV1(state = state, success = true, reason = reason)

    private fun failure(state: MatchStateV1, reason: String) =
        ActionEffectResultV1(state = state, success = false, reason = reason)
}
