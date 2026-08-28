package com.metahumanlegacy.game

enum class Scope(val label: String) {
    STREET("Rue"), DISTRICT("Quartier"), CITY("Ville"), REGION("Région"), COUNTRY("Pays"), WORLD("Monde")
}

data class CharacterBlueprint(
    val firstName: String, val lastName: String, val alias: String, val pronouns: String,
    val city: String, val district: String, val socialBackground: String, val origin: String,
    val powerFamily: String, val weakness: String, val motivation: String, val visualStyle: String
) {
    val fullName: String get() = listOf(firstName.trim(), lastName.trim()).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Alex Vesper" }
}

data class Choice(
    val label: String, val moral: Int, val prestige: Int, val opinion: Int, val fear: Int,
    val power: Int, val impact: Int, val risk: Int, val approach: String, val stakes: Int,
    val sourceCategory: String, val threadId: String? = null, val relationDelta: Int = 0,
    val flag: String? = null, val identityDelta: Int = 0, val healthDelta: Int = 0
)

data class EventNode(
    val id: String, val title: String, val text: String, val choices: List<Choice>,
    val category: String, val provocation: String, val stakes: Int,
    val threadId: String? = null, val threadStage: Int = 0
)

data class StoryThread(
    val id: String, val openedTurn: Int, val lastTurn: Int, val stage: Int,
    val lastApproach: String, val intensity: Int
)

data class Resolution(val campaign: Campaign, val outcome: String)

data class Campaign(
    val seed: Long, val name: String, val alias: String, val origin: String, val powerFamily: String,
    val weakness: String, val modifier: String, val pronouns: String = "iel", val city: String = "Vesper",
    val district: String = "Centre", val socialBackground: String = "Classe moyenne",
    val motivation: String = "Protéger les miens", val visualStyle: String = "Masque minimal",
    val turn: Int = 0, val morality: Int = 0, val prestige: Int = 0, val opinion: Int = 0,
    val fear: Int = 0, val power: Int = 28, val control: Int = 25, val influence: Int = 0,
    val health: Int = 100, val civilianCasualties: Int = 0, val identityExposure: Int = 0,
    val familyBond: Int = 50, val rivalStanding: Int = 0, val governmentStanding: Int = 0,
    val factionStanding: Int = 0, val mediaStanding: Int = 0, val flags: Set<String> = emptySet(),
    val threads: List<StoryThread> = emptyList(), val lastCategory: String = "",
    val lastApproach: String = "", val timeline: List<String> = emptyList()
) {
    val age: Int get() = 18 + turn / 4
    val scope: Scope get() = when {
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
    val finished: Boolean get() = turn >= 140 || health <= 0
}

internal data class CatalogStats(
    val events: Int, val choices: Int, val arcs: Int, val epilogues: Int,
    val duplicateIds: Int, val duplicateTitles: Int, val duplicateTexts: Int
)
