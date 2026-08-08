package com.zeubicardgames.app.core.effects

import com.zeubicardgames.app.core.model.CardDefinition
import com.zeubicardgames.app.core.model.CardType

/**
 * Sémantique de gameplay vérifiée à partir du texte source des cartes.
 *
 * IMPORTANT : les anciens wire codes (ex. `heal90`) servent uniquement à la
 * compatibilité/migration. Les valeurs numériques et timings ci-dessous sont
 * la source de vérité du moteur dès qu'une carte est marquée VERIFIED.
 */
enum class EffectVerification { VERIFIED, RULE_CONFLICT, UNVERIFIED }

enum class EffectTiming {
    IMMEDIATE,
    AFTER_ATTACK,
    BEFORE_DAMAGE,
    BEFORE_KO,
    ON_RESOURCE_ATTACHED,
    ON_ATTACK_DECLARED,
    NEXT_TURN,
}

enum class EffectTarget {
    OWN_ACTIVE,
    OWN_CHARACTER,
    OPPONENT_ACTIVE,
    ATTACKER,
    SELF,
    CHOSEN_CHARACTER,
}

sealed interface EffectOperation {
    data class Heal(val amount: Int, val target: EffectTarget) : EffectOperation
    data class ReduceIncomingDamage(val amount: Int) : EffectOperation
    data class DealDamage(val amount: Int, val target: EffectTarget) : EffectOperation
    data class PreventKo(val hpRemaining: Int) : EffectOperation
    data class Draw(val count: Int) : EffectOperation
    data class DiscardFromHand(val count: Int) : EffectOperation
    data class MoveResources(val maxCount: Int) : EffectOperation
    data class SearchEvolution(val count: Int) : EffectOperation
    data class SearchTopForCardType(
        val inspectCount: Int,
        val takeCount: Int,
        val type: CardType,
    ) : EffectOperation
    data class SearchAction(val count: Int) : EffectOperation
    data class ModifyAttackDamage(
        val amount: Int,
        val target: EffectTarget,
        val untilNextTurnEnd: Boolean = false,
    ) : EffectOperation
    data class SelfDamageAfterAttack(val amount: Int) : EffectOperation
    data object InstantEvolveFromHand : EffectOperation
    data object SwitchBeforeDamage : EffectOperation
    data object CancelAttackDamageBonus : EffectOperation
    data class RecoverFromDiscard(val maxCount: Int, val minimumDiscardSize: Int) : EffectOperation
}

data class SemanticEffectStep(
    val timing: EffectTiming,
    val operation: EffectOperation,
)

data class SemanticEffectSpec(
    val cardKey: String,
    val verification: EffectVerification,
    val sourceSummary: String,
    val steps: List<SemanticEffectStep>,
)

object SemanticEffectRegistry {
    private fun key(setId: String, number: String) = "$setId:${number.padStart(3, '0')}"

