package com.metahumanlegacy.game

import java.util.Random
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class Scope(val label: String) { STREET("Rue"), DISTRICT("Quartier"), CITY("Ville"), REGION("Région"), COUNTRY("Pays"), WORLD("Monde") }

data class Choice(
    val label: String,
    val moral: Int,
    val prestige: Int,
    val opinion: Int,
    val fear: Int,
    val power: Int,
    val impact: Int,
    val risk: Int
)

data class EventNode(val id: String, val title: String, val text: String, val choices: List<Choice>, val category: String)

data class Campaign(
    val seed: Long,
    val name: String,
    val alias: String,
    val origin: String,
    val powerFamily: String,
    val weakness: String,
    val modifier: String,
    val turn: Int = 0,
    val morality: Int = 0,
    val prestige: Int = 0,
    val opinion: Int = 0,
    val fear: Int = 0,
    val power: Int = 28,
    val control: Int = 25,
    val influence: Int = 0,
    val health: Int = 100,
    val civilianCasualties: Int = 0,
    val identityExposure: Int = 0,
    val timeline: List<String> = emptyList()
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
    val finished: Boolean get() = turn >= 120 || health <= 0
}

object GameEngine {
    val origins = listOf(
        "Mutation naturelle", "Accident scientifique", "Expérience clandestine", "Programme militaire",
        "Technologie personnelle", "Héritage familial", "Artefact mystérieux", "Pacte occulte",
        "Origine extraterrestre", "Énergie cosmique", "Entraînement humain extrême", "Intelligence augmentée"
    )
    val powers = listOf(
        "Force", "Résistance", "Vitesse", "Vol", "Énergie", "Feu", "Glace", "Électricité",
        "Télékinésie", "Télépathie", "Illusion", "Influence mentale limitée", "Métamorphose", "Invisibilité",
        "Régénération", "Technologie", "Armes spécialisées", "Magie", "Matière", "Gravité", "Espace",
        "Duplication", "Invocation", "Absorption", "Adaptation", "Humain exceptionnel"
    )
    private val weaknesses = listOf(
        "Surcharge", "Fatigue extrême", "Concentration", "Énergie externe", "Fréquence sonore",
        "Instabilité émotionnelle", "Temps de récupération", "Environnement", "Vulnérabilité psychique",
        "Vulnérabilité mystique", "Pouvoir difficile à dissimuler", "Précision limitée"
    )
    private val modifiers = listOf(
        "Âge des héros", "Première génération", "Société méfiante", "Culture héroïque", "État autoritaire",
        "Criminalité endémique", "Ère technologique", "Menace occulte", "Silence cosmique", "Médias omniprésents"
    )
    private val categories = listOf("RUE", "IDENTITÉ", "FAMILLE", "MÉDIAS", "FACTION", "RIVAL", "SAUVETAGE", "CRIME", "POUVOIR", "GOUVERNEMENT", "MENTOR", "CRISE")
    private val situations = listOf(
        "Un incendie coupe une rue entière tandis qu'un suspect profite de la panique.",
        "Une vidéo floue prétend révéler ton identité civile et les journalistes encerclent le quartier.",
        "Un proche te demande d'être présent au moment exact où une urgence éclate à quelques rues.",
        "Un média influent te propose une interview qui pourrait changer la façon dont la ville te voit.",
        "Une organisation offre des ressources en échange d'une loyauté dont les limites restent volontairement vagues.",
        "Un autre surhumain intervient sur ton territoire et refuse de reconnaître ton autorité.",
        "Des civils sont coincés dans un bâtiment pendant que le responsable de la catastrophe tente de fuir.",
        "Un réseau criminel teste tes limites sans attaquer directement, certain que tu finiras par réagir.",
        "Ton pouvoir évolue brutalement et devient plus puissant que précis pendant quelques minutes.",
        "Une unité spéciale exige ta coopération après plusieurs incidents impliquant des êtres augmentés.",
        "Une figure expérimentée prétend pouvoir t'apprendre à survivre, mais sa réputation est controversée.",
        "Une anomalie frappe plusieurs quartiers à la fois et personne ne sait encore si elle est naturelle."
    )

    fun newCampaign(seed: Long, randomIdentity: Boolean = true): Campaign {
        val r = Random(seed)
        val firstNames = listOf("Malik", "Nora", "Elias", "Maya", "Soren", "Lina", "Ilyan", "Kael", "Naël", "Ava")
        val lastNames = listOf("Voss", "Deren", "Kess", "Arden", "Vale", "Nox", "Raine", "Sol", "Marek", "Serrin")
        val name = if (randomIdentity) "${firstNames[r.nextInt(firstNames.size)]} ${lastNames[r.nextInt(lastNames.size)]}" else "Alex Vesper"
        val aliasRoots = listOf("Vesper", "Axiom", "Morrow", "Cipher", "Silex", "Halo", "Noctis", "Vector", "Rift", "Cinder")
        return Campaign(
            seed = seed,
            name = name,
            alias = aliasRoots[r.nextInt(aliasRoots.size)],
            origin = origins[r.nextInt(origins.size)],
            powerFamily = powers[r.nextInt(powers.size)],
            weakness = weaknesses[r.nextInt(weaknesses.size)],
            modifier = modifiers[r.nextInt(modifiers.size)],
            power = 22 + r.nextInt(20),
            control = 18 + r.nextInt(28)
        )
    }

