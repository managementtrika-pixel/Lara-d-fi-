package com.metahumanlegacy.game

/**
 * Bridges techniques learned in the life simulation into authored story scenes.
 * This never invents a technique from raw control: only persisted, unlocked techniques are eligible.
 */
internal object StoryTechniqueDirector {
    private val crisisWords = setOf("POUVOIR", "RIVAL", "CRISE", "COMBAT", "MENACE", "CATASTROPHE", "SAUVETAGE", "ATTAQUE")

    fun enrich(c: Campaign, deep: DeepLifeState, base: EventNode): EventNode {
        if (!c.powerRevealed || base.kind in setOf("FORMATIVE", "AWAKENING", "ENDING")) return base
        val life = deep.lifeSimulation ?: return base
        if (!isTechniqueScene(base)) return base

        val available = life.powerRules.techniques
            .filter { it.unlocked && it.cooldownTurns <= 0 }
            .sortedWith(compareByDescending<TechniqueState> { it.proficiency }.thenByDescending { it.masteryRequired })
        if (available.isEmpty()) return base

        val technique = available[positive(c.seed xor base.id.hashCode().toLong(), available.size)]
        val tier = tier(technique)
        val proficiencyBonus = technique.proficiency / 25
        val risk = (base.stakes + tier - 1 - proficiencyBonus - life.powerRules.control / 45).coerceAtLeast(1)
        val architecture = DeepLifeDirector.architectureFor(c.powerFamily)
        val choice = Choice(
            label = "Utiliser « ${technique.name} »",
            moral = if (architecture in setOf(PowerArchitecture.BODY, PowerArchitecture.MOBILITY)) 1 else 0,
            prestige = if (tier >= 3) 2 else 1,
            opinion = if (architecture == PowerArchitecture.MENTAL) 0 else 1,
            fear = if (tier >= 4) 1 else 0,
            power = tier.coerceAtMost(3),
            impact = (1 + tier / 2).coerceAtMost(3),
            risk = risk,
            approach = approachFor(architecture),
            stakes = base.stakes,
            sourceCategory = base.category,
            identityDelta = if (tier >= 3) 2 else 1,
            healthDelta = if (life.powerRules.fatigue >= 70) -2 else 0,
            flag = "story_technique:${technique.id}"
        )
        val note = when {
            technique.proficiency >= 75 -> "« ${technique.name} » est devenue une signature : tu connais ses angles morts autant que ses forces."
            technique.proficiency >= 40 -> "Tu as assez pratiqué « ${technique.name} » pour l'utiliser autrement qu'en improvisation."
            else -> "« ${technique.name} » est disponible, mais la scène réelle reste plus dangereuse que l'entraînement."
        }

        val protected = base.choices.filter {
            it.flag?.startsWith("identity_pressure:") == true ||
                it.flag?.startsWith("scope_response_") == true
        }
        val ordinary = base.choices.filterNot {
            it in protected || it.flag?.startsWith("story_technique:") == true
        }
        val retained = if (base.choices.size < 7) {
            base.choices
        } else {
            ordinary.take((6 - protected.size).coerceAtLeast(0)) + protected
        }
        val choices = (retained + choice).distinctBy { it.label }.take(7)

        return base.copy(
            text = base.text + "\n\n" + note,
            choices = choices
        )
    }

    fun techniqueId(choice: Choice): String? = choice.flag
        ?.takeIf { it.startsWith("story_technique:") }
        ?.substringAfter(':')
        ?.takeIf { it.isNotBlank() }

    fun applyUse(c: Campaign, deep: DeepLifeState, choice: Choice): DeepLifeState {
        val techniqueId = techniqueId(choice) ?: return deep
        val life = deep.lifeSimulation ?: return deep
        val rules = life.powerRules
        val technique = rules.techniques.firstOrNull { it.id == techniqueId && it.unlocked } ?: return deep
        val tier = tier(technique)
        val fatigue = (4 + tier * 2 - technique.proficiency / 30).coerceAtLeast(3)
        val overload = (tier - rules.control / 40).coerceAtLeast(0)
        val nextTechniques = rules.techniques.map {
            if (it.id != techniqueId) it else it.copy(
                proficiency = (it.proficiency + 4).coerceAtMost(100),
                cooldownTurns = if (tier >= 3) 1 else 0,
                lastUsedTurn = c.turn
            )
        }
        val nextRules = rules.copy(
            fatigue = (rules.fatigue + fatigue).coerceAtMost(100),
            overload = (rules.overload + overload).coerceAtMost(100),
            techniques = nextTechniques
        )
        val nextLife = life.copy(
            powerRules = nextRules,
            secretIdentity = life.secretIdentity.copy(
                exposure = (life.secretIdentity.exposure + if (tier >= 3) 2 else 1).coerceAtMost(100)
            ),
            actionLog = (life.actionLog + "${c.age} ans · ${technique.name} utilisée dans une scène majeure").takeLast(80)
        )
        val evolution = deep.powerEvolution?.copy(
            mastery = maxOf(deep.powerEvolution.mastery, nextRules.control),
            strain = (deep.powerEvolution.strain + fatigue / 2).coerceAtMost(100),
            unlockedTechniques = (deep.powerEvolution.unlockedTechniques + "${technique.name} — ${c.powerFamily}").distinct()
        )
        return deep.copy(lifeSimulation = nextLife, powerEvolution = evolution)
    }

    private fun isTechniqueScene(event: EventNode): Boolean =
        event.stakes >= 3 || crisisWords.any { event.category.contains(it, ignoreCase = true) }

    private fun tier(t: TechniqueState): Int = when {
        t.masteryRequired >= 70 -> 4
        t.masteryRequired >= 55 -> 3
        t.masteryRequired >= 40 -> 2
        else -> 1
    }

    private fun approachFor(a: PowerArchitecture): String = when (a) {
        PowerArchitecture.BODY, PowerArchitecture.MOBILITY -> "CARE"
        PowerArchitecture.TECH, PowerArchitecture.PROJECTOR, PowerArchitecture.COSMIC -> "ORDER"
        PowerArchitecture.MENTAL, PowerArchitecture.MATTER, PowerArchitecture.ADAPTIVE -> "TRUTH"
        PowerArchitecture.OCCULT -> "ASCEND"
    }

    private fun positive(seed: Long, modulo: Int): Int {
        if (modulo <= 1) return 0
        val value = (seed xor (seed ushr 33) xor (seed shl 11)) and Long.MAX_VALUE
        return (value % modulo).toInt()
    }
}
