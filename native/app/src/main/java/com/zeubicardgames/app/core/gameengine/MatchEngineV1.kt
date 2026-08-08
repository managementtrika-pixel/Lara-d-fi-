package com.zeubicardgames.app.core.gameengine

import com.zeubicardgames.app.core.model.Attack
import com.zeubicardgames.app.core.model.CardDefinition
import com.zeubicardgames.app.core.model.CardType
import com.zeubicardgames.app.core.model.EvolutionStage
import kotlin.random.Random

const val MATCH_STARTING_HAND = 7
const val MATCH_MAX_RESERVES = 2
const val MATCH_MAX_SUPPORT_SLOTS = 3
const val MATCH_POINTS_TO_WIN = 3

enum class MatchSide { PLAYER, OPPONENT }
enum class MatchPhase { SETUP, MAIN, RESOLVING, PROMOTION, FINISHED }
enum class CharacterZone { ACTIVE, RESERVE_1, RESERVE_2 }

enum class MatchEventType {
    MATCH_STARTED,
    DECK_SHUFFLED,
    CARD_DRAWN,
    CHARACTER_PLAYED,
    RESOURCE_ATTACHED,
    CARD_EVOLVED,
    ACTION_USED,
    REPLY_ARMED,
    ATTACK_DECLARED,
    DAMAGE_APPLIED,
    CARD_KNOCKED_OUT,
    CHARACTER_PROMOTED,
    TURN_STARTED,
    TURN_ENDED,
    MATCH_FINISHED,
    COMMAND_REJECTED,
}

data class MatchEvent(
    val index: Int,
    val type: MatchEventType,
    val side: MatchSide?,
    val cardId: String? = null,
    val message: String,
)

data class CharacterInPlay(
    val instanceId: String,
    val cardId: String,
    val damage: Int = 0,
    val resources: Int = 0,
    val enteredTurn: Int = 0,
    val evolvedThisTurn: Boolean = false,
) {
    fun hpLeft(catalog: Map<String, CardDefinition>): Int =
        ((catalog[cardId]?.hp ?: 0) - damage).coerceAtLeast(0)
}

data class SideState(
    val deck: List<String>,
    val hand: List<String> = emptyList(),
    val discard: List<String> = emptyList(),
    val active: CharacterInPlay? = null,
    val reserves: List<CharacterInPlay> = emptyList(),
    val supportSlots: List<String> = emptyList(),
    val points: Int = 0,
)

data class TurnFlags(
    val resourceAttached: Boolean = false,
    val attackDeclared: Boolean = false,
)

data class MatchStateV1(
    val schemaVersion: Int = 1,
    val seed: Long,
    val turnNumber: Int = 1,
    val activeSide: MatchSide = MatchSide.PLAYER,
    val phase: MatchPhase = MatchPhase.SETUP,
    val player: SideState,
    val opponent: SideState,
    val flags: TurnFlags = TurnFlags(),
    val winner: MatchSide? = null,
    val events: List<MatchEvent> = emptyList(),
)

sealed interface MatchCommandV1 {
    data class PlayCharacter(val cardId: String, val zone: CharacterZone) : MatchCommandV1
    data class AttachResource(val instanceId: String) : MatchCommandV1
    data class Evolve(val evolutionCardId: String, val targetInstanceId: String) : MatchCommandV1
    data class ArmReply(val cardId: String) : MatchCommandV1
    data class UseAction(val cardId: String) : MatchCommandV1
    data class Attack(val attackIndex: Int) : MatchCommandV1
    data object EndTurn : MatchCommandV1
}

