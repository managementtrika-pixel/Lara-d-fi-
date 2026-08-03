package com.zeubicardgames.app.core.model

import androidx.compose.runtime.Immutable

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
)

@Immutable data class CardVariant(val variantId: String, val fullPath: String, val thumbPath: String)
@Immutable data class Attack(val name: String, val damage: Int, val cost: Int)
enum class Rarity(val rank: Int) { C(0), U(1), R(2), SR(3), UR(4); companion object { fun from(raw: String) = entries.firstOrNull { it.name == raw.uppercase() } ?: C } }

@Immutable
data class ExtensionDefinition(
    val id: String,
    val name: String,
    val subtitle: String,
    val accent: Long,
    val boosterPath: String,
    val cardCount: Int,
)

@Immutable data class OwnedCard(val canonicalId: String, val quantity: Int, val selectedVariantId: String?)
@Immutable data class Deck(val id: Long = 0, val name: String, val cardIds: List<String>)

@Immutable
data class CampaignOpponent(
    val id: String,
    val name: String,
    val extensionId: String,
    val difficulty: Difficulty,
    val rewardCoins: Int,
    val description: String,
    val bossCardName: String,
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
