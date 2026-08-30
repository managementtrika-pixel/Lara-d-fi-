package com.metahumanlegacy.game

import java.util.Random

object GameEngine {
    val pronouns = listOf("il", "elle", "iel")
    val cities = listOf("Vesper", "Greybridge", "Noxhaven", "Solara", "Kade City", "Oris", "Meridian", "Eidolon")
    val districts = listOf("Centre", "Les Docks", "Vieille-Ville", "Nord-Est", "Ceinture Sud", "Hauteurs", "Rives", "Secteur industriel")
    val socialBackgrounds = listOf(
        "Quartier populaire", "Classe moyenne", "Milieu privilégié", "Foyer instable",
        "Famille militaire", "Milieu scientifique", "Autodidacte précaire", "Famille très présente"
    )
    val motivations = listOf(
        "Protéger mes proches", "Justice", "Reconnaissance", "Liberté",
        "Réparer une faute", "Comprendre le monde", "Changer le système", "Réussir ma vie"
    )
    val civilianPaths = listOf(
        "Études scientifiques", "Métier manuel", "Sécurité / secours", "Création artistique",
        "Droit / service public", "Technologie", "Commerce / indépendance", "Parcours encore incertain"
    )
    val temperaments = listOf(
        "Prudent", "Curieux", "Protecteur", "Ambitieux",
        "Méfiant", "Discipliné", "Impulsif", "Indépendant"
    )
    private val modifiers = listOf(
        "Âge des héros", "Première génération", "Société méfiante", "Culture héroïque",
        "État autoritaire", "Criminalité endémique", "Ère technologique", "Menace occulte",
        "Silence cosmique", "Médias omniprésents"
    )

    fun randomBlueprint(seed: Long): CharacterBlueprint {
        val r = Random(seed)
        val fn = listOf("Malik", "Nora", "Elias", "Maya", "Soren", "Lina", "Kael", "Naël", "Ava", "Milo", "Yara", "Nell", "Zayn")
        val ln = listOf("Voss", "Deren", "Kess", "Arden", "Vale", "Nox", "Raine", "Sol", "Marek", "Serrin", "Vey", "Korr")
        return CharacterBlueprint(
            fn[r.nextInt(fn.size)], ln[r.nextInt(ln.size)],
            pronouns[r.nextInt(pronouns.size)],
            cities[r.nextInt(cities.size)], districts[r.nextInt(districts.size)],
            socialBackgrounds[r.nextInt(socialBackgrounds.size)],
            motivations[r.nextInt(motivations.size)],
            civilianPaths[r.nextInt(civilianPaths.size)],
            temperaments[r.nextInt(temperaments.size)]
        )
    }

    fun newCampaign(seed: Long, blueprint: CharacterBlueprint = randomBlueprint(seed)): Campaign {
        val r = Random(seed xor 0x51A7L)
        val moralSeed = when (blueprint.motivation) {
            "Protéger mes proches" -> 4
            "Justice" -> 3
            "Réparer une faute" -> 2
            "Reconnaissance" -> -1
            else -> 0
        }
        val controlSeed = when {
            blueprint.temperament == "Discipliné" -> 5
            blueprint.temperament == "Prudent" -> 3
            blueprint.temperament == "Impulsif" -> -2
            blueprint.socialBackground == "Famille militaire" -> 3
            else -> 0
        }
        val family = when {
            blueprint.motivation == "Protéger mes proches" -> 62
            blueprint.socialBackground == "Foyer instable" -> 38
            blueprint.socialBackground == "Famille très présente" -> 68
            else -> 50
        }
        return Campaign(
            seed = seed,
            name = blueprint.fullName,
            modifier = modifiers[r.nextInt(modifiers.size)],
            pronouns = blueprint.pronouns,
            city = blueprint.city,
            district = blueprint.district,
            socialBackground = blueprint.socialBackground,
            motivation = blueprint.motivation,
            civilianPath = blueprint.civilianPath,
            temperament = blueprint.temperament,
            morality = moralSeed,
            control = clamp(15 + controlSeed, 5, 35),
            familyBond = family,
            flags = setOf(
                "status:NOBODY",
                "power_state:UNRESOLVED",
                "motivation:${blueprint.motivation}",
                "background:${blueprint.socialBackground}",
                "civilian_path:${blueprint.civilianPath}",
                "temperament:${blueprint.temperament}",
                "deep:v1"
            )
        )
    }

    /**
     * The authored repository still chooses the canonical story beat. Two deterministic depth
     * passes then make accumulated life experience relevant without replacing the authored arc.
     * Formative/awakening beats are deliberately left structurally untouched.
     */
    fun event(c: Campaign): EventNode {
        val authored = NarrativeRepository.event(c)
        val deep = DepthDirector.enrichEvent(c, authored)
        return CareerVariationDirector.enrich(c, deep)
    }

    fun resolve(c: Campaign, event: EventNode, choice: Choice): Resolution {
        val rawNext = GameRules.apply(c, event, choice)
        val deep = DepthDirector.afterChoice(c, rawNext, event, choice)
        val next = deep.campaign
        val fallback = GameRules.outcome(c, next, event, choice)
        val choiceIndex = event.choices.indexOf(choice)
        val choiceId = if (choiceIndex >= 0) "${event.id}_C${choiceIndex + 1}" else ""
        val constat = choiceId.takeIf { it.isNotBlank() }?.let(NarrativeCodec::constat)
        val authored = constat?.let { "${it.title}\n\n${it.text}" } ?: fallback
        val outcome = if (deep.echo.isBlank()) authored else "$authored\n\nÉCHOS DE TA VIE\n${deep.echo}"
        val timelineNote = constat?.title ?: fallback
        return Resolution(
            next.copy(timeline = (next.timeline + "↳ $timelineNote").takeLast(180)),
            outcome
        )
    }

    fun setAlias(c: Campaign, alias: String): Campaign {
        val cleaned = alias.trim().take(28)
        if (cleaned.isBlank()) return c
        return c.copy(
            alias = cleaned,
            visualStyle = "Identité à construire",
            flags = c.flags + "ALIAS_CHOSEN",
            timeline = (c.timeline + "${c.age} ans — Tu choisis enfin le nom « $cleaned ».").takeLast(180)
        )
    }

    fun legacyTitle(c: Campaign) = DepthDirector.legacyTitle(c, GameRules.legacyTitle(c))
    fun legacyScore(c: Campaign) = (GameRules.legacyScore(c) + DepthDirector.legacyScoreBonus(c)).coerceAtLeast(0)
    fun legacySummary(c: Campaign) = DepthDirector.legacySummary(c)
    internal fun depthDossier(c: Campaign) = DepthDirector.dossier(c)

    internal fun catalogStats() = NarrativeRepository.stats()
    internal fun constatCount() = NarrativeCodec.constatCount()
    internal fun debugEventById(id: String, c: Campaign? = null) = NarrativeRepository.byId(id, c)
    internal fun debugAuthoredIds() = NarrativeRepository.ids()
}
