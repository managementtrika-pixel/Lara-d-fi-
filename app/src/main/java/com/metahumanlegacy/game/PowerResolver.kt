package com.metahumanlegacy.game

internal object PowerResolver {
    private val affinityOptions = mapOf(
        "BODY" to listOf("Force", "Résistance", "Régénération", "Métamorphose"),
        "MOTION" to listOf("Vitesse", "Vol", "Gravité", "Espace"),
        "ENERGY" to listOf("Électricité", "Feu", "Glace", "Énergie"),
        "MIND" to listOf("Télépathie", "Illusion", "Influence mentale limitée", "Télékinésie"),
        "MATTER" to listOf("Matière", "Gravité", "Absorption", "Duplication"),
        "TECH" to listOf("Technologie", "Armes spécialisées", "Intelligence augmentée", "Armure adaptative"),
        "OCCULT" to listOf("Magie", "Invocation", "Projection astrale", "Métamorphose"),
        "COSMIC" to listOf("Énergie cosmique", "Gravité", "Espace", "Portails"),
        "ADAPTATION" to listOf("Adaptation", "Régénération", "Métamorphose", "Résistance")
    )

    private val costNames = mapOf(
        "STRAIN" to "Fatigue extrême",
        "EMOTIONAL" to "Instabilité émotionnelle",
        "ENVIRONMENT" to "Dépendance à l'environnement",
        "FUEL" to "Besoin d'une énergie externe",
        "FOCUS" to "Concentration",
        "INSTABILITY" to "Surcharge",
        "VISIBILITY" to "Pouvoir difficile à dissimuler",
        "COOLDOWN" to "Temps de récupération"
    )

    internal fun powerCatalog(): Set<String> = buildSet {
        affinityOptions.values.forEach { addAll(it) }
        addAll(listOf("Lumière", "Plasma", "Chaleur contrôlée"))
        addAll(listOf("Réflexes surhumains", "Propulsion physique", "Sauts cinétiques"))
        addAll(listOf("Précognition limitée", "Lecture émotionnelle", "Perception extrasensorielle"))
        addAll(listOf("Cristal", "Métal", "Transmutation limitée", "Construction de matière"))
        addAll(listOf("Interface neuronale", "Cybernétique", "Drones liés"))
        addAll(listOf("Magie symbolique", "Rêve", "Malédiction"))
        addAll(listOf("Portails limités", "Rayonnement stellaire"))
        addAll(listOf("Densité", "Résistance adaptative", "Métamorphose défensive"))
    }

    internal fun weaknessCatalog(): Set<String> = costNames.values.toSet()

    fun resolve(c: Campaign): Campaign {
        if (c.powerResolved) return c
        val affinity = ranked(c.affinityScores, c.seed xor 0xA11FL)
        val expression = ranked(c.expressionScores, c.seed xor 0xE771L)
        val cost = ranked(c.costScores, c.seed xor 0xC057L)
        val primary = affinity.firstOrNull() ?: "ADAPTATION"
        val secondary = affinity.getOrNull(1) ?: primary
        val dominantExpression = expression.firstOrNull() ?: "CONTROL"
        val dominantCost = cost.firstOrNull() ?: "STRAIN"

        val options = affinityOptions.getValue(primary)
        val optionIndex = positiveMod(
            mix(c.seed, (secondary.hashCode().toLong() shl 1) xor dominantExpression.hashCode().toLong()),
            options.size
        )
        var power = options[optionIndex]
        power = when {
            primary == "ENERGY" && dominantExpression == "CONTROL" ->
                listOf("Électricité", "Lumière", "Plasma", "Chaleur contrôlée")[positiveMod(mix(c.seed, 101L), 4)]
            primary == "BODY" && secondary == "MOTION" ->
                listOf("Vitesse", "Réflexes surhumains", "Propulsion physique", "Sauts cinétiques")[positiveMod(mix(c.seed, 102L), 4)]
            primary == "MIND" && dominantExpression == "PERCEPTION" ->
                listOf("Télépathie", "Précognition limitée", "Lecture émotionnelle", "Perception extrasensorielle")[positiveMod(mix(c.seed, 103L), 4)]
            primary == "MATTER" && dominantExpression == "CREATION" ->
                listOf("Cristal", "Métal", "Transmutation limitée", "Construction de matière")[positiveMod(mix(c.seed, 104L), 4)]
            primary == "TECH" && dominantExpression == "CONTROL" ->
                listOf("Interface neuronale", "Cybernétique", "Armure adaptative", "Drones liés")[positiveMod(mix(c.seed, 105L), 4)]
            primary == "OCCULT" && secondary == "MIND" ->
                listOf("Magie symbolique", "Rêve", "Malédiction", "Projection astrale")[positiveMod(mix(c.seed, 106L), 4)]
            primary == "COSMIC" && secondary == "ENERGY" ->
                listOf("Gravité", "Énergie cosmique", "Portails limités", "Rayonnement stellaire")[positiveMod(mix(c.seed, 107L), 4)]
            primary == "ADAPTATION" && dominantExpression == "DEFENSE" ->
                listOf("Régénération", "Densité", "Résistance adaptative", "Métamorphose défensive")[positiveMod(mix(c.seed, 108L), 4)]
            else -> power
        }

        val weakness = costNames[dominantCost] ?: "Surcharge"
        val origin = originFromHistory(c, primary)
        val signature = signatureFor(power, primary, dominantExpression)
        val reveal = revealFor(power, signature)
        val costText = costText(weakness)
        val totalAffinity = c.affinityScores.values.sum()
        val riskPotential = c.formativeRisk
        val startingPower = clamp(24 + totalAffinity / 3 + riskPotential / 3, 24, 58)
        val focus = c.costScores["FOCUS"] ?: 0
        val instability = c.costScores["INSTABILITY"] ?: 0
        val startingControl = clamp(30 + focus * 2 - instability * 2, 18, 55)

        return c.copy(
            origin = origin,
            powerFamily = power,
            weakness = weakness,
            power = startingPower,
            control = startingControl,
            powerRevealText = reveal,
            powerCostText = costText,
            powerSignature = signature
        )
    }

