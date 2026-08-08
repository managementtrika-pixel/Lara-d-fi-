package com.zeubicardgames.app.core.effects

import com.zeubicardgames.app.core.model.CardDefinition
import com.zeubicardgames.app.core.model.CardType

enum class ReplyWindowV1 {
    BEFORE_DAMAGE,
    BEFORE_KO,
    AFTER_ATTACK,
}

data class ReplyResolutionInputV1(
    val incomingDamage: Int = 0,
    val defenderMaxHp: Int = 0,
    val defenderDamageBeforeAttack: Int = 0,
    val attackerDamageBeforeReply: Int = 0,
)

data class ReplyResolutionOutputV1(
    val incomingDamage: Int,
    val defenderDamage: Int,
    val attackerDamage: Int,
    val preventsKo: Boolean = false,
    val hpRemainingAfterPrevention: Int? = null,
    val success: Boolean,
    val reason: String,
)

/**
 * Résolveur pur : aucune mutation de match, aucun choix caché, aucune
 * consommation de carte. Il transforme uniquement le contexte d'une attaque.
 * La consommation/les fenêtres de réaction restent la responsabilité du
 * MatchEngine.
 */
class ReplyEffectResolverV1(cards: List<CardDefinition>) {
    private val catalog = cards.associateBy(CardDefinition::canonicalId)

    fun supportedWindow(cardId: String): ReplyWindowV1? {
        val card = catalog[cardId] ?: return null
        if (card.type != CardType.REPLIQUE) return null
        return when (SemanticEffectRegistry.find(card)?.cardKey) {
            "cod:025", "ninja:026" -> ReplyWindowV1.BEFORE_DAMAGE
            "cod:027", "emerald:027" -> ReplyWindowV1.AFTER_ATTACK
            "cod:028", "emerald:028" -> ReplyWindowV1.BEFORE_KO
            else -> null
        }
    }

    fun resolve(
        cardId: String,
        window: ReplyWindowV1,
        input: ReplyResolutionInputV1,
    ): ReplyResolutionOutputV1 {
        val card = catalog[cardId] ?: return failure(input, "Réplique inconnue.")
        if (card.type != CardType.REPLIQUE) return failure(input, "Cette carte n’est pas une Réplique.")
        val expected = supportedWindow(cardId)
            ?: return failure(input, "Cette Réplique n’a pas encore d’exécuteur V1.")
        if (expected != window) return failure(input, "Cette Réplique ne peut pas se déclencher à ce moment.")

        return when (SemanticEffectRegistry.find(card)?.cardKey) {
            "cod:025", "ninja:026" -> {
                val reduced = (input.incomingDamage - 40).coerceAtLeast(0)
                success(
                    input = input,
                    incomingDamage = reduced,
                    defenderDamage = input.defenderDamageBeforeAttack + reduced,
                    reason = "Dégâts de l’attaque réduits de 40.",
                )
            }
            "cod:027", "emerald:027" -> success(
                input = input,
                attackerDamage = input.attackerDamageBeforeReply + 30,
                reason = "30 dégâts infligés à l’attaquant après son attaque.",
            )
            "cod:028", "emerald:028" -> {
                if (input.defenderMaxHp <= 0) return failure(input, "PV maximum invalides.")
                val wouldBeKo = input.defenderDamageBeforeAttack + input.incomingDamage >= input.defenderMaxHp
                if (!wouldBeKo) return failure(input, "La Réplique de survie ne peut se déclencher que sur un K.O. imminent.")
                success(
                    input = input,
                    defenderDamage = (input.defenderMaxHp - 10).coerceAtLeast(0),
                    preventsKo = true,
                    hpRemainingAfterPrevention = 10,
                    reason = "Le Personnage reste à 10 PV.",
                )
            }
            else -> failure(input, "Réplique non prise en charge.")
        }
    }

    private fun success(
        input: ReplyResolutionInputV1,
        incomingDamage: Int = input.incomingDamage,
        defenderDamage: Int = input.defenderDamageBeforeAttack + incomingDamage,
        attackerDamage: Int = input.attackerDamageBeforeReply,
        preventsKo: Boolean = false,
        hpRemainingAfterPrevention: Int? = null,
        reason: String,
    ) = ReplyResolutionOutputV1(
        incomingDamage = incomingDamage,
        defenderDamage = defenderDamage,
        attackerDamage = attackerDamage,
        preventsKo = preventsKo,
        hpRemainingAfterPrevention = hpRemainingAfterPrevention,
        success = true,
        reason = reason,
    )

    private fun failure(input: ReplyResolutionInputV1, reason: String) = ReplyResolutionOutputV1(
        incomingDamage = input.incomingDamage,
        defenderDamage = input.defenderDamageBeforeAttack + input.incomingDamage,
        attackerDamage = input.attackerDamageBeforeReply,
        success = false,
        reason = reason,
    )
}
