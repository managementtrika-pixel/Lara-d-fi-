package com.metahumanlegacy.game

/** Gives power architectures different decision grammar instead of only different labels/stats. */
internal object PowerGameplayDirector {
    fun enrich(c: Campaign, base: EventNode): EventNode {
        if (!c.powerRevealed || base.kind in setOf("FORMATIVE", "AWAKENING", "ENDING")) return base
        val architecture = DeepLifeDirector.architectureFor(c.powerFamily)
        val notes = mutableListOf<String>()
        val extra = mutableListOf<Choice>()
        val crisis = base.category.uppercase() in setOf("POUVOIR", "RIVAL", "CRISE", "COMBAT", "MENACE", "CATASTROPHE", "SAUVETAGE") || base.stakes >= 3

        if (crisis && base.choices.size < 7) {
            extra += architectureChoice(architecture, c, base)
            notes += architectureNote(architecture)
        }
        weaknessNote(c, base)?.let { notes += it }
        if (c.age >= 45 && c.control >= 70 && crisis && base.choices.size + extra.size < 7) {
            extra += Choice(
                label = "Laisser l'expérience remplacer la force brute",
                prestige = 1, opinion = 1, impact = 1,
                risk = (base.stakes - 1).coerceAtLeast(1), approach = "ORDER",
                stakes = base.stakes, sourceCategory = base.category, healthDelta = 1,
                flag = "v2_veteran_mastery"
            )
            notes += "Avec l'âge, tu ne peux plus compter uniquement sur la récupération. L'expérience devient une vraie ressource."
        }

        val merged = (base.choices + extra).distinctBy { it.label }.take(7)
        val mechanicallyBound = if (crisis) merged.map { applyWeakness(c, it) } else merged
        if (extra.isEmpty() && notes.isEmpty() && mechanicallyBound == base.choices) return base
        return base.copy(
            text = buildString {
                append(base.text)
                if (notes.isNotEmpty()) append("\n\n").append(notes.distinct().take(3).joinToString("\n"))
            },
            choices = mechanicallyBound
        )
    }

    /** A weakness only modifies decisions that actually lean on the power. */
    internal fun applyWeakness(c: Campaign, choice: Choice): Choice {
        val usesPower = choice.power > 0 || choice.flag?.startsWith("v2_power_") == true
        if (!usesPower) return choice
        return when (c.weakness) {
            "Fatigue extrême" -> choice.copy(
                risk = choice.risk + 1,
                healthDelta = choice.healthDelta - 3,
                flag = mergeFlag(choice.flag, "weakness_strain")
            )
            "Instabilité émotionnelle" -> choice.copy(
                risk = choice.risk + if (choice.approach in setOf("CARE", "ASCEND")) 2 else 1,
                fear = choice.fear + 1,
                flag = mergeFlag(choice.flag, "weakness_emotional")
            )
            "Dépendance à l'environnement" -> choice.copy(
                risk = choice.risk + 2,
                impact = (choice.impact - 1).coerceAtLeast(0),
                flag = mergeFlag(choice.flag, "weakness_environment")
            )
            "Besoin d'une énergie externe" -> choice.copy(
                risk = choice.risk + 1,
                power = (choice.power - 1).coerceAtLeast(0),
                healthDelta = choice.healthDelta - 1,
                flag = mergeFlag(choice.flag, "weakness_fuel")
            )
            "Concentration" -> choice.copy(
                risk = choice.risk + 2,
                relationDelta = choice.relationDelta - if (choice.approach == "CARE") 1 else 0,
                flag = mergeFlag(choice.flag, "weakness_focus")
            )
            "Surcharge" -> choice.copy(
                risk = choice.risk + 2,
                healthDelta = choice.healthDelta - 2,
                power = choice.power + 1,
                flag = mergeFlag(choice.flag, "weakness_overload")
            )
            "Pouvoir difficile à dissimuler" -> choice.copy(
                identityDelta = choice.identityDelta + 2,
                opinion = choice.opinion + if (choice.impact > 0) 1 else 0,
                flag = mergeFlag(choice.flag, "weakness_visibility")
            )
            "Temps de récupération" -> choice.copy(
                risk = choice.risk + 1,
                healthDelta = choice.healthDelta - 1,
                deferredHook = true,
                flag = mergeFlag(choice.flag, "weakness_cooldown")
            )
            else -> choice
        }
    }

    private fun mergeFlag(original: String?, weakness: String): String = when {
        original.isNullOrBlank() -> weakness
        else -> "$original+$weakness"
    }

