package com.metahumanlegacy.game

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal object GameRules {
    fun apply(c: Campaign, current: EventNode, choice: Choice): Campaign = when (current.kind) {
        "FORMATIVE" -> applyFormative(c, current, choice)
        "AWAKENING" -> applyAwakening(c, current, choice)
        "FOUNDATION" -> applyFoundation(c, current, choice)
        "ENDING" -> applyEnding(c, current, choice)
        else -> applyMajor(c, current, choice)
    }

    private fun applyFormative(c: Campaign, current: EventNode, choice: Choice): Campaign {
        val nextTurn = c.turn + 1
        val flags = c.flags + choiceFlags(choice) + "seen:${current.id}"
        val affinity = addScores(c.affinityScores, choice.affinityDelta)
        val expression = addScores(c.expressionScores, choice.expressionDelta)
        val costs = addScores(c.costScores, choice.costDelta)
        val family = clamp(c.familyBond + choice.relationDelta, 0, 100)
        val risk = c.formativeRisk + choice.risk
        val base = c.copy(
            turn = nextTurn,
            morality = clamp(c.morality + choice.moral, -100, 100),
            familyBond = family,
            flags = flags,
            affinityScores = affinity,
            expressionScores = expression,
            costScores = costs,
            formativeRisk = risk,
            lastCategory = "VIE",
            lastApproach = "FORMATIVE",
            timeline = (c.timeline + "${c.age} ans — ${current.title} → ${choice.label}").takeLast(180)
        )
        return if (nextTurn == 10) PowerResolver.resolve(base) else base
    }

    private fun applyAwakening(c: Campaign, current: EventNode, choice: Choice): Campaign {
        val resolved = PowerResolver.resolve(c)
        val fixed = setOf("POWER_REVEALED", "FORMATIVE_DECADE_COMPLETE")
        val flags = resolved.flags + fixed + choiceFlags(choice) + "seen:${current.id}"
        val morality = when (choice.approach) {
            "CARE" -> 3
            "ASCEND" -> -2
            else -> 0
        }
        val control = if (choice.approach in setOf("ORDER", "TRUTH")) 2 else 0
        val exposure = if (choice.approach == "CARE") 2 else if (choice.approach == "ASCEND") 3 else 0
        return resolved.copy(
            turn = c.turn + 1,
            morality = clamp(resolved.morality + morality, -100, 100),
            control = clamp(resolved.control + control, 0, 100),
            identityExposure = clamp(resolved.identityExposure + exposure, 0, 100),
            flags = flags,
            lastCategory = "ÉVEIL",
            lastApproach = choice.approach,
            timeline = (resolved.timeline +
                "${resolved.age} ans — ${current.title} → ${choice.label}" +
                "↳ Ton pouvoir s'est révélé : ${resolved.powerFamily}. Tu ne l'as pas choisi ; tes dix années précédentes l'ont façonné."
            ).takeLast(180)
        )
    }

    private fun applyFoundation(c: Campaign, current: EventNode, choice: Choice): Campaign {
        val next = applyStatDeltas(c, current, choice, scale = 1.0)
        return next.copy(turn = c.turn + 1)
    }

    private fun applyMajor(c: Campaign, current: EventNode, choice: Choice): Campaign {
        val scale = when (choice.stakes) { 3 -> 1.5; 2 -> 1.25; else -> 1.0 }
        val next = applyStatDeltas(c, current, choice, scale)
        val threads = updateThread(next.threads, c, current, choice)
        val extraFlags = if (choice.deferredHook) setOf("DEFERRED:${current.threadId}:${c.turn}") else emptySet()
        return next.copy(
            turn = c.turn + 1,
            threads = threads,
            flags = next.flags + extraFlags
        )
    }

    private fun applyEnding(c: Campaign, current: EventNode, choice: Choice): Campaign {
        val arc = current.threadId
        val flags = c.flags + choiceFlags(choice) + "seen:${current.id}"
        return c.copy(
            turn = c.turn + 1,
            flags = flags,
            threads = if (arc == null) c.threads else c.threads.filterNot { it.id == arc },
            lastCategory = "ÉPILOGUE",
            lastApproach = current.provocation,
            timeline = (c.timeline + "${c.age} ans — ${current.title} → ${current.text}").takeLast(180)
        )
    }

    private fun applyStatDeltas(c: Campaign, current: EventNode, choice: Choice, scale: Double): Campaign {
        fun scaled(v: Int) = (v * scale).roundToInt()
        val nextTurn = c.turn + 1
        val weaknessRisk = if (
            current.category in setOf("POUVOIR", "RIVAL", "SANTÉ", "CRISE") &&
            c.weakness in setOf("Surcharge", "Fatigue extrême", "Concentration", "Instabilité émotionnelle")
        ) 5 else 0
        val injuryRoll = positiveMod(mix(c.seed, nextTurn * 131L + current.id.hashCode() + choice.label.hashCode()), 100)
        val injury = if (choice.risk >= 4 && injuryRoll < choice.risk * 4 + weaknessRisk) max(1, choice.risk / 2) else 0
        val flags = c.flags + choiceFlags(choice) + "seen:${current.id}" + "route:${choice.approach}:$nextTurn"

        val relation = scaled(choice.relationDelta) + if (choice.approach == "CARE" && c.motivation.contains("proche", true)) 1 else 0
        var family = c.familyBond
        var rival = c.rivalStanding
        var government = c.governmentStanding
        var faction = c.factionStanding
        var media = c.mediaStanding
        when (current.category) {
            "FAMILLE", "CIVIL", "JEUNESSE", "TRAUMA", "SANTÉ" -> family = clamp(family + relation, 0, 100)
            "RIVAL" -> rival = clamp(rival + relation, -100, 100)
            "GOUVERNEMENT", "POLITIQUE", "PAYS", "PRISON", "TRAQUE" -> government = clamp(government + relation, -100, 100)
            "FACTION", "MENTOR", "SIDEKICK", "REFUGE" -> faction = clamp(faction + relation, -100, 100)
            "MÉDIAS", "IDENTITÉ", "HÉRITAGE" -> media = clamp(media + relation, -100, 100)
        }

        val casualties = if (choice.approach == "ASCEND" && choice.risk >= 6 &&
            positiveMod(mix(c.seed, nextTurn * 313L + current.id.hashCode()), 100) < 5) 1 else 0

        return c.copy(
            morality = clamp(c.morality + scaled(choice.moral), -100, 100),
            prestige = max(0, c.prestige + scaled(choice.prestige)),
            opinion = clamp(c.opinion + scaled(choice.opinion), -100, 100),
            fear = clamp(c.fear + scaled(choice.fear), 0, 100),
            power = clamp(c.power + scaled(choice.power), 0, 100),
            control = clamp(c.control + if (choice.approach in setOf("ORDER", "TRUTH")) 1 else 0, 0, 100),
            influence = max(0, c.influence + max(0, scaled(choice.impact))),
            health = clamp(c.health + scaled(choice.healthDelta) - injury, 0, 100),
            civilianCasualties = c.civilianCasualties + casualties,
            identityExposure = clamp(c.identityExposure + scaled(choice.identityDelta), 0, 100),
            familyBond = family,
            rivalStanding = rival,
            governmentStanding = government,
            factionStanding = faction,
            mediaStanding = media,
            flags = flags,
            lastCategory = current.category,
            lastApproach = choice.approach,
            timeline = (c.timeline + "${c.age} ans — ${current.title} → ${choice.label}").takeLast(180)
        )
    }

    private fun updateThread(
        threads: List<StoryThread>, c: Campaign, current: EventNode, choice: Choice
    ): List<StoryThread> {
        val arc = current.threadId ?: return threads
        val existing = threads.firstOrNull { it.id == arc }
        val nextStage = if (current.threadStage >= 5) 6 else current.threadStage
        fun increment(t: StoryThread) = t.copy(
            lastTurn = c.turn,
            stage = max(t.stage, nextStage),
            lastApproach = choice.approach,
            intensity = max(t.intensity, choice.stakes),
            care = t.care + if (choice.approach == "CARE") 1 else 0,
            order = t.order + if (choice.approach == "ORDER") 1 else 0,
            truth = t.truth + if (choice.approach == "TRUTH") 1 else 0,
            ascend = t.ascend + if (choice.approach == "ASCEND") 1 else 0
        )
        return if (existing == null) {
            val base = StoryThread(arc, c.turn, c.turn, 0, "", choice.stakes)
            (threads + increment(base)).takeLast(8)
        } else {
            threads.map { if (it.id == arc) increment(it) else it }
        }
    }

    fun outcome(before: Campaign, after: Campaign, event: EventNode, choice: Choice): String = when (event.kind) {
        "FORMATIVE" -> {
            "Tu ne sais pas encore ce que cette décision construit. Pour l'instant, elle change surtout la personne que tu deviens — et le jeu garde le reste caché."
        }
        "AWAKENING" -> {
            "Ce pouvoir n'a pas été sélectionné : il s'est révélé à partir de tes dix années précédentes. ${after.powerSignature.replaceFirstChar { it.uppercase() }}. Ta réaction, elle, t'appartient."
        }
        "ENDING" -> "Cette conclusion ferme l'arc, mais ses conséquences et les choix qui l'ont créée restent inscrits dans ta carrière."
        else -> {
            val route = when (choice.approach) {
                "CARE" -> "Tu as privilégié les personnes avant le symbole."
                "ORDER" -> "Tu as imposé un cadre et une responsabilité claire."
                "TRUTH" -> "Tu as choisi de comprendre et d'exposer ce qui était caché."
                "ASCEND" -> "Tu as transformé la situation en levier de puissance et d'influence."
                else -> "Ta décision déplace durablement l'équilibre."
            }
            val deltas = mutableListOf<String>()
            fun add(label: String, v: Int) { if (v != 0) deltas += "$label ${signed(v)}" }
            add("moralité", after.morality - before.morality)
            add("prestige", after.prestige - before.prestige)
            add("opinion", after.opinion - before.opinion)
            add("peur", after.fear - before.fear)
            add("puissance", after.power - before.power)
            add("influence", after.influence - before.influence)
            add("santé", after.health - before.health)
            add("exposition", after.identityExposure - before.identityExposure)
            "$route ${if (deltas.isEmpty()) "L'effet principal est narratif." else deltas.joinToString(" · ") + "."}"
        }
    }

    fun legacyTitle(c: Campaign): String {
        if (!c.powerRevealed) return "Une vie encore ordinaire"
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

    fun legacyScore(c: Campaign) = max(
        0, c.prestige + c.influence / 2 + c.power * 2 + abs(c.morality) * 2 + c.turn - c.civilianCasualties * 2
    )

    private fun choiceFlags(choice: Choice): Set<String> =
        choice.flag.orEmpty().split('+').filter { it.isNotBlank() }.toSet()

    private fun addScores(base: Map<String, Int>, keys: List<String>): Map<String, Int> {
        if (keys.isEmpty()) return base
        val out = base.toMutableMap()
        keys.forEach { out[it] = (out[it] ?: 0) + 1 }
        return out
    }
}

internal fun clamp(v: Int, lo: Int, hi: Int) = min(hi, max(lo, v))
internal fun signed(v: Int) = if (v >= 0) "+$v" else v.toString()
internal fun mix(seed: Long, salt: Long): Long {
    var z = seed + salt + -7046029254386353131L
    z = (z xor (z ushr 30)) * -4658895280553007687L
    z = (z xor (z ushr 27)) * -7723592293110705685L
    return z xor (z ushr 31)
}
internal fun positiveMod(v: Long, n: Int): Int =
    if (n <= 1) 0 else (((v xor (v ushr 32)) and Long.MAX_VALUE) % n).toInt()