    private val verified = listOf(
        SemanticEffectSpec(
            cardKey = "dbz:019",
            verification = EffectVerification.VERIFIED,
            sourceSummary = "Haricot Senzu : soigne 80 PV à un de vos Personnages.",
            steps = listOf(
                SemanticEffectStep(
                    EffectTiming.IMMEDIATE,
                    EffectOperation.Heal(80, EffectTarget.OWN_CHARACTER),
                )
            ),
        ),
        SemanticEffectSpec(
            cardKey = "dbz:020",
            verification = EffectVerification.VERIFIED,
            sourceSummary = "Détecteur de puissance : regardez les 5 cartes du dessus et prenez 1 Personnage.",
            steps = listOf(
                SemanticEffectStep(
                    EffectTiming.IMMEDIATE,
                    EffectOperation.SearchTopForCardType(5, 1, CardType.PERSONNAGE),
                )
            ),
        ),
        SemanticEffectSpec(
            cardKey = "dbz:022",
            verification = EffectVerification.VERIFIED,
            sourceSummary = "Entraînement sous gravité : Actif +30 dégâts ce tour puis 10 dégâts après son attaque.",
            steps = listOf(
                SemanticEffectStep(
                    EffectTiming.IMMEDIATE,
                    EffectOperation.ModifyAttackDamage(30, EffectTarget.OWN_ACTIVE),
                ),
                SemanticEffectStep(
                    EffectTiming.AFTER_ATTACK,
                    EffectOperation.SelfDamageAfterAttack(10),
                ),
            ),
        ),
        SemanticEffectSpec(
            cardKey = "dbz:023",
            verification = EffectVerification.VERIFIED,
            sourceSummary = "Chambre de l’esprit et du temps : faites évoluer immédiatement une forme initiale si son évolution est en main.",
            steps = listOf(
                SemanticEffectStep(EffectTiming.IMMEDIATE, EffectOperation.InstantEvolveFromHand)
            ),
        ),
        SemanticEffectSpec(
            cardKey = "dbz:025",
            verification = EffectVerification.VERIFIED,
            sourceSummary = "Téléportation ! : échangez l’Actif ciblé avant l’application des dégâts.",
            steps = listOf(
                SemanticEffectStep(EffectTiming.BEFORE_DAMAGE, EffectOperation.SwitchBeforeDamage)
            ),
        ),
        SemanticEffectSpec(
            cardKey = "dbz:026",
            verification = EffectVerification.VERIFIED,
            sourceSummary = "Même pas ma forme finale : si le Personnage devait être K.O., évoluez-le depuis la main et laissez-le à 30 PV.",
            steps = listOf(
                SemanticEffectStep(EffectTiming.BEFORE_KO, EffectOperation.InstantEvolveFromHand),
                SemanticEffectStep(EffectTiming.BEFORE_KO, EffectOperation.PreventKo(30)),
            ),
        ),
        SemanticEffectSpec(
            cardKey = "dbz:027",
            verification = EffectVerification.VERIFIED,
            sourceSummary = "Le détecteur explose : annule le bonus de dégâts d’attaque provenant d’un effet adverse.",
            steps = listOf(
                SemanticEffectStep(EffectTiming.ON_ATTACK_DECLARED, EffectOperation.CancelAttackDamageBonus)
            ),
        ),
        SemanticEffectSpec(
            cardKey = "dbz:029",
            verification = EffectVerification.VERIFIED,
            sourceSummary = "Les sept boules sont réunies : avec au moins 7 cartes en défausse, récupérez jusqu’à 2 cartes en main.",
            steps = listOf(
                SemanticEffectStep(
                    EffectTiming.IMMEDIATE,
                    EffectOperation.RecoverFromDiscard(maxCount = 2, minimumDiscardSize = 7),
                )
            ),
        ),
        SemanticEffectSpec(
            cardKey = "dbz:030",
            verification = EffectVerification.VERIFIED,
            sourceSummary = "Cinq minutes avant l’explosion : le dernier Personnage reste à 10 PV puis gagne +50 dégâts au tour suivant.",
            steps = listOf(
                SemanticEffectStep(EffectTiming.BEFORE_KO, EffectOperation.PreventKo(10)),
                SemanticEffectStep(
                    EffectTiming.NEXT_TURN,
                    EffectOperation.ModifyAttackDamage(50, EffectTarget.OWN_ACTIVE),
                ),
            ),
        ),
        SemanticEffectSpec(
            cardKey = "ninja:021",
            verification = EffectVerification.VERIFIED,
            sourceSummary = "Parchemin interdit : cherchez 1 Évolution dans le deck et ajoutez-la à la main.",
            steps = listOf(
                SemanticEffectStep(EffectTiming.IMMEDIATE, EffectOperation.SearchEvolution(1))
            ),
        ),
        SemanticEffectSpec(
            cardKey = "ninja:023",
            verification = EffectVerification.VERIFIED,
            sourceSummary = "Concentration du chakra : déplacez jusqu’à 2 Énergies entre vos Personnages.",
            steps = listOf(
                SemanticEffectStep(EffectTiming.IMMEDIATE, EffectOperation.MoveResources(2))
            ),
        ),
        SemanticEffectSpec(
            cardKey = "ninja:024",
            verification = EffectVerification.VERIFIED,
            sourceSummary = "Mission de rang S : piochez 3 cartes puis défaussez 1 carte.",
            steps = listOf(
                SemanticEffectStep(EffectTiming.IMMEDIATE, EffectOperation.Draw(3)),
                SemanticEffectStep(EffectTiming.IMMEDIATE, EffectOperation.DiscardFromHand(1)),
            ),
        ),
        SemanticEffectSpec(
            cardKey = "ninja:026",
            verification = EffectVerification.VERIFIED,
            sourceSummary = "C’était un clone : réduisez de 40 les dégâts de l’attaque entrante.",
            steps = listOf(
                SemanticEffectStep(EffectTiming.BEFORE_DAMAGE, EffectOperation.ReduceIncomingDamage(40))
            ),
        ),
        SemanticEffectSpec(
            cardKey = "emerald:022",
            verification = EffectVerification.VERIFIED,
            sourceSummary = "Construction impossible : cherchez 1 Action dans le deck puis piochez 1 carte.",
            steps = listOf(
                SemanticEffectStep(EffectTiming.IMMEDIATE, EffectOperation.SearchAction(1)),
                SemanticEffectStep(EffectTiming.IMMEDIATE, EffectOperation.Draw(1)),
            ),
        ),
        SemanticEffectSpec(
            cardKey = "emerald:027",
            verification = EffectVerification.VERIFIED,
            sourceSummary = "Construction surprise : après l’attaque adverse, infligez 30 dégâts à l’attaquant.",
            steps = listOf(
                SemanticEffectStep(
                    EffectTiming.AFTER_ATTACK,
                    EffectOperation.DealDamage(30, EffectTarget.ATTACKER),
                )
            ),
        ),
        SemanticEffectSpec(
            cardKey = "emerald:028",
            verification = EffectVerification.VERIFIED,
            sourceSummary = "Volonté inébranlable : lorsqu’un Personnage devait être K.O., il reste à 10 PV.",
            steps = listOf(
                SemanticEffectStep(EffectTiming.BEFORE_KO, EffectOperation.PreventKo(10))
            ),
        ),
        SemanticEffectSpec(
            cardKey = "cod:025",
            verification = EffectVerification.VERIFIED,
            sourceSummary = "Plaque d’armure : réduisez de 40 les dégâts de l’attaque entrante.",
            steps = listOf(
                SemanticEffectStep(EffectTiming.BEFORE_DAMAGE, EffectOperation.ReduceIncomingDamage(40))
            ),
        ),
        SemanticEffectSpec(
            cardKey = "cod:027",
            verification = EffectVerification.VERIFIED,
            sourceSummary = "Mine surprise : après l’attaque adverse, infligez 30 dégâts à l’attaquant.",
            steps = listOf(
                SemanticEffectStep(
                    EffectTiming.AFTER_ATTACK,
                    EffectOperation.DealDamage(30, EffectTarget.ATTACKER),
                )
            ),
        ),
        SemanticEffectSpec(
            cardKey = "cod:028",
            verification = EffectVerification.VERIFIED,
            sourceSummary = "Dernière chance : lorsqu’un Personnage devait être K.O., il reste à 10 PV.",
            steps = listOf(
                SemanticEffectStep(EffectTiming.BEFORE_KO, EffectOperation.PreventKo(10))
            ),
        ),
    ).associateBy(SemanticEffectSpec::cardKey)