    private fun architectureChoice(a: PowerArchitecture, c: Campaign, e: EventNode): Choice = when (a) {
        PowerArchitecture.PROJECTOR -> Choice(
            "Modeler la puissance plutôt que tirer au maximum",
            power = 1, prestige = 1, impact = 1, risk = (e.stakes - if (c.control >= 55) 1 else 0).coerceAtLeast(1),
            approach = "ORDER", stakes = e.stakes, sourceCategory = e.category,
            identityDelta = 1, flag = "v2_power_projector_control"
        )
        PowerArchitecture.MENTAL -> Choice(
            "Chercher une ouverture mentale sans franchir la ligne du consentement",
            power = 1, moral = 1, opinion = 1, impact = 1, risk = e.stakes,
            approach = "TRUTH", stakes = e.stakes, sourceCategory = e.category,
            relationDelta = 1, flag = "v2_power_mental_boundary"
        )
        PowerArchitecture.BODY -> Choice(
            "Encaisser pour créer une fenêtre aux autres",
            power = 1, moral = 1, prestige = 1, impact = 1, risk = e.stakes + 1,
            approach = "CARE", stakes = e.stakes, sourceCategory = e.category,
            healthDelta = -1, flag = "v2_power_body_anchor"
        )
        PowerArchitecture.MOBILITY -> Choice(
            "Transformer la vitesse en évacuation plutôt qu'en poursuite",
            power = 1, moral = 2, opinion = 1, impact = 1, risk = (e.stakes - 1).coerceAtLeast(1),
            approach = "CARE", stakes = e.stakes, sourceCategory = e.category,
            flag = "v2_power_mobility_extract"
        )
        PowerArchitecture.MATTER -> Choice(
            "Modifier le décor pour supprimer le danger à sa source",
            power = 1, prestige = 1, impact = 2, risk = e.stakes,
            approach = "TRUTH", stakes = e.stakes, sourceCategory = e.category,
            identityDelta = 1, flag = "v2_power_matter_shape"
        )
        PowerArchitecture.TECH -> Choice(
            "Déployer tes systèmes avant de t'exposer physiquement",
            power = 1, prestige = 1, impact = 1, risk = (e.stakes - 1).coerceAtLeast(1),
            approach = "ORDER", stakes = e.stakes, sourceCategory = e.category,
            flag = "v2_power_tech_remote"
        )
        PowerArchitecture.OCCULT -> Choice(
            "Payer un coût contrôlé maintenant pour éviter un prix incontrôlable ensuite",
            power = 1, impact = 2, risk = e.stakes + 1,
            approach = "TRUTH", stakes = e.stakes, sourceCategory = e.category,
            healthDelta = -1, flag = "v2_power_occult_price"
        )
        PowerArchitecture.COSMIC -> Choice(
            "Employer une fraction du phénomène et préserver la zone",
            power = 2, prestige = 1, impact = 2, risk = e.stakes + if (c.control < 60) 2 else 0,
            approach = "ORDER", stakes = e.stakes, sourceCategory = e.category,
            identityDelta = 2, flag = "v2_power_cosmic_fraction"
        )
        PowerArchitecture.ADAPTIVE -> Choice(
            "Observer la menace assez longtemps pour laisser ton corps trouver sa réponse",
            power = 1, impact = 1, risk = e.stakes,
            approach = "TRUTH", stakes = e.stakes, sourceCategory = e.category,
            healthDelta = if (c.health < 45) -1 else 0, flag = "v2_power_adaptive_read"
        )
    }

    private fun architectureNote(a: PowerArchitecture): String = when (a) {
        PowerArchitecture.PROJECTOR -> "Ton vrai enjeu n'est pas seulement la puissance : portée, précision et dommages autour de la cible comptent autant."
        PowerArchitecture.MENTAL -> "Un pouvoir mental ouvre des raccourcis impossibles aux autres — et des frontières morales qu'eux n'ont jamais à franchir."
        PowerArchitecture.BODY -> "Ton corps est ton outil principal. Chaque solution physique pose aussi la question de ce qu'il restera de toi après l'impact."
        PowerArchitecture.MOBILITY -> "Arriver le premier ne signifie pas pouvoir être partout : la priorité que tu choisis devient le vrai pouvoir."
        PowerArchitecture.MATTER -> "Modifier le décor peut sauver une scène ou la rendre inhabitable. La structure devient une partie de la décision."
        PowerArchitecture.TECH -> "Tes systèmes permettent d'agir à distance, mais maintenance, accès et dépendance technologique limitent l'improvisation."
        PowerArchitecture.OCCULT -> "Ici, chaque raccourci possède un prix. Ce qui compte n'est pas seulement si tu peux agir, mais ce que tu acceptes de payer."
        PowerArchitecture.COSMIC -> "Ton échelle de puissance dépasse souvent l'échelle du problème. La retenue devient une compétence de survie collective."
        PowerArchitecture.ADAPTIVE -> "Tu deviens meilleur face à ce qui t'a déjà blessé ; la première exposition reste toujours la plus dangereuse."
    }

    private fun weaknessNote(c: Campaign, e: EventNode): String? {
        if (e.stakes < 2) return null
        return when (c.weakness) {
            "Fatigue extrême" -> "Forcer ici te coûte réellement de la santé et augmente le risque de la décision."
            "Instabilité émotionnelle" -> "Une décision émotionnelle augmente réellement la peur et l'imprévisibilité de ton pouvoir."
            "Dépendance à l'environnement" -> "Un environnement défavorable réduit l'efficacité et augmente le risque de tes solutions métahumaines."
            "Besoin d'une énergie externe" -> "Tes réserves réduisent la puissance disponible et font payer l'effort à ton corps."
            "Concentration" -> "Les solutions qui utilisent ton pouvoir deviennent plus fragiles aux interruptions."
            "Surcharge" -> "Tu peux arracher plus de puissance, mais au prix d'un risque et d'un coût physique supérieurs."
            "Pouvoir difficile à dissimuler" -> "Chaque usage métahumain ajoute désormais de vraies traces à ton identité."
            "Temps de récupération" -> "Un usage important crée une dette différée : la prochaine séquence peut te trouver sans cette ressource."
            else -> null
        }
    }
}