class MatchEngineV1(
    cards: List<CardDefinition>,
    private val seed: Long,
) {
    private val catalog = cards.associateBy { it.canonicalId }
    private val random = Random(seed)
    private var instanceCounter = 0L

    fun start(playerDeck: List<String>, opponentDeck: List<String>): MatchStateV1 {
        require(playerDeck.all(catalog::containsKey)) { "Le deck joueur contient une carte inconnue." }
        require(opponentDeck.all(catalog::containsKey)) { "Le deck adverse contient une carte inconnue." }

        val shuffledPlayer = playerDeck.shuffled(random)
        val shuffledOpponent = opponentDeck.shuffled(random)
        val player = drawInitialHand(SideState(deck = shuffledPlayer), MatchSide.PLAYER)
        val opponent = drawInitialHand(SideState(deck = shuffledOpponent), MatchSide.OPPONENT)
        val baseEvents = mutableListOf<MatchEvent>()
        append(baseEvents, MatchEventType.MATCH_STARTED, null, null, "Match démarré")
        append(baseEvents, MatchEventType.DECK_SHUFFLED, MatchSide.PLAYER, null, "Deck PLAYER mélangé")
        append(baseEvents, MatchEventType.DECK_SHUFFLED, MatchSide.OPPONENT, null, "Deck adverse mélangé")
        player.hand.forEach { append(baseEvents, MatchEventType.CARD_DRAWN, MatchSide.PLAYER, it, "Carte piochée") }
        opponent.hand.forEach { append(baseEvents, MatchEventType.CARD_DRAWN, MatchSide.OPPONENT, it, "Carte adverse piochée") }
        append(baseEvents, MatchEventType.TURN_STARTED, MatchSide.PLAYER, null, "Tour 1 — PLAYER")
        return MatchStateV1(
            seed = seed,
            phase = MatchPhase.SETUP,
            player = player,
            opponent = opponent,
            events = baseEvents,
        )
    }

    fun apply(state: MatchStateV1, command: MatchCommandV1): MatchStateV1 {
        if (state.phase == MatchPhase.FINISHED) return reject(state, "Le match est terminé.")
        return when (command) {
            is MatchCommandV1.PlayCharacter -> playCharacter(state, command)
            is MatchCommandV1.AttachResource -> attachResource(state, command)
            is MatchCommandV1.Evolve -> evolve(state, command)
            is MatchCommandV1.ArmReply -> armReply(state, command)
            is MatchCommandV1.UseAction -> useAction(state, command)
            is MatchCommandV1.Attack -> attack(state, command)
            MatchCommandV1.EndTurn -> endTurn(state)
        }
    }

    private fun playCharacter(state: MatchStateV1, command: MatchCommandV1): MatchStateV1 {
        if (state.phase !in setOf(MatchPhase.SETUP, MatchPhase.MAIN)) return reject(state, "Impossible de poser un Personnage maintenant.")
        val side = currentSide(state)
        if (command.cardId !in side.hand) return reject(state, "Cette carte n’est pas dans la main.")
        val card = catalog[command.cardId] ?: return reject(state, "Carte inconnue.")
        if (card.type != CardType.PERSONNAGE || card.evolutionStage != EvolutionStage.BASE) {
            return reject(state, "Seule une forme initiale peut être posée directement.")
        }

        val fighter = CharacterInPlay(
            instanceId = nextInstanceId(state.activeSide),
            cardId = card.canonicalId,
            enteredTurn = state.turnNumber,
        )
        val updated = when (command.zone) {
            CharacterZone.ACTIVE -> {
                if (side.active != null) return reject(state, "L’emplacement actif est déjà occupé.")
                side.copy(active = fighter, hand = side.hand.removeOne(command.cardId))
            }
            CharacterZone.RESERVE_1, CharacterZone.RESERVE_2 -> {
                if (side.reserves.size >= MATCH_MAX_RESERVES) return reject(state, "Les deux réserves sont occupées.")
                side.copy(reserves = side.reserves + fighter, hand = side.hand.removeOne(command.cardId))
            }
        }
        val nextPhase = if (state.phase == MatchPhase.SETUP && updated.active != null) MatchPhase.MAIN else state.phase
        return replaceCurrentSide(state, updated).withEvent(
            MatchEventType.CHARACTER_PLAYED,
            state.activeSide,
            card.canonicalId,
            "${card.name} rejoint ${command.zone.name.lowercase()}.",
        ).copy(phase = nextPhase)
    }

    private fun attachResource(state: MatchStateV1, command: MatchCommandV1): MatchStateV1 {
        if (state.phase != MatchPhase.MAIN) return reject(state, "Les ressources s’attachent pendant la phase principale.")
        if (state.flags.resourceAttached) return reject(state, "Une ressource a déjà été attachée ce tour.")
        val side = currentSide(state)
        val updated = side.updateCharacter(command.instanceId) { it.copy(resources = it.resources + 1) }
            ?: return reject(state, "Personnage cible introuvable.")
        return replaceCurrentSide(state, updated)
            .copy(flags = state.flags.copy(resourceAttached = true))
            .withEvent(MatchEventType.RESOURCE_ATTACHED, state.activeSide, updated.findCharacter(command.instanceId)?.cardId, "Ressource attachée.")
    }

    private fun evolve(state: MatchStateV1, command: MatchCommandV1): MatchStateV1 {
        if (state.phase != MatchPhase.MAIN) return reject(state, "Une évolution se joue pendant la phase principale.")
        val side = currentSide(state)
        if (command.evolutionCardId !in side.hand) return reject(state, "L’évolution n’est pas dans la main.")
        val evolution = catalog[command.evolutionCardId] ?: return reject(state, "Évolution inconnue.")
        if (evolution.type != CardType.PERSONNAGE || evolution.evolutionStage == EvolutionStage.BASE) {
            return reject(state, "Cette carte n’est pas une évolution.")
        }
        val target = side.findCharacter(command.targetInstanceId) ?: return reject(state, "Personnage cible introuvable.")
        if (evolution.evolvesFromId != target.cardId) return reject(state, "Cette évolution ne correspond pas au Personnage ciblé.")
        if (target.enteredTurn >= state.turnNumber || target.evolvedThisTurn) return reject(state, "Ce Personnage ne peut pas encore évoluer ce tour.")
        val oldHp = catalog[target.cardId]?.hp ?: 0
        val newHp = evolution.hp
        val preservedDamage = target.damage.coerceAtMost(newHp)
        val evolved = target.copy(cardId = evolution.canonicalId, damage = preservedDamage, evolvedThisTurn = true)
        val updatedSide = side.updateCharacter(command.targetInstanceId) { evolved }
            ?.copy(hand = side.hand.removeOne(command.evolutionCardId), discard = side.discard + target.cardId)
            ?: return reject(state, "Évolution impossible.")
        return replaceCurrentSide(state, updatedSide).withEvent(
            MatchEventType.CARD_EVOLVED,
            state.activeSide,
            evolution.canonicalId,
            "Évolution : ${catalog[target.cardId]?.name ?: target.cardId} → ${evolution.name} (${oldHp}→${newHp} PV max).",
        )
    }

    private fun armReply(state: MatchStateV1, command: MatchCommandV1): MatchStateV1 {
        if (state.phase != MatchPhase.MAIN) return reject(state, "Une Réplique s’arme pendant la phase principale.")
        val side = currentSide(state)
        if (side.supportSlots.size >= MATCH_MAX_SUPPORT_SLOTS) return reject(state, "Les trois emplacements Action/Réplique sont occupés.")
        if (command.cardId !in side.hand) return reject(state, "Cette Réplique n’est pas dans la main.")
        val card = catalog[command.cardId] ?: return reject(state, "Carte inconnue.")
        if (card.type != CardType.REPLIQUE) return reject(state, "Cette carte n’est pas une Réplique.")
        val updated = side.copy(
            hand = side.hand.removeOne(command.cardId),
            supportSlots = side.supportSlots + command.cardId,
        )
        return replaceCurrentSide(state, updated).withEvent(
            MatchEventType.REPLY_ARMED,
            state.activeSide,
            command.cardId,
            "Réplique armée.",
        )
    }

    private fun useAction(state: MatchStateV1, command: MatchCommandV1): MatchStateV1 {
        if (state.phase != MatchPhase.MAIN) return reject(state, "Une Action se joue pendant la phase principale.")
        val side = currentSide(state)
        if (command.cardId !in side.hand) return reject(state, "Cette Action n’est pas dans la main.")
        val card = catalog[command.cardId] ?: return reject(state, "Carte inconnue.")
        if (card.type != CardType.ACTION) return reject(state, "Cette carte n’est pas une Action.")
        val updated = side.copy(hand = side.hand.removeOne(command.cardId), discard = side.discard + command.cardId)
        return replaceCurrentSide(state, updated).withEvent(
            MatchEventType.ACTION_USED,
            state.activeSide,
            command.cardId,
            "Action jouée : ${card.name}. L’effet sera résolu par l’interpréteur d’effets.",
        )
    }

    private fun attack(state: MatchStateV1, command: MatchCommandV1): MatchStateV1 {
        if (state.phase != MatchPhase.MAIN) return reject(state, "Impossible d’attaquer maintenant.")
        if (state.flags.attackDeclared) return reject(state, "Une attaque a déjà été déclarée ce tour.")
        val attackerSide = currentSide(state)
        val defenderSide = otherSide(state)
        val attacker = attackerSide.active ?: return reject(state, "Aucun Personnage actif.")
        val defender = defenderSide.active ?: return reject(state, "Aucun Personnage adverse actif.")
        val attackerCard = catalog[attacker.cardId] ?: return reject(state, "Carte active inconnue.")
        val attack: Attack = attackerCard.attacks.getOrNull(command.attackIndex)
            ?: return reject(state, "Attaque inconnue.")
        if (attacker.resources < attack.cost) return reject(state, "Ressources insuffisantes pour ${attack.name}.")

        var next = state.copy(phase = MatchPhase.RESOLVING, flags = state.flags.copy(attackDeclared = true))
            .withEvent(MatchEventType.ATTACK_DECLARED, state.activeSide, attacker.cardId, attack.name)

        val hit = defender.copy(damage = defender.damage + attack.damage)
        var newDefenderSide = defenderSide.copy(active = hit)
        next = replaceOtherSide(next, newDefenderSide).withEvent(
            MatchEventType.DAMAGE_APPLIED,
            opposite(state.activeSide),
            defender.cardId,
            "${attack.damage} dégâts appliqués.",
        )

        if (hit.hpLeft(catalog) <= 0) {
            newDefenderSide = newDefenderSide.copy(
                active = null,
                discard = newDefenderSide.discard + defender.cardId,
            )
            var scoringSide = currentSide(next).copy(points = currentSide(next).points + 1)
            next = replaceCurrentSide(replaceOtherSide(next, newDefenderSide), scoringSide)
                .withEvent(MatchEventType.CARD_KNOCKED_OUT, opposite(state.activeSide), defender.cardId, "Personnage K.O.")

            if (scoringSide.points >= MATCH_POINTS_TO_WIN) {
                return next.copy(phase = MatchPhase.FINISHED, winner = state.activeSide)
                    .withEvent(MatchEventType.MATCH_FINISHED, state.activeSide, null, "Victoire ${state.activeSide}.")
            }
            if (newDefenderSide.reserves.isNotEmpty()) {
                val promoted = newDefenderSide.reserves.first()
                newDefenderSide = newDefenderSide.copy(active = promoted, reserves = newDefenderSide.reserves.drop(1))
                next = replaceOtherSide(next, newDefenderSide).withEvent(
                    MatchEventType.CHARACTER_PROMOTED,
                    opposite(state.activeSide),
                    promoted.cardId,
                    "Une réserve devient active.",
                )
            } else {
                return next.copy(phase = MatchPhase.FINISHED, winner = state.activeSide)
                    .withEvent(MatchEventType.MATCH_FINISHED, state.activeSide, null, "Victoire : plus aucun Personnage adverse en jeu.")
            }
        }
        return endTurn(next.copy(phase = MatchPhase.MAIN))
    }

    private fun endTurn(state: MatchStateV1): MatchStateV1 {
        if (state.phase !in setOf(MatchPhase.MAIN, MatchPhase.RESOLVING)) return reject(state, "Impossible de terminer le tour maintenant.")
        val endingSide = state.activeSide
        val nextSide = opposite(endingSide)
        var next = state.withEvent(MatchEventType.TURN_ENDED, endingSide, null, "Fin du tour.")
        val newTurn = state.turnNumber + 1
        next = next.copy(
            activeSide = nextSide,
            turnNumber = newTurn,
            phase = MatchPhase.MAIN,
            flags = TurnFlags(),
            player = next.player.resetTurnFlags(),
            opponent = next.opponent.resetTurnFlags(),
        )
        next = drawForTurn(next, nextSide)
        return next.withEvent(MatchEventType.TURN_STARTED, nextSide, null, "Tour $newTurn — $nextSide")
    }

    private fun drawInitialHand(side: SideState): SideState {
        val count = minOf(MATCH_STARTING_HAND, side.deck.size)
        return side.copy(hand = side.deck.take(count), deck = side.deck.drop(count))
    }

    private fun drawForTurn(state: MatchStateV1, side: MatchSide): MatchStateV1 {
        val current = if (side == MatchSide.PLAYER) state.player else state.opponent
        val card = current.deck.firstOrNull() ?: return state
        val updated = current.copy(deck = current.deck.drop(1), hand = current.hand + card)
        return if (side == MatchSide.PLAYER) state.copy(player = updated).withEvent(MatchEventType.CARD_DRAWN, side, card, "Carte piochée")
        else state.copy(opponent = updated).withEvent(MatchEventType.CARD_DRAWN, side, card, "Carte piochée")
    }

    private fun currentSide(state: MatchStateV1): SideState = if (state.activeSide == MatchSide.PLAYER) state.player else state.opponent
    private fun otherSide(state: MatchStateV1): SideState = if (state.activeSide == MatchSide.PLAYER) state.opponent else state.player
    private fun replaceCurrentSide(state: MatchStateV1, side: SideState): MatchStateV1 =
        if (state.activeSide == MatchSide.PLAYER) state.copy(player = side) else state.copy(opponent = side)
    private fun replaceOtherSide(state: MatchStateV1, side: SideState): MatchStateV1 =
        if (state.activeSide == MatchSide.PLAYER) state.copy(opponent = side) else state.copy(player = side)

    private fun reject(state: MatchStateV1, reason: String): MatchStateV1 =
        state.withEvent(MatchEventType.COMMAND_REJECTED, state.activeSide, null, reason)

    private fun nextInstanceId(side: MatchSide): String = "${if (side == MatchSide.PLAYER) "p" else "o"}-${seed}-${++instanceCounter}"

    private fun append(events: MutableList<MatchEvent>, type: MatchEventType, side: MatchSide?, cardId: String?, message: String) {
        events += MatchEvent(events.size, type, side, cardId, message)
    }

    private fun MatchStateV1.withEvent(type: MatchEventType, side: MatchSide?, cardId: String?, message: String): MatchStateV1 =
        copy(events = events + MatchEvent(events.size, type, side, cardId, message))

    private fun SideState.findCharacter(instanceId: String): CharacterInPlay? =
        active?.takeIf { it.instanceId == instanceId } ?: reserves.firstOrNull { it.instanceId == instanceId }

    private fun SideState.updateCharacter(instanceId: String, transform: (CharacterInPlay) -> CharacterInPlay): SideState? {
        if (active?.instanceId == instanceId) return copy(active = transform(active))
        val index = reserves.indexOfFirst { it.instanceId == instanceId }
        if (index < 0) return null
        return copy(reserves = reserves.toMutableList().also { it[index] = transform(it[index]) })
    }

    private fun SideState.resetTurnFlags(): SideState = copy(
        active = active?.copy(evolvedThisTurn = false),
        reserves = reserves.map { it.copy(evolvedThisTurn = false) },
    )

    private fun List<String>.removeOne(value: String): List<String> {
        val index = indexOf(value)
        if (index < 0) return this
        return toMutableList().also { it.removeAt(index) }
    }

    private fun opposite(side: MatchSide): MatchSide = if (side == MatchSide.PLAYER) MatchSide.OPPONENT else MatchSide.PLAYER
}
