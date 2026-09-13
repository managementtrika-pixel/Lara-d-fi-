package com.metahumanlegacy.game

internal object LifePowerTechniqueCatalog {
    data class UseResult(
        val techniques: List<TechniqueState>,
        val fatigueDelta: Int,
        val overloadDelta: Int,
        val districtSafety: Int,
        val districtCrime: Int,
        val exposureDelta: Int,
        val headline: String,
        val detail: String
    )

    fun seeded(c: Campaign, existing: List<TechniqueState> = emptyList()): List<TechniqueState> {
        if (!c.powerRevealed) return existing
        val byId = existing.associateBy { it.id }
        return definitions(DeepLifeDirector.architectureFor(c.powerFamily)).map { definition ->
            val old = byId[definition.id]
            if (old == null) {
                definition.copy(unlocked = c.control >= definition.masteryRequired)
            } else {
                old.copy(
                    name = definition.name,
                    masteryRequired = definition.masteryRequired,
                    unlocked = old.unlocked || c.control >= definition.masteryRequired
                )
            }
        }
    }

    fun train(c: Campaign, rules: PowerRulesState): Pair<PowerRulesState, String?> {
        val seeded = seeded(c, rules.techniques)
        val focus = seeded.firstOrNull { !it.unlocked }
            ?: seeded.minByOrNull { it.proficiency }
            ?: return rules to null
        val beforeUnlocked = focus.unlocked
        val gain = when {
            rules.control < 35 -> 8
            rules.control < 60 -> 6
            else -> 4
        }
        val nextControl = (rules.control + 3).coerceAtMost(100)
        val next = seeded.map { technique ->
            if (technique.id != focus.id) technique
            else {
                val proficiency = (technique.proficiency + gain).coerceAtMost(100)
                technique.copy(
                    proficiency = proficiency,
                    unlocked = technique.unlocked || nextControl >= technique.masteryRequired || proficiency >= 35
                )
            }
        }
        val trained = next.first { it.id == focus.id }
        val unlockedText = if (!beforeUnlocked && trained.unlocked) "Technique débloquée : ${trained.name}." else null
        return rules.copy(
            control = nextControl,
            precision = (rules.precision + 2).coerceAtMost(100),
            fatigue = (rules.fatigue + 9).coerceAtMost(100),
            overload = (rules.overload + if (rules.fatigue > 70) 8 else 2).coerceAtMost(100),
            techniques = next
        ) to unlockedText
    }

    fun use(c: Campaign, rules: PowerRulesState, techniqueId: String): UseResult? {
        val seeded = seeded(c, rules.techniques)
        val technique = seeded.firstOrNull { it.id == techniqueId && it.unlocked } ?: return null
        if (technique.cooldownTurns > 0) return null
        val tier = when {
            technique.masteryRequired >= 70 -> 4
            technique.masteryRequired >= 55 -> 3
            technique.masteryRequired >= 40 -> 2
            else -> 1
        }
        val strainDiscount = (technique.proficiency / 25).coerceIn(0, 3)
        val fatigue = (5 + tier * 2 - strainDiscount).coerceAtLeast(3)
        val overload = (tier - rules.control / 35).coerceAtLeast(0)
        val nextTechniques = seeded.map {
            if (it.id == technique.id) it.copy(
                proficiency = (it.proficiency + 3).coerceAtMost(100),
                cooldownTurns = if (tier >= 3) 1 else 0,
                lastUsedTurn = c.turn
            ) else it
        }
        return UseResult(
            techniques = nextTechniques,
            fatigueDelta = fatigue,
            overloadDelta = overload,
            districtSafety = 2 + tier,
            districtCrime = 1 + tier / 2,
            exposureDelta = if (tier >= 3) 2 else 1,
            headline = technique.name,
            detail = "Tu utilises ${technique.name.lowercase()} comme une vraie technique maîtrisée. Sa précision progresse, mais ton corps et ton identité paient encore une partie du coût."
        )
    }

    fun tickCooldowns(techniques: List<TechniqueState>): List<TechniqueState> = techniques.map {
        if (it.cooldownTurns > 0) it.copy(cooldownTurns = it.cooldownTurns - 1) else it
    }

    private fun definitions(a: PowerArchitecture): List<TechniqueState> = when (a) {
        PowerArchitecture.PROJECTOR -> listOf(
            t("projector_focus", "Tir focalisé", 25), t("projector_zone", "Zone contrôlée", 40),
            t("projector_deflect", "Déviation", 55), t("projector_signature", "Décharge signature", 70)
        )
        PowerArchitecture.MENTAL -> listOf(
            t("mental_focus", "Lecture ciblée", 25), t("mental_screen", "Écran mental", 40),
            t("mental_emotion", "Projection émotionnelle", 55), t("mental_network", "Réseau psychique", 70)
        )
        PowerArchitecture.BODY -> listOf(
            t("body_anchor", "Ancrage", 25), t("body_impact", "Impact contrôlé", 40),
            t("body_recovery", "Récupération active", 55), t("body_peak", "Forme de pointe", 70)
        )
        PowerArchitecture.MOBILITY -> listOf(
            t("mobility_extract", "Extraction rapide", 25), t("mobility_path", "Trajectoire impossible", 40),
            t("mobility_transport", "Transport assisté", 55), t("mobility_signature", "Mouvement signature", 70)
        )
        PowerArchitecture.MATTER -> listOf(
            t("matter_fine", "Façonnage fin", 25), t("matter_reinforce", "Renforcement", 40),
            t("matter_build", "Construction rapide", 55), t("matter_architecture", "Architecture instantanée", 70)
        )
        PowerArchitecture.TECH -> listOf(
            t("tech_diagnostic", "Diagnostic tactique", 25), t("tech_drones", "Drones coordonnés", 40),
            t("tech_counter", "Contre-mesures", 55), t("tech_signature", "Système signature", 70)
        )
        PowerArchitecture.OCCULT -> listOf(
            t("occult_seal", "Sceau stable", 25), t("occult_ritual", "Rituel court", 40),
            t("occult_guard", "Protection liée", 55), t("occult_signature", "Invocation signature", 70)
        )
        PowerArchitecture.COSMIC -> listOf(
            t("cosmic_curve", "Courbure locale", 25), t("cosmic_field", "Champ stabilisé", 40),
            t("cosmic_anchor", "Ancrage spatial", 55), t("cosmic_signature", "Phénomène signature", 70)
        )
        PowerArchitecture.ADAPTIVE -> listOf(
            t("adaptive_response", "Réponse ciblée", 25), t("adaptive_memory", "Mémoire corporelle", 40),
            t("adaptive_mutation", "Mutation contrôlée", 55), t("adaptive_signature", "Adaptation signature", 70)
        )
    }

    private fun t(id: String, name: String, mastery: Int) = TechniqueState(
        id = id,
        name = name,
        masteryRequired = mastery
    )
}
