package com.metahumanlegacy.game

enum class Scope(val label: String) {
    STREET("Rue"), DISTRICT("Quartier"), CITY("Ville"), REGION("Région"), COUNTRY("Pays"), WORLD("Monde")
}

data class CharacterBlueprint(
    val firstName: String,
    val lastName: String,
    val pronouns: String,
    val city: String,
    val district: String,
    val socialBackground: String,
    val motivation: String,
    val civilianPath: String,
    val temperament: String
) {
    val fullName: String
        get() = listOf(firstName.trim(), lastName.trim())
            .filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Alex Vesper" }
}

data class Choice(
    val label: String,
    val moral: Int = 0,
    val prestige: Int = 0,
    val opinion: Int = 0,
    val fear: Int = 0,
    val power: Int = 0,
    val impact: Int = 0,
    val risk: Int = 0,
    val approach: String = "",
    val stakes: Int = 1,
    val sourceCategory: String = "",
    val threadId: String? = null,
    val relationDelta: Int = 0,
    val flag: String? = null,
    val identityDelta: Int = 0,
    val healthDelta: Int = 0,
    val affinityDelta: List<String> = emptyList(),
    val expressionDelta: List<String> = emptyList(),
    val costDelta: List<String> = emptyList(),
    val deferredHook: Boolean = false
)

data class EventNode(
    val id: String,
    val title: String,
    val text: String,
    val choices: List<Choice>,
    val category: String,
    val provocation: String,
    val stakes: Int,
    val threadId: String? = null,
    val threadStage: Int = 0,
    val kind: String = "MAJOR"
)

data class StoryThread(
    val id: String,
    val openedTurn: Int,
    val lastTurn: Int,
    val stage: Int,
    val lastApproach: String,
    val intensity: Int,
    val care: Int = 0,
    val order: Int = 0,
    val truth: Int = 0,
    val ascend: Int = 0
) {
    fun score(route: String): Int = when (route) {
        "CARE" -> care
        "ORDER" -> order
        "TRUTH" -> truth
        "ASCEND" -> ascend
        else -> 0
    }
}

data class Resolution(val campaign: Campaign, val outcome: String)

data class Campaign(
    val seed: Long,
    val name: String,
    val alias: String = "",
    val origin: String = "Origine inconnue",
    val powerFamily: String = "Non révélé",
    val weakness: String = "Inconnue",
    val modifier: String,
    val pronouns: String = "iel",
    val city: String = "Vesper",
    val district: String = "Centre",
    val socialBackground: String = "Classe moyenne",
    val motivation: String = "Protéger les miens",
    val civilianPath: String = "Vie ordinaire",
    val temperament: String = "Prudent",
    val visualStyle: String = "Aucun masque",
    val turn: Int = 0,
    val morality: Int = 0,
    val prestige: Int = 0,
    val opinion: Int = 0,
    val fear: Int = 0,
    val power: Int = 0,
    val control: Int = 15,
    val influence: Int = 0,
    val health: Int = 100,
    val civilianCasualties: Int = 0,
    val identityExposure: Int = 0,
    val familyBond: Int = 50,
    val rivalStanding: Int = 0,
    val governmentStanding: Int = 0,
    val factionStanding: Int = 0,
    val mediaStanding: Int = 0,
    val flags: Set<String> = emptySet(),
    val threads: List<StoryThread> = emptyList(),
    val lastCategory: String = "",
    val lastApproach: String = "",
    val timeline: List<String> = emptyList(),
    val affinityScores: Map<String, Int> = emptyMap(),
    val expressionScores: Map<String, Int> = emptyMap(),
    val costScores: Map<String, Int> = emptyMap(),
    val formativeRisk: Int = 0,
    val powerRevealText: String = "",
    val powerCostText: String = "",
    val powerSignature: String = ""
) {
    val powerRevealed: Boolean get() = "POWER_REVEALED" in flags
    val powerResolved: Boolean get() = powerFamily != "Non révélé"
    val needsAlias: Boolean get() = powerRevealed && alias.isBlank()

    val age: Int get() = when {
        turn <= 9 -> 8 + turn
        turn == 10 -> 18
        turn == 11 -> 18
        turn in 12..13 -> 19
        turn in 14..15 -> 20
        else -> 20 + (turn - 16).coerceAtLeast(0) / 4
    }

    val phaseLabel: String get() = when {
        turn < 10 -> "AVANT LE MASQUE"
        turn == 10 -> "ÉVEIL"
        turn in 11..15 -> "PREMIERS PAS"
        else -> "DESTINÉE MÉTAHUMAINE"
    }

    val scope: Scope get() = when {
        !powerRevealed -> Scope.STREET
        influence >= 900 -> Scope.WORLD
        influence >= 560 -> Scope.COUNTRY
        influence >= 340 -> Scope.REGION
        influence >= 180 -> Scope.CITY
        influence >= 75 -> Scope.DISTRICT
        else -> Scope.STREET
    }

    val moralLabel: String get() = when {
        morality >= 70 -> "Héroïque"
        morality >= 35 -> "Bienveillant"
        morality >= 10 -> "Altruiste"
        morality > -10 -> "Ambigu"
        morality > -35 -> "Impitoyable"
        morality > -70 -> "Corrompu"
        else -> "Monstrueux"
    }

    val finished: Boolean get() = turn >= 196 || health <= 0
}

internal data class FormativeChapter(
    val id: String, val age: Int, val title: String, val text: String, val choices: List<Choice>
)

internal data class FoundationScene(
    val id: String, val yearsAfterAwakening: Int, val title: String, val text: String, val choices: List<Choice>
)

internal data class MajorBeat(
    val id: String, val arc: String, val stage: Int,
    val minAge: Int, val maxAge: Int, val minScope: Scope,
    val requiresFlags: Set<String>, val tags: List<String>,
    val title: String, val text: String, val callbacks: List<String>,
    val choices: List<Choice>
)

internal data class AwakeningScene(
    val id: String, val age: Int, val title: String, val text: String,
    val alwaysFlags: Set<String>, val choices: List<Choice>
)

internal data class CatalogStats(
    val prologue: Int, val foundation: Int, val majorBeats: Int, val majorChoices: Int,
    val arcs: Int, val endings: Int
)
