package com.zeubicardgames.app.core.gameengine

import com.zeubicardgames.app.core.effects.ActionEffectChoiceV1
import com.zeubicardgames.app.core.effects.ActionEffectResolverV1
import com.zeubicardgames.app.core.effects.ReplyEffectResolverV1
import com.zeubicardgames.app.core.effects.ReplyResolutionInputV1
import com.zeubicardgames.app.core.effects.ReplyWindowV1
import com.zeubicardgames.app.core.model.Attack
import com.zeubicardgames.app.core.model.CardDefinition
import com.zeubicardgames.app.core.model.CardType
import com.zeubicardgames.app.core.model.EvolutionStage
import kotlin.random.Random

const val MATCH_STARTING_HAND = 5
const val MATCH_MAX_HAND = 10
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
    DRAW_SKIPPED,
    CHARACTER_PLAYED,
    SETUP_COMPLETED,
    RESOURCE_ATTACHED,
    CHARACTER_RETREATED,
    CARD_EVOLVED,
    ACTION_USED,
    REPLY_ARMED,
    REACTION_REQUIRED,
    REACTION_PASSED,
    REPLY_TRIGGERED,
    ATTACK_DECLARED,
    DAMAGE_CALCULATED,
    DAMAGE_APPLIED,
    EFFECT_APPLIED,
    CARD_KNOCKED_OUT,
    PROMOTION_REQUIRED,
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
    val evolutionStack: List<String> = emptyList(),
) {
    fun hpLeft(catalog: Map<String, CardDefinition>): Int =
        ((catalog[cardId]?.hp ?: 0) - damage).coerceAtLeast(0)

    fun allCardIds(): List<String> = evolutionStack + cardId
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
    val retreatUsed: Boolean = false,
    val attackDeclared: Boolean = false,
)

data class PendingAttackV1(
    val attackerSide: MatchSide,
    val attackerInstanceId: String,
    val defenderInstanceId: String,
    val attackIndex: Int,
    val attackName: String,
    val incomingDamage: Int,
    val defenderDamageOverride: Int? = null,
)

data class PendingReactionV1(
    val side: MatchSide,
    val window: ReplyWindowV1,
    val eligibleCardIds: List<String>,
)

data class MatchStateV1(
    val schemaVersion: Int = 2,
    val seed: Long,
    val turnNumber: Int = 1,
    val activeSide: MatchSide = MatchSide.PLAYER,
    val phase: MatchPhase = MatchPhase.SETUP,
    val player: SideState,
    val opponent: SideState,
    val playerSetupComplete: Boolean = false,
    val opponentSetupComplete: Boolean = false,
    val pendingPromotionSide: MatchSide? = null,
    val pendingAttack: PendingAttackV1? = null,
    val pendingReaction: PendingReactionV1? = null,
    val flags: TurnFlags = TurnFlags(),
    val winner: MatchSide? = null,
    val events: List<MatchEvent> = emptyList(),
)

sealed interface MatchCommandV1 {
    data class PlayCharacter(val cardId: String, val zone: CharacterZone) : MatchCommandV1
    data object CompleteSetup : MatchCommandV1
    data class AttachResource(val instanceId: String) : MatchCommandV1
    data class Retreat(val reserveInstanceId: String) : MatchCommandV1
    data class Evolve(val evolutionCardId: String, val targetInstanceId: String) : MatchCommandV1
    data class ArmReply(val cardId: String) : MatchCommandV1
    data class UseAction(
        val cardId: String,
        val choice: ActionEffectChoiceV1 = ActionEffectChoiceV1(),
    ) : MatchCommandV1
    data class Attack(val attackIndex: Int) : MatchCommandV1
    data class ResolveReaction(val cardId: String? = null) : MatchCommandV1
    data class Promote(val reserveInstanceId: String) : MatchCommandV1
    data object EndTurn : MatchCommandV1
}

