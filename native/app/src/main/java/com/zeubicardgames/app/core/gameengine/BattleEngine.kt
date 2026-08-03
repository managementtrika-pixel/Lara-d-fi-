package com.zeubicardgames.app.core.gameengine

import com.zeubicardgames.app.core.model.Attack
import com.zeubicardgames.app.core.model.CardDefinition
import kotlin.random.Random

sealed interface BattlePhase { data object Setup : BattlePhase; data object PlayerDraw : BattlePhase; data object PlayerMain : BattlePhase; data object Resolving : BattlePhase; data object AiTurn : BattlePhase; data object Promotion : BattlePhase; data object GameOver : BattlePhase }
enum class BattleEventType { CARD_DRAWN, CARD_PLAYED, ENERGY_ATTACHED, CARD_EVOLVED, RETREAT_PERFORMED, ATTACK_STARTED, DAMAGE_DEALT, CARD_KO, CARD_PROMOTED, TURN_CHANGED, GAME_OVER }
data class BattleEvent(val type: BattleEventType, val message: String)
data class Fighter(val instanceId: String, val card: CardDefinition, val damage: Int = 0, val energy: Int = 0) { val hpLeft get() = (card.hp - damage).coerceAtLeast(0) }
data class BattleState(
    val phase: BattlePhase = BattlePhase.Setup,
    val player: Fighter? = null,
    val opponent: Fighter? = null,
    val playerPoints: Int = 0,
    val opponentPoints: Int = 0,
    val energyAttachedThisTurn: Boolean = false,
    val events: List<BattleEvent> = emptyList(),
    val winner: String? = null,
)

class BattleEngine(private val seed: Long = 1L) {
    private val random = Random(seed)
    fun start(player: CardDefinition, opponent: CardDefinition): BattleState = BattleState(
        phase = BattlePhase.PlayerMain,
        player = Fighter("p-${random.nextInt()}", player), opponent = Fighter("a-${random.nextInt()}", opponent),
        events = listOf(BattleEvent(BattleEventType.TURN_CHANGED, "À toi de jouer"))
    )
    fun attachEnergy(state: BattleState): BattleState {
        if (state.phase != BattlePhase.PlayerMain || state.energyAttachedThisTurn || state.player == null) return state
        return state.copy(player = state.player.copy(energy = state.player.energy + 1), energyAttachedThisTurn = true, events = state.events + BattleEvent(BattleEventType.ENERGY_ATTACHED, "Énergie attachée"))
    }
    fun attack(state: BattleState, attack: Attack): BattleState {
        val player = state.player ?: return state
        val foe = state.opponent ?: return state
        if (state.phase != BattlePhase.PlayerMain || player.energy < attack.cost) return state
        val hit = foe.copy(damage = foe.damage + attack.damage)
        var next = state.copy(phase = BattlePhase.Resolving, opponent = hit, events = state.events + BattleEvent(BattleEventType.ATTACK_STARTED, attack.name) + BattleEvent(BattleEventType.DAMAGE_DEALT, "${attack.damage} dégâts"))
        if (hit.hpLeft <= 0) {
            val points = state.playerPoints + 1
            next = next.copy(playerPoints = points, events = next.events + BattleEvent(BattleEventType.CARD_KO, "K.O. adverse"))
            if (points >= 3) return next.copy(phase = BattlePhase.GameOver, winner = "player", events = next.events + BattleEvent(BattleEventType.GAME_OVER, "Victoire"))
            return next.copy(opponent = foe.copy(damage = 0, energy = 0), phase = BattlePhase.PlayerMain, energyAttachedThisTurn = false)
        }
        return aiTurn(next)
    }
    private fun aiTurn(state: BattleState): BattleState {
        val player = state.player ?: return state
        val foe = state.opponent ?: return state
        val attack = foe.card.attacks.maxByOrNull { it.damage } ?: Attack("Impact", 10, 0)
        val hit = player.copy(damage = player.damage + attack.damage)
        var next = state.copy(phase = BattlePhase.AiTurn, player = hit, events = state.events + BattleEvent(BattleEventType.TURN_CHANGED, "Tour IA") + BattleEvent(BattleEventType.DAMAGE_DEALT, "L’IA inflige ${attack.damage}"))
        if (hit.hpLeft <= 0) {
            val points = state.opponentPoints + 1
            next = next.copy(opponentPoints = points, events = next.events + BattleEvent(BattleEventType.CARD_KO, "Ton actif est K.O."))
            if (points >= 3) return next.copy(phase = BattlePhase.GameOver, winner = "ai", events = next.events + BattleEvent(BattleEventType.GAME_OVER, "Défaite"))
            next = next.copy(player = player.copy(damage = 0, energy = 0))
        }
        return next.copy(phase = BattlePhase.PlayerMain, energyAttachedThisTurn = false, events = next.events + BattleEvent(BattleEventType.TURN_CHANGED, "À toi"))
    }
}