    private fun ranked(scores: Map<String, Int>, salt: Long): List<String> =
        scores.keys.sortedWith(
            compareByDescending<String> { scores[it] ?: 0 }
                .thenBy { positiveMod(mix(salt, it.hashCode().toLong()), 10_000) }
        )

    private fun originFromHistory(c: Campaign, primary: String): String = when {
        "PF08_TOUCH" in c.flags -> "Résonance avec le fragment noir"
        "PF04_TEST_ALL" in c.flags || "PF04_TEST_MEDICAL" in c.flags -> "Manifestation surveillée depuis des années"
        "PF07_RETURN" in c.flags || "PF10_ACCEPT" in c.flags -> "Éveil lié aux rêves récurrents"
        primary == "TECH" -> "Interface émergente inexpliquée"
        primary == "OCCULT" -> "Résonance occulte"
        primary == "COSMIC" -> "Résonance cosmique"
        primary == "BODY" || primary == "ADAPTATION" -> "Éveil physiologique latent"
        else -> "Manifestation inexpliquée"
    }

    private fun signatureFor(power: String, affinity: String, expression: String): String = when {
        power.contains("Électricité", true) -> "l'air crépite et les sources lumineuses vacillent"
        power.contains("Feu", true) || power.contains("Chaleur", true) -> "la température grimpe autour de tes mains"
        power.contains("Glace", true) -> "une pellicule de givre se forme sur ce que tu frôles"
        power.contains("Télépath", true) || affinity == "MIND" -> "les émotions et les pensées proches deviennent presque tangibles"
        power.contains("Vitesse", true) || affinity == "MOTION" -> "le monde paraît soudain trop lent autour de toi"
        power.contains("Techn", true) || power.contains("Interface", true) -> "les machines proches répondent avant même que tu les touches"
        affinity == "COSMIC" -> "l'espace autour de toi semble perdre sa géométrie habituelle"
        affinity == "OCCULT" -> "des symboles et sensations impossibles s'imposent à ta perception"
        expression == "DEFENSE" -> "ton corps encaisse ce qui aurait dû te briser"
        expression == "CREATION" -> "la matière répond à une intention que tu n'avais jamais su formuler"
        else -> "quelque chose dans ton corps et dans l'air autour de toi cesse d'obéir aux règles ordinaires"
    }

    private fun revealFor(power: String, signature: String): String =
        "Ce qui couvait depuis dix ans prend enfin une forme : $signature. La manifestation ressemble à ce que tu appelleras plus tard « $power »."

    private fun costText(weakness: String): String = when (weakness) {
        "Fatigue extrême" -> "chaque usage majeur vide brutalement tes réserves physiques"
        "Instabilité émotionnelle" -> "tes émotions peuvent amplifier ou dérégler la manifestation"
        "Dépendance à l'environnement" -> "certaines conditions extérieures peuvent affaiblir ou bloquer la manifestation"
        "Besoin d'une énergie externe" -> "ta capacité dépend d'une ressource que tu dois renouveler"
        "Concentration" -> "la moindre rupture de concentration peut interrompre ou déformer l'effet"
        "Surcharge" -> "forcer trop longtemps peut rendre le pouvoir imprévisible et dangereux"
        "Pouvoir difficile à dissimuler" -> "chaque usage laisse une signature trop visible pour rester longtemps secrète"
        "Temps de récupération" -> "après un effort important, la capacité refuse de répondre pendant un temps"
        else -> "toute utilisation importante possède un coût que ton corps ne peut pas ignorer"
    }
}