    fun event(c: Campaign): EventNode {
        val index = ((mix(c.seed, c.turn.toLong()) ushr 1) % 660L).toInt()
        val categoryIndex = index % categories.size
        val chapter = index / 110 + 1
        val category = categories[categoryIndex]
        val situation = situations[categoryIndex]
        val titles = listOf("La ligne rouge", "Visages dans la fumée", "Dette invisible", "Mauvais symbole", "Territoire neutre", "Le prix du silence", "Sous les projecteurs", "Après minuit", "Point de rupture", "La ville écoute", "Deux vérités", "Avant l'orage")
        val title = "${titles[(index / categories.size) % titles.size]} · ${index + 1}"
        val local = c.scope.label.lowercase()
        val text = "$situation À ${c.age} ans, ton influence reste à l'échelle $local. Chapitre $chapter : la décision prise ici peut revenir plusieurs années plus tard."
        val base = 1 + (index % 4)
        return EventNode(
            id = "evt_${index.toString().padStart(3, '0')}",
            title = title,
            text = text,
            category = category,
            choices = listOf(
                Choice("Protéger les civils d'abord", 7 + base, 3, 4, -1, 0, 4, 2),
                Choice("Poursuivre l'objectif principal", -1, 6, -1, 3, 2, 6, 5),
                Choice("Négocier et gagner du temps", 2, 2, 2, -2, 0, 3, 2),
                Choice("Imposer ta solution", -7 - base, 7, -3, 8, 3, 7, 7)
            )
        )
    }

    fun choose(c: Campaign, choice: Choice): Campaign {
        val roll = ((mix(c.seed xor 0x5EEDL, c.turn.toLong() * 17 + choice.label.hashCode()) ushr 2) % 100).toInt()
        val danger = max(0, choice.risk - c.control / 20)
        val injury = if (roll < danger * 4) 8 + danger * 2 else 0
        val casualties = if (choice.moral < -5 && roll < 35) 1 + roll % 4 else 0
        val exposure = if (roll < choice.risk * 3) 3 + choice.risk else 0
        val nextTurn = c.turn + 1
        val scopeGain = choice.impact + max(0, c.prestige / 120)
        val newPower = clamp(c.power + choice.power + if (nextTurn % 8 == 0) 1 else 0, 0, 100)
        val summary = "${18 + c.turn / 4} ans — ${choice.label} (${c.scope.label})"
        return c.copy(
            turn = nextTurn,
            morality = clamp(c.morality + choice.moral, -100, 100),
            prestige = max(0, c.prestige + choice.prestige + c.scope.ordinal),
            opinion = clamp(c.opinion + choice.opinion, -100, 100),
            fear = clamp(c.fear + choice.fear, 0, 100),
            power = newPower,
            control = clamp(c.control + if (choice.risk <= 3) 1 else 0, 0, 100),
            influence = max(0, c.influence + scopeGain),
            health = clamp(c.health - injury, 0, 100),
            civilianCasualties = c.civilianCasualties + casualties,
            identityExposure = clamp(c.identityExposure + exposure, 0, 100),
            timeline = (c.timeline + summary).takeLast(60)
        )
    }

    fun legacyTitle(c: Campaign): String {
        val heroic = c.morality >= 25
        return when (c.scope) {
            Scope.STREET -> if (heroic) "Gardien de la rue" else "Prédateur local"
            Scope.DISTRICT -> if (heroic) "Protecteur du quartier" else "Terreur du quartier"
            Scope.CITY -> if (heroic) "Gardien métropolitain" else "Fléau métropolitain"
            Scope.REGION -> if (heroic) "Défenseur régional" else "Seigneur criminel"
            Scope.COUNTRY -> if (heroic) "Symbole de la nation" else "Ennemi public national"
            Scope.WORLD -> if (heroic) "Gardien de la Terre" else "Ennemi de l'humanité"
        }
    }

    fun legacyScore(c: Campaign): Int = max(0, c.prestige + c.influence / 2 + c.power * 2 + abs(c.morality) * 2 + c.turn - c.civilianCasualties)

    private fun clamp(v: Int, min: Int, max: Int) = min(max, max(min, v))

    private fun mix(a: Long, b: Long): Long {
        var z = a + 0x9E3779B97F4A7C15UL.toLong() + b * 0xBF58476D1CE4E5B9UL.toLong()
        z = (z xor (z ushr 30)) * 0xBF58476D1CE4E5B9UL.toLong()
        z = (z xor (z ushr 27)) * 0x94D049BB133111EBUL.toLong()
        return z xor (z ushr 31)
    }
}
