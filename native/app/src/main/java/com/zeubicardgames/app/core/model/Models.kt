package com.zeubicardgames.app.core.model

import androidx.compose.runtime.Immutable

enum class CardType(val label: String) {
    PERSONNAGE("Personnage"),
    ACTION("Action"),
    REPLIQUE("Réplique"),
    RESSOURCE("Ressource"),
    INCONNU("Autre");

    companion object {
        fun from(raw: String): CardType = when (raw.trim().lowercase()) {
            "pokemon", "personnage", "character", "fighter" -> PERSONNAGE
            "action", "trainer", "support" -> ACTION
            "replique", "réplique", "reply", "trap" -> REPLIQUE
            "ressource", "resource", "energy", "energie", "énergie" -> RESSOURCE
            else -> INCONNU
        }
    }
}

enum class EvolutionStage(val label: String) {
    BASE("Forme initiale"),
    EVOLUTION("Évolution"),
    SUREVOLUTION("Surévolution"),
    AUCUNE("—");

    companion object {
        fun from(raw: String): EvolutionStage = when (raw.trim().lowercase()) {
            "base", "basic" -> BASE
            "evo1", "stage1", "evolution", "évolution" -> EVOLUTION
            "evo2", "stage2", "over", "over_evolution", "surevolution", "surévolution" -> SUREVOLUTION
            else -> AUCUNE
        }
    }
}

@Immutable
data class CardDefinition(
    val canonicalId: String,
    val setId: String,
    val number: String,
    val name: String,
    val kind: String,
    val stage: String,
    val evolvesFrom: String?,
    val hp: Int,
    val retreat: Int,
    val rarity: Rarity,
    val attacks: List<Attack>,
    val effect: String?,
    val variants: List<CardVariant>,
    val evolvesFromId: String? = null,
    val schemaVersion: Int = 1,
) {
    val type: CardType get() = CardType.from(kind)
    val evolutionStage: EvolutionStage get() =
        if (type == CardType.PERSONNAGE) EvolutionStage.from(stage) else EvolutionStage.AUCUNE
}

@Immutable
data class CardVariant(
    val variantId: String,
    val fullPath: String,
    val thumbPath: String,
)

@Immutable
data class Attack(
    val name: String,
    val damage: Int,
    val cost: Int,
)

enum class Rarity(val rank: Int, val label: String) {
    C(0, "Commune"),
    U(1, "Peu commune"),
    R(2, "Rare"),
    SR(3, "Super rare"),
    UR(4, "Ultra rare"),
    SUPRA(5, "Supra rare");

    companion object {
        fun from(raw: String): Rarity = entries.firstOrNull { it.name == raw.uppercase() } ?: C
    }
}

enum class ContentStatus { ACTIVE, INACTIVE }

@Immutable
data class ExtensionDefinition(
    val id: String,
    val name: String,
    val subtitle: String,
    val accent: Long,
    val boosterPath: String,
    val cardCount: Int,
    val schemaVersion: Int = 1,
    val order: Int = 0,
    val status: ContentStatus = ContentStatus.ACTIVE,
    val code: String = id.uppercase(),
)

@Immutable
data class OwnedCard(
    val canonicalId: String,
    val quantity: Int,
    val selectedVariantId: String?,
)

@Immutable
data class Deck(
    val id: Long = 0,
    val name: String,
    val cardIds: List<String>,
)

@Immutable
data class CampaignOpponent(
    val id: String,
    val name: String,
    val extensionId: String,
    val difficulty: Difficulty,
    val rewardCoins: Int,
    val description: String,
    val bossCardName: String,
    val bossCardId: String? = null,
)

enum class Difficulty { FACILE, NORMAL, DIFFICILE, EXPERT }

val OfficialOpponents = listOf(
    CampaignOpponent("ninja_1", "Recrue Ninja", "ninja", Difficulty.FACILE, 120, "Premiers pas dans l’ombre.", "Zaim Sinja"),
    CampaignOpponent("ninja_2", "Maître du Chakra", "ninja", Difficulty.DIFFICILE, 260, "Une maîtrise précise du chakra.", "Roobkage"),
    CampaignOpponent("emerald_1", "Patrouille Émeraude", "emerald", Difficulty.NORMAL, 160, "La volonté protège le secteur.", "Bafolantern"),
    CampaignOpponent("emerald_2", "Baforallax", "emerald", Difficulty.EXPERT, 360, "La peur prend une forme colossale.", "Baforallax"),
    CampaignOpponent("cod_1", "Escouade Recrue", "cod", Difficulty.NORMAL, 180, "Une unité encore maladroite mais dangereuse.", "Lara Recrue"),
    CampaignOpponent("cod_2", "Lara Prestige Master", "cod", Difficulty.EXPERT, 400, "Le niveau maximal des opérations PLAYER.", "Lara Prestige Master"),
    CampaignOpponent("dbz_1", "Guerrier Saiyan", "dbz", Difficulty.DIFFICILE, 280, "La puissance monte à chaque tour.", "Super Rayanjin"),
    CampaignOpponent("dbz_2", "Trika Genkidama", "dbz", Difficulty.EXPERT, 450, "L’ultime épreuve de Trika Ball Z.", "Trika Genkidama"),
)