class MatchEngineV1(
    cards: List<CardDefinition>,
    private val seed: Long,
) {
    private val catalog = cards.associateBy { it.canonicalId }
    private val random = Random(seed)
    private val actionResolver = ActionEffectResolverV1(cards)
    private val replyResolver = ReplyEffectResolverV1(cards)
    private var instanceCounter = 0L

    fun start(playerDeck: List<String>, opponentDeck: List<String>): MatchStateV1 {
        require(playerDeck.all(catalog::containsKey)) { "Le deck joueur contient une carte inconnue." }
        require(opponentDeck.all(catalog::containsKey)) { "Le deck adverse contient une carte inconnue." }

        val shuffledPlayer = playerDeck.shuffled(random)
        val shuffledOpponent = opponentDeck.shuffled(random)
        val player = drawInitialHand(SideState(deck = shuffledPlayer))
        val opponent = drawInitialHand(SideState(deck = shuffledOpponent))
        val baseEvents = mutableListOf<MatchEvent>()
        append(baseEvents, MatchEventType.MATCH_STARTED, null, null, "Match démarré")
        append(baseEvents, MatchEventType.DECK_SHUFFLED, MatchSide.PLAYER, null, "Deck PLAYER mélangé")
        append(baseEvents, MatchEventType.DECK_SHUFFLED, MatchSide.OPPONENT, null, "Deck adverse mélangé")
        player.hand.forEach { append(baseEvents, MatchEventType.CARD_DRAWN, MatchSide.PLAYER, it, "Carte piochée") }
        opponent.hand.forEach { append(baseEvents, MatchEventType.CARD_DRAWN, MatchSide.OPPONENT, it, "Carte adverse piochée") }
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
            MatchCommandV1.CompleteSetup -> completeSetup(state)
            is MatchCommandV1.AttachResource -> attachResource(state, command)
            is MatchCommandV1.Retreat -> retreat(state, command)
            is MatchCommandV1.Evolve -> evolve(state, command)
            is MatchCommandV1.ArmReply -> armReply(state, command)
            is MatchCommandV1.UseAction -> useAction(state, command)
            is MatchCommandV1.Attack -> attack(state, command)
            is MatchCommandV1.ResolveReaction -> resolveReaction(state, command)
            is MatchCommandV1.Promote -> promote(state, command)
            MatchCommandV1.EndTurn -> endTurn(state)
        }
    }

    private fun playCharacter(state: MatchStateV1, command: MatchCommandV1.PlayCharacter): MatchStateV1 {
        if (state.phase !in setOf(MatchPhase.SETUP, MatchPhase.MAIN)) {
            return reject(state, "Impossible de poser un Personnage maintenant.")
        }
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
        return replaceCurrentSide(state, updated).withEvent(
            MatchEventType.CHARACTER_PLAYED,
            state.activeSide,
            card.canonicalId,
            "${card.name} rejoint ${command.zone.name.lowercase()}.",
        )
    }

    private fun completeSetup(state: MatchStateV1): MatchStateV1 {
        if (state.phase != MatchPhase.SETUP) return reject(state, "Le placement initial est déjà terminé.")
        val side = currentSide(state)
        if (side.active == null) return reject(state, "Choisis un Personnage actif avant de terminer le placement.")

        var next = if (state.activeSide == MatchSide.PLAYER) {
            state.copy(playerSetupComplete = true)
        } else {
            state.copy(opponentSetupComplete = true)
        }.withEvent(MatchEventType.SETUP_COMPLETED, state.activeSide, side.active.cardId, "Placement initial terminé.")

        if (next.playerSetupComplete && next.opponentSetupComplete) {
            return next.copy(
                activeSide = MatchSide.PLAYER,
                phase = MatchPhase.MAIN,
                turnNumber = 1,
                flags = TurnFlags(),
            ).withEvent(MatchEventType.TURN_STARTED, MatchSide.PLAYER, null, "Tour 1 — PLAYER")
        }

        return next.copy(activeSide = opposite(state.activeSide))
    }

    private fun attachResource(state: MatchStateV1, command: MatchCommandV1.AttachResource): MatchStateV1 {
        if (state.phase != MatchPhase.MAIN) return reject(state, "Les ressources s’attachent pendant la phase principale.")
        if (state.flags.resourceAttached) return reject(state, "Une ressource a déjà été attachée ce tour.")
        val side = currentSide(state)
        val updated = side.updateCharacter(command.instanceId) { it.copy(resources = it.resources + 1) }
            ?: return reject(state, "Personnage cible introuvable.")
        return replaceCurrentSide(state, updated)
            .copy(flags = state.flags.copy(resourceAttached = true))
            .withEvent(
                MatchEventType.RESOURCE_ATTACHED,
                state.activeSide,
                updated.findCharacter(command.instanceId)?.cardId,
                "Ressource attachée.",
            )
    }

    private fun retreat(state: MatchStateV1, command: MatchCommandV1.Retreat): MatchStateV1 {
        if (state.phase != MatchPhase.MAIN) return reject(state, "La retraite se fait pendant la phase principale.")
        if (state.flags.retreatUsed) return reject(state, "Une retraite a déjà été effectuée ce tour.")
        val side = currentSide(state)
        val active = side.active ?: return reject(state, "Aucun Personnage actif.")
        val reserveIndex = side.reserves.indexOfFirst { it.instanceId == command.reserveInstanceId }
        if (reserveIndex < 0) return reject(state, "Le Personnage choisi n’est pas en réserve.")
        val retreatCost = catalog[active.cardId]?.retreat ?: return reject(state, "Coût de retraite introuvable.")
        if (active.resources < retreatCost) {
            return reject(state, "Ressources insuffisantes pour payer la retraite ($retreatCost).")
        }

        val promoted = side.reserves[reserveIndex]
        val retired = active.copy(resources = active.resources - retreatCost)
        val newReserves = side.reserves.toMutableList().also { it[reserveIndex] = retired }
        val updated = side.copy(active = promoted, reserves = newReserves)
        return replaceCurrentSide(state, updated)
            .copy(flags = state.flags.copy(retreatUsed = true))
            .withEvent(
                MatchEventType.CHARACTER_RETREATED,
                state.activeSide,
                retired.cardId,
                "Retraite payée : $retreatCost ressource(s). ${catalog[promoted.cardId]?.name ?: promoted.cardId} devient Actif.",
            )
    }

    private fun evolve(state: MatchStateV1, command: MatchCommandV1.Evolve): MatchStateV1 {
        if (state.phase != MatchPhase.MAIN) return reject(state, "Une évolution se joue pendant la phase principale.")
        if (state.turnNumber == 1) return reject(state, "Aucune évolution n’est autorisée au premier tour.")
        val side = currentSide(state)
        if (command.evolutionCardId !in side.hand) return reject(state, "L’évolution n’est pas dans la main.")
        val evolution = catalog[command.evolutionCardId] ?: return reject(state, "Évolution inconnue.")
        if (evolution.type != CardType.PERSONNAGE || evolution.evolutionStage == EvolutionStage.BASE) {
            return reject(state, "Cette carte n’est pas une évolution.")
        }
        val target = side.findCharacter(command.targetInstanceId) ?: return reject(state, "Personnage cible introuvable.")
        if (evolution.evolvesFromId != target.cardId) return reject(state, "Cette évolution ne correspond pas au Personnage ciblé.")
        if (target.enteredTurn >= state.turnNumber || target.evolvedThisTurn) {
            return reject(state, "Ce Personnage ne peut pas encore évoluer ce tour.")
        }

        val oldCardId = target.cardId
        val oldHp = catalog[oldCardId]?.hp ?: 0
        val evolved = target.copy(
            cardId = evolution.canonicalId,
            damage = target.damage.coerceAtMost(evolution.hp),
            evolvedThisTurn = true,
            evolutionStack = target.evolutionStack + oldCardId,
        )
        val updatedSide = side.updateCharacter(command.targetInstanceId) { evolved }
            ?.copy(hand = side.hand.removeOne(command.evolutionCardId))
            ?: return reject(state, "Évolution impossible.")
        return replaceCurrentSide(state, updatedSide).withEvent(
            MatchEventType.CARD_EVOLVED,
            state.activeSide,
            evolution.canonicalId,
            "Évolution : ${catalog[oldCardId]?.name ?: oldCardId} → ${evolution.name} ($oldHp→${evolution.hp} PV max).",
        )
    }

    private fun armReply(state: MatchStateV1, command: MatchCommandV1.ArmReply): MatchStateV1 {
        if (state.phase != MatchPhase.MAIN) return reject(state, "Une Réplique s’arme pendant la phase principale.")
        val side = currentSide(state)
        if (command.cardId !in side.hand) return reject(state, "Cette Réplique n’est pas dans la main.")
        val card = catalog[command.cardId] ?: return reject(state, "Carte inconnue.")
        if (card.type != CardType.REPLIQUE) return reject(state, "Cette carte n’est pas une Réplique.")
        val window = replyResolver.supportedWindow(command.cardId)
            ?: return reject(state, "${card.name} n’a pas encore d’exécuteur de Réplique V1 : la carte reste dans ta main.")
        if (side.supportSlots.size >= MATCH_MAX_SUPPORT_SLOTS) {
            return reject(state, "Les trois emplacements Action/Réplique sont occupés.")
        }
        val updated = side.copy(
            hand = side.hand.removeOne(command.cardId),
            supportSlots = side.supportSlots + command.cardId,
        )
        return replaceCurrentSide(state, updated).withEvent(
            MatchEventType.REPLY_ARMED,
            state.activeSide,
            command.cardId,
            "${card.name} est armée pour ${window.name}.",
        )
    }

    private fun useAction(state: MatchStateV1, command: MatchCommandV1.UseAction): MatchStateV1 {
        val result = actionResolver.resolve(
            state = state,
            side = state.activeSide,
            actionCardId = command.cardId,
            choice = command.choice,
        )
        return if (result.success) result.state else reject(state, result.reason)
    }

    private fun attack(state: MatchStateV1, command: MatchCommandV1.Attack): MatchStateV1 {
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

        val pending = PendingAttackV1(
            attackerSide = state.activeSide,
            attackerInstanceId = attacker.instanceId,
            defenderInstanceId = defender.instanceId,
            attackIndex = command.attackIndex,
            attackName = attack.name,
            incomingDamage = attack.damage,
        )
        val next = state.copy(
            phase = MatchPhase.RESOLVING,
            flags = state.flags.copy(attackDeclared = true),
            pendingAttack = pending,
            pendingReaction = null,
        ).withEvent(
            MatchEventType.ATTACK_DECLARED,
            state.activeSide,
            attacker.cardId,
            attack.name,
        )
        return openBeforeDamage(next)
    }

    private fun resolveReaction(state: MatchStateV1, command: MatchCommandV1.ResolveReaction): MatchStateV1 {
        if (state.phase != MatchPhase.RESOLVING) return reject(state, "Aucune attaque n’attend de Réplique.")
        val reaction = state.pendingReaction ?: return reject(state, "Aucune fenêtre de Réplique n’est ouverte.")
        val pending = state.pendingAttack ?: return reject(state, "Contexte d’attaque introuvable.")

        if (command.cardId == null) {
            val passed = state.copy(pendingReaction = null).withEvent(
                MatchEventType.REACTION_PASSED,
                reaction.side,
                null,
                "Fenêtre ${reaction.window.name} passée.",
            )
            return continueAfterWindow(passed, reaction.window)
        }

        val cardId = command.cardId
        val reactionSide = sideFor(state, reaction.side)
        if (cardId !in reaction.eligibleCardIds || cardId !in reactionSide.supportSlots) {
            return reject(state, "Cette Réplique n’est pas disponible dans cette fenêtre.")
        }

        val attackerSide = sideFor(state, pending.attackerSide)
        val defenderSideName = opposite(pending.attackerSide)
        val defenderSide = sideFor(state, defenderSideName)
        val attacker = attackerSide.findCharacter(pending.attackerInstanceId)
            ?: return reject(state, "Attaquant introuvable pendant la résolution.")
        val defender = defenderSide.findCharacter(pending.defenderInstanceId)
            ?: return reject(state, "Défenseur introuvable pendant la résolution.")
        val defenderMaxHp = catalog[defender.cardId]?.hp ?: return reject(state, "PV du défenseur introuvables.")

        if (reaction.window == ReplyWindowV1.AFTER_ATTACK) {
            val attackerMaxHp = catalog[attacker.cardId]?.hp ?: return reject(state, "PV de l’attaquant introuvables.")
            if (attacker.damage + 30 >= attackerMaxHp) {
                return reject(
                    state,
                    "Cette Réplique provoquerait un K.O. de l’attaquant après l’attaque. Ce cas n’est pas défini par la source vérifiée : la carte reste armée.",
                )
            }
        }

        val output = replyResolver.resolve(
            cardId = cardId,
            window = reaction.window,
            input = ReplyResolutionInputV1(
                incomingDamage = pending.incomingDamage,
                defenderMaxHp = defenderMaxHp,
                defenderDamageBeforeAttack = defender.damage,
                attackerDamageBeforeReply = attacker.damage,
            ),
        )
        if (!output.success) return reject(state, output.reason)

        var next = consumeReply(state, reaction.side, cardId)
            .copy(pendingReaction = null)
            .withEvent(
                MatchEventType.REPLY_TRIGGERED,
                reaction.side,
                cardId,
                output.reason,
            )
            .withEvent(
                MatchEventType.EFFECT_APPLIED,
                reaction.side,
                cardId,
                output.reason,
            )

        return when (reaction.window) {
            ReplyWindowV1.BEFORE_DAMAGE -> {
                next = next.copy(
                    pendingAttack = pending.copy(incomingDamage = output.incomingDamage),
                ).withEvent(
                    MatchEventType.DAMAGE_CALCULATED,
                    defenderSideName,
                    defender.cardId,
                    "Dégâts après Réplique : ${output.incomingDamage}.",
                )
                openBeforeKo(next)
            }
            ReplyWindowV1.BEFORE_KO -> {
                next = next.copy(
                    pendingAttack = pending.copy(defenderDamageOverride = output.defenderDamage),
                )
                applyPendingDamage(next)
            }
            ReplyWindowV1.AFTER_ATTACK -> {
                val updatedAttackerSide = attackerSide.updateCharacter(attacker.instanceId) {
                    it.copy(damage = output.attackerDamage)
                } ?: return reject(state, "Impossible d’appliquer le contre-dégât.")
                next = replaceSide(next, pending.attackerSide, updatedAttackerSide)
                    .withEvent(
                        MatchEventType.DAMAGE_APPLIED,
                        pending.attackerSide,
                        attacker.cardId,
                        "${output.attackerDamage - attacker.damage} dégâts de Réplique appliqués à l’attaquant.",
                    )
                finishAttack(next)
            }
        }
    }

    private fun openBeforeDamage(state: MatchStateV1): MatchStateV1 =
        openReactionWindow(state, ReplyWindowV1.BEFORE_DAMAGE) ?: openBeforeKo(state)

    private fun openBeforeKo(state: MatchStateV1): MatchStateV1 {
        val pending = state.pendingAttack ?: return reject(state, "Contexte d’attaque introuvable.")
        val defenderSide = sideFor(state, opposite(pending.attackerSide))
        val defender = defenderSide.findCharacter(pending.defenderInstanceId)
            ?: return reject(state, "Défenseur introuvable.")
        val maxHp = catalog[defender.cardId]?.hp ?: return reject(state, "PV du défenseur introuvables.")
        val wouldBeKo = defender.damage + pending.incomingDamage >= maxHp
        if (!wouldBeKo) return applyPendingDamage(state)
        return openReactionWindow(state, ReplyWindowV1.BEFORE_KO) ?: applyPendingDamage(state)
    }

    private fun openAfterAttack(state: MatchStateV1): MatchStateV1 =
        openReactionWindow(state, ReplyWindowV1.AFTER_ATTACK) ?: finishAttack(state)

    private fun openReactionWindow(state: MatchStateV1, window: ReplyWindowV1): MatchStateV1? {
        val pending = state.pendingAttack ?: return null
        val reactionSideName = opposite(pending.attackerSide)
        val reactionSide = sideFor(state, reactionSideName)
        val eligible = reactionSide.supportSlots.filter { replyResolver.supportedWindow(it) == window }
        if (eligible.isEmpty()) return null
        return state.copy(
            phase = MatchPhase.RESOLVING,
            pendingReaction = PendingReactionV1(
                side = reactionSideName,
                window = window,
                eligibleCardIds = eligible,
            ),
        ).withEvent(
            MatchEventType.REACTION_REQUIRED,
            reactionSideName,
            null,
            "Fenêtre de Réplique ${window.name}.",
        )
    }

    private fun continueAfterWindow(state: MatchStateV1, window: ReplyWindowV1): MatchStateV1 =
        when (window) {
            ReplyWindowV1.BEFORE_DAMAGE -> openBeforeKo(state)
            ReplyWindowV1.BEFORE_KO -> applyPendingDamage(state)
            ReplyWindowV1.AFTER_ATTACK -> finishAttack(state)
        }

    private fun applyPendingDamage(state: MatchStateV1): MatchStateV1 {
        val pending = state.pendingAttack ?: return reject(state, "Contexte d’attaque introuvable.")
        val defenderSideName = opposite(pending.attackerSide)
        val defenderSide = sideFor(state, defenderSideName)
        val defender = defenderSide.findCharacter(pending.defenderInstanceId)
            ?: return reject(state, "Défenseur introuvable.")
        val finalDamage = pending.defenderDamageOverride ?: (defender.damage + pending.incomingDamage)
        val applied = (finalDamage - defender.damage).coerceAtLeast(0)
        val hit = defender.copy(damage = finalDamage)
        val updatedDefenderSide = defenderSide.updateCharacter(defender.instanceId) { hit }
            ?: return reject(state, "Impossible d’appliquer les dégâts.")

        var next = replaceSide(state, defenderSideName, updatedDefenderSide)
            .copy(pendingReaction = null)
            .withEvent(
                MatchEventType.DAMAGE_CALCULATED,
                defenderSideName,
                defender.cardId,
                "Dégâts finaux de l’attaque : $applied.",
            )
            .withEvent(
                MatchEventType.DAMAGE_APPLIED,
                defenderSideName,
                defender.cardId,
                "$applied dégâts appliqués.",
            )

        if (hit.hpLeft(catalog) <= 0) {
            return resolveDefenderKo(next, pending, hit)
        }
        return openAfterAttack(next)
    }

    private fun resolveDefenderKo(
        state: MatchStateV1,
        pending: PendingAttackV1,
        defeated: CharacterInPlay,
    ): MatchStateV1 {
        val defenderSideName = opposite(pending.attackerSide)
        val defenderSide = sideFor(state, defenderSideName)
        val clearedDefender = defenderSide.copy(
            active = null,
            discard = defenderSide.discard + defeated.allCardIds(),
        )
        val attackerSide = sideFor(state, pending.attackerSide)
        val scoringSide = attackerSide.copy(points = attackerSide.points + 1)
        var next = replaceSide(
            replaceSide(state, defenderSideName, clearedDefender),
            pending.attackerSide,
            scoringSide,
        ).copy(
            pendingAttack = null,
            pendingReaction = null,
        ).withEvent(
            MatchEventType.CARD_KNOCKED_OUT,
            defenderSideName,
            defeated.cardId,
            "Personnage K.O.",
        )

        if (scoringSide.points >= MATCH_POINTS_TO_WIN) {
            return next.copy(phase = MatchPhase.FINISHED, winner = pending.attackerSide)
                .withEvent(MatchEventType.MATCH_FINISHED, pending.attackerSide, null, "Victoire ${pending.attackerSide}.")
        }
        if (clearedDefender.reserves.isEmpty()) {
            return next.copy(phase = MatchPhase.FINISHED, winner = pending.attackerSide)
                .withEvent(
                    MatchEventType.MATCH_FINISHED,
                    pending.attackerSide,
                    null,
                    "Victoire : plus aucun Personnage adverse en jeu.",
                )
        }

        return next.copy(
            phase = MatchPhase.PROMOTION,
            pendingPromotionSide = defenderSideName,
        ).withEvent(
            MatchEventType.PROMOTION_REQUIRED,
            defenderSideName,
            null,
            "Choisissez un Personnage de réserve à promouvoir.",
        )
    }

    private fun finishAttack(state: MatchStateV1): MatchStateV1 =
        endTurn(
            state.copy(
                phase = MatchPhase.MAIN,
                pendingAttack = null,
                pendingReaction = null,
            )
        )

    private fun consumeReply(state: MatchStateV1, side: MatchSide, cardId: String): MatchStateV1 {
        val current = sideFor(state, side)
        val updated = current.copy(
            supportSlots = current.supportSlots.removeOne(cardId),
            discard = current.discard + cardId,
        )
        return replaceSide(state, side, updated)
    }

    private fun promote(state: MatchStateV1, command: MatchCommandV1.Promote): MatchStateV1 {
        if (state.phase != MatchPhase.PROMOTION) return reject(state, "Aucune promotion n’est attendue.")
        val promotionSide = state.pendingPromotionSide ?: return reject(state, "Aucun camp n’attend de promotion.")
        val side = sideFor(state, promotionSide)
        if (side.active != null) return reject(state, "Le camp possède déjà un Personnage actif.")
        val reserveIndex = side.reserves.indexOfFirst { it.instanceId == command.reserveInstanceId }
        if (reserveIndex < 0) return reject(state, "Le Personnage choisi n’est pas disponible pour la promotion.")

        val promoted = side.reserves[reserveIndex]
        val updated = side.copy(
            active = promoted,
            reserves = side.reserves.toMutableList().also { it.removeAt(reserveIndex) },
        )
        var next = replaceSide(state, promotionSide, updated)
            .copy(phase = MatchPhase.MAIN, pendingPromotionSide = null)
            .withEvent(
                MatchEventType.CHARACTER_PROMOTED,
                promotionSide,
                promoted.cardId,
                "${catalog[promoted.cardId]?.name ?: promoted.cardId} devient Actif.",
            )
        next = endTurn(next)
        return next
    }

    private fun endTurn(state: MatchStateV1): MatchStateV1 {
        if (state.phase !in setOf(MatchPhase.MAIN, MatchPhase.RESOLVING)) {
            return reject(state, "Impossible de terminer le tour maintenant.")
        }
        if (state.pendingPromotionSide != null) return reject(state, "Une promotion doit être résolue avant la fin du tour.")
        if (state.pendingAttack != null || state.pendingReaction != null) {
            return reject(state, "Une attaque ou une fenêtre de Réplique doit être résolue avant la fin du tour.")
        }
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
        val count = minOf(MATCH_STARTING_HAND, MATCH_MAX_HAND, side.deck.size)
        return side.copy(hand = side.deck.take(count), deck = side.deck.drop(count))
    }

    private fun drawForTurn(state: MatchStateV1, side: MatchSide): MatchStateV1 {
        val current = sideFor(state, side)
        if (current.hand.size >= MATCH_MAX_HAND) {
            return state.withEvent(
                MatchEventType.DRAW_SKIPPED,
                side,
                null,
                "Pioche ignorée : main pleine ($MATCH_MAX_HAND cartes).",
            )
        }
        val card = current.deck.firstOrNull() ?: return state
        val updated = current.copy(deck = current.deck.drop(1), hand = current.hand + card)
        return replaceSide(state, side, updated)
            .withEvent(MatchEventType.CARD_DRAWN, side, card, "Carte piochée")
    }

    private fun currentSide(state: MatchStateV1): SideState = sideFor(state, state.activeSide)
    private fun otherSide(state: MatchStateV1): SideState = sideFor(state, opposite(state.activeSide))

    private fun sideFor(state: MatchStateV1, side: MatchSide): SideState =
        if (side == MatchSide.PLAYER) state.player else state.opponent

    private fun replaceSide(state: MatchStateV1, side: MatchSide, value: SideState): MatchStateV1 =
        if (side == MatchSide.PLAYER) state.copy(player = value) else state.copy(opponent = value)

    private fun replaceCurrentSide(state: MatchStateV1, side: SideState): MatchStateV1 =
        replaceSide(state, state.activeSide, side)

    private fun reject(state: MatchStateV1, reason: String): MatchStateV1 =
        state.withEvent(MatchEventType.COMMAND_REJECTED, state.activeSide, null, reason)

    private fun nextInstanceId(side: MatchSide): String =
        "${if (side == MatchSide.PLAYER) "p" else "o"}-${seed}-${++instanceCounter}"

    private fun append(
        events: MutableList<MatchEvent>,
        type: MatchEventType,
        side: MatchSide?,
        cardId: String?,
        message: String,
    ) {
        events += MatchEvent(events.size, type, side, cardId, message)
    }

    private fun MatchStateV1.withEvent(
        type: MatchEventType,
        side: MatchSide?,
        cardId: String?,
        message: String,
    ): MatchStateV1 = copy(events = events + MatchEvent(events.size, type, side, cardId, message))

    private fun SideState.findCharacter(instanceId: String): CharacterInPlay? =
        active?.takeIf { it.instanceId == instanceId } ?: reserves.firstOrNull { it.instanceId == instanceId }

    private fun SideState.updateCharacter(
        instanceId: String,
        transform: (CharacterInPlay) -> CharacterInPlay,
    ): SideState? {
        val currentActive = active
        if (currentActive?.instanceId == instanceId) return copy(active = transform(currentActive))
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

    private fun opposite(side: MatchSide): MatchSide =
        if (side == MatchSide.PLAYER) MatchSide.OPPONENT else MatchSide.PLAYER
}
