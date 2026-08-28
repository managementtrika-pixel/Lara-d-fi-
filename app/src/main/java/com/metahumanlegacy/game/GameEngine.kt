package com.metahumanlegacy.game

import java.util.Random
import kotlin.math.abs

object GameEngine {
    val pronouns = listOf("il", "elle", "iel")
    val cities = listOf("Vesper", "Greybridge", "Noxhaven", "Solara", "Kade City", "Oris", "Meridian", "Eidolon")
    val districts = listOf("Centre", "Les Docks", "Vieille-Ville", "Nord-Est", "Ceinture Sud", "Hauteurs", "Rives", "Secteur industriel")
    val socialBackgrounds = listOf("Quartier populaire", "Classe moyenne", "Milieu privilégié", "Foyer instable", "Famille militaire", "Milieu scientifique", "Autodidacte précaire", "Héritier d'une organisation")
    val motivations = listOf("Protéger les miens", "Justice", "Reconnaissance", "Pouvoir", "Liberté", "Réparer une faute", "Comprendre mes pouvoirs", "Changer le système")
    val visualStyles = listOf("Masque minimal", "Capuche tactique", "Silhouette civile", "Armure artisanale", "Tenue symbolique", "Visage découvert", "Manteau long", "Équipement modulaire")
    val origins = listOf("Mutation naturelle", "Accident scientifique", "Expérience clandestine", "Programme militaire", "Technologie personnelle", "Héritage familial", "Artefact mystérieux", "Pacte occulte", "Origine extraterrestre", "Énergie cosmique", "Entraînement humain extrême", "Intelligence augmentée")
    val powers = listOf("Force", "Résistance", "Vitesse", "Vol", "Énergie", "Feu", "Glace", "Électricité", "Télékinésie", "Télépathie", "Illusion", "Influence mentale limitée", "Métamorphose", "Invisibilité", "Régénération", "Technologie", "Armes spécialisées", "Magie", "Matière", "Gravité", "Espace", "Duplication", "Invocation", "Absorption", "Adaptation", "Humain exceptionnel")
    val weaknesses = listOf("Surcharge", "Fatigue extrême", "Concentration", "Énergie externe", "Fréquence sonore", "Instabilité émotionnelle", "Temps de récupération", "Environnement", "Vulnérabilité psychique", "Vulnérabilité mystique", "Pouvoir difficile à dissimuler", "Précision limitée")
    private val modifiers = listOf("Âge des héros", "Première génération", "Société méfiante", "Culture héroïque", "État autoritaire", "Criminalité endémique", "Ère technologique", "Menace occulte", "Silence cosmique", "Médias omniprésents")

    fun randomBlueprint(seed: Long): CharacterBlueprint {
        val r = Random(seed)
        val fn = listOf("Malik", "Nora", "Elias", "Maya", "Soren", "Lina", "Ilyan", "Kael", "Naël", "Ava", "Milo", "Yara", "Nell", "Zayn")
        val ln = listOf("Voss", "Deren", "Kess", "Arden", "Vale", "Nox", "Raine", "Sol", "Marek", "Serrin", "Vey", "Korr")
        val al = listOf("Vesper", "Axiom", "Morrow", "Cipher", "Silex", "Halo", "Noctis", "Vector", "Rift", "Cinder", "Mantis", "Aster")
        return CharacterBlueprint(
            fn[r.nextInt(fn.size)], ln[r.nextInt(ln.size)], al[r.nextInt(al.size)],
            pronouns[r.nextInt(pronouns.size)], cities[r.nextInt(cities.size)], districts[r.nextInt(districts.size)],
            socialBackgrounds[r.nextInt(socialBackgrounds.size)], origins[r.nextInt(origins.size)], powers[r.nextInt(powers.size)],
            weaknesses[r.nextInt(weaknesses.size)], motivations[r.nextInt(motivations.size)], visualStyles[r.nextInt(visualStyles.size)]
        )
    }

    fun newCampaign(seed: Long, blueprint: CharacterBlueprint = randomBlueprint(seed)): Campaign {
        val r = Random(seed xor 0x51A7L)
        val originPower = when (blueprint.origin) {
            "Programme militaire", "Entraînement humain extrême" -> 6
            "Énergie cosmique", "Origine extraterrestre" -> 9
            "Technologie personnelle", "Intelligence augmentée" -> 3
            else -> 5
        }
        val controlBonus = when (blueprint.socialBackground) {
            "Famille militaire" -> 7; "Milieu scientifique" -> 5; "Foyer instable" -> -3; else -> 1
        }
        val moralSeed = when (blueprint.motivation) {
            "Protéger les miens" -> 6; "Justice" -> 4; "Réparer une faute" -> 2; "Pouvoir" -> -6; else -> 0
        }
        val exposure = when (blueprint.visualStyle) { "Visage découvert" -> 38; "Silhouette civile" -> 10; else -> 0 }
        return Campaign(
            seed, blueprint.fullName, blueprint.alias.ifBlank { "Vesper" }, blueprint.origin, blueprint.powerFamily,
            blueprint.weakness, modifiers[r.nextInt(modifiers.size)], blueprint.pronouns, blueprint.city, blueprint.district,
            blueprint.socialBackground, blueprint.motivation, blueprint.visualStyle, morality = moralSeed,
            power = clamp(22 + originPower + r.nextInt(12), 10, 60), control = clamp(20 + controlBonus + r.nextInt(15), 10, 60),
            identityExposure = exposure, familyBond = if (blueprint.motivation == "Protéger les miens") 62 else 50,
            flags = setOf("origin:${blueprint.origin}", "motivation:${blueprint.motivation}", "background:${blueprint.socialBackground}", "style:${blueprint.visualStyle}", "power:${blueprint.powerFamily}", "weakness:${blueprint.weakness}")
        )
    }

    fun event(c: Campaign): EventNode = NarrativeRepository.event(c)
    fun choose(c: Campaign, choice: Choice): Campaign = GameRules.apply(c, event(c), choice)
    fun resolve(c: Campaign, event: EventNode, choice: Choice): Resolution {
        val next = GameRules.apply(c, event, choice)
        val outcome = GameRules.outcome(c, next, event, choice)
        return Resolution(next.copy(timeline = (next.timeline + "↳ $outcome").takeLast(160)), outcome)
    }
    fun legacyTitle(c: Campaign) = GameRules.legacyTitle(c)
    fun legacyScore(c: Campaign) = GameRules.legacyScore(c)

    internal fun catalogStats() = NarrativeRepository.stats()
    internal fun debugEventById(id: String) = NarrativeRepository.byId(id)
    internal fun debugAuthoredIds() = NarrativeRepository.ids()
    internal fun debugEffectsCount() = NarrativeRepository.effectsCount()
}