    /**
     * Les cartes dont le texte confirmé dépend d'une ancienne règle d'Énergie
     * "dans le deck" sont explicitement signalées au lieu d'être interprétées
     * silencieusement avec la règle actuelle d'Énergie hors deck.
     */
    private val conflicts = mapOf(
        "dbz:021" to SemanticEffectSpec(
            cardKey = "dbz:021",
            verification = EffectVerification.RULE_CONFLICT,
            sourceSummary = "Capsule : le texte source demande de chercher une Énergie dans le deck, alors que le moteur actuel gère l’Énergie hors deck.",
            steps = emptyList(),
        ),
        "ninja:019" to SemanticEffectSpec(
            cardKey = "ninja:019",
            verification = EffectVerification.RULE_CONFLICT,
            sourceSummary = "Entraînement au chakra : le texte source demande de chercher une Énergie dans le deck, incompatible avec l’Énergie hors deck actuelle.",
            steps = emptyList(),
        ),
    )

    fun find(card: CardDefinition): SemanticEffectSpec? =
        verified[key(card.setId, card.number)] ?: conflicts[key(card.setId, card.number)]

    fun find(setId: String, number: String): SemanticEffectSpec? =
        verified[key(setId, number)] ?: conflicts[key(setId, number)]

    fun isExecutable(card: CardDefinition): Boolean =
        find(card)?.verification == EffectVerification.VERIFIED

    val verifiedCardKeys: Set<String> get() = verified.keys
    val conflictCardKeys: Set<String> get() = conflicts.keys
}
