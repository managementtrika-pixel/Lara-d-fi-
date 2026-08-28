package com.metahumanlegacy.game

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal object GameRules {
    fun apply(c: Campaign, current: EventNode, choice: Choice): Campaign {
        val scale = when (choice.stakes) { 3 -> 1.50; 2 -> 1.25; else -> 1.0 }
        fun scaled(v: Int) = (v * scale).roundToInt()
        val nextTurn = c.turn + 1
        val controlBonus = when {
            choice.approach == "TRUTH" && c.socialBackground == "Milieu scientifique" -> 2
            choice.approach == "ORDER" && c.socialBackground == "Famille militaire" -> 2
            choice.approach in setOf("TRUTH", "ORDER") -> 1
            else -> 0
        }
        val motivationPower = if (choice.approach == "ASCEND" && c.motivation == "Pouvoir") 1 else 0
        val motivationCare = if (choice.approach == "CARE" && c.motivation == "Protéger les miens") 1 else 0
        val exposureStyle = if (c.visualStyle == "Visage découvert" && choice.identityDelta > 0) 1 else 0
        val weaknessRisk = if (
            current.category in setOf("POUVOIR", "RIVAL", "SANTÉ", "CRISE") &&
            c.weakness in setOf("Surcharge", "Fatigue extrême", "Concentration", "Instabilité émotionnelle")
        ) 5 else 0
        val injuryRoll = positiveMod(mix(c.seed, nextTurn * 131L + current.id.hashCode() + choice.label.hashCode()), 100)
        val injury = if (choice.risk >= 4 && injuryRoll < choice.risk * 4 + weaknessRisk) max(1, choice.risk / 2) else 0

        val directFlags = choice.flag.orEmpty().split('+').filter { it.isNotBlank() }
        val flags = c.flags + directFlags + "seen:${current.id}" + "route:${choice.approach}:$nextTurn"
        val threads = updateThreads(c, current, choice, flags)
        val relation = scaled(choice.relationDelta) + motivationCare
        var family = c.familyBond; var rival = c.rivalStanding; var government = c.governmentStanding
        var faction = c.factionStanding; var media = c.mediaStanding
        when (current.category) {
            "FAMILLE", "CIVIL", "JEUNESSE", "TRAUMA", "SANTÉ" -> family = clamp(family + relation, 0, 100)
            "RIVAL" -> rival = clamp(rival + relation, -100, 100)
            "GOUVERNEMENT", "POLITIQUE", "PAYS", "PRISON", "TRAQUE" -> government = clamp(government + relation, -100, 100)
            "FACTION", "MENTOR", "SIDEKICK", "REFUGE" -> faction = clamp(faction + relation, -100, 100)
            "MÉDIAS", "IDENTITÉ", "HÉRITAGE" -> media = clamp(media + relation, -100, 100)
        }
        val casualties = if (choice.approach == "ASCEND" && choice.risk >= 4 && positiveMod(mix(c.seed, nextTurn * 313L + current.id.hashCode()), 100) < 5) 1 else 0
        val influenceGain = max(0, scaled(choice.impact) + when (choice.stakes) { 3 -> 3; 2 -> 2; else -> 1 })
        val moralityExtra = if (choice.approach == "ASCEND" && c.motivation == "Pouvoir") -1 else 0
        return c.copy(
            turn = nextTurn,
            morality = clamp(c.morality + scaled(choice.moral) + moralityExtra, -100, 100),
            prestige = max(0, c.prestige + scaled(choice.prestige)), opinion = clamp(c.opinion + scaled(choice.opinion), -100, 100),
            fear = clamp(c.fear + scaled(choice.fear), 0, 100),
            power = clamp(c.power + scaled(choice.power) + motivationPower + if (nextTurn % 16 == 0) 1 else 0, 0, 100),
            control = clamp(c.control + controlBonus, 0, 100), influence = max(0, c.influence + influenceGain),
            health = clamp(c.health + scaled(choice.healthDelta) - injury, 0, 100), civilianCasualties = c.civilianCasualties + casualties,
            identityExposure = clamp(c.identityExposure + scaled(choice.identityDelta) + exposureStyle, 0, 100),
            familyBond = family, rivalStanding = rival, governmentStanding = government, factionStanding = faction, mediaStanding = media,
            flags = flags, threads = threads, lastCategory = current.category, lastApproach = choice.approach,
            timeline = (c.timeline + "${c.age} ans — ${current.title} → ${choice.label}").takeLast(160)
        )
    }

    private fun updateThreads(c: Campaign, current: EventNode, choice: Choice, flags: Set<String>): List<StoryThread> {
        val arc = current.threadId ?: return c.threads
        val existing = c.threads.firstOrNull { it.id == arc }
        if ("${arc}_COMPLETE" in flags || current.id.contains("_EP_")) return c.threads.filterNot { it.id == arc }
        if (existing == null) return (c.threads + StoryThread(arc, c.turn, c.turn, current.threadStage, choice.approach, choice.stakes)).takeLast(8)
        return c.threads.map {
            if (it.id == arc) it.copy(lastTurn = c.turn, stage = max(it.stage, current.threadStage), lastApproach = choice.approach, intensity = max(it.intensity, choice.stakes)) else it
        }
    }

    fun outcome(before: Campaign, after: Campaign, event: EventNode, choice: Choice): String {
        val route = when (choice.approach) {
            "CARE" -> "Tu as privilégié les personnes avant le symbole."
            "ORDER" -> "Tu as imposé un cadre, des règles et une responsabilité claire."
            "TRUTH" -> "Tu as choisi de comprendre et d'exposer ce qui était caché."
            "ASCEND" -> "Tu as transformé la crise en levier de puissance et d'influence."
            else -> "Ta décision déplace durablement l'équilibre de cette histoire."
        }
        val world = when (event.category) {
            "RIVAL" -> if (after.rivalStanding > before.rivalStanding) "Ton rival te respecte davantage et retiendra cette méthode." else "La rivalité se durcit et devient plus personnelle."
            "FAMILLE", "CIVIL", "JEUNESSE", "TRAUMA" -> if (after.familyBond >= before.familyBond) "Tes liens civils absorbent une partie du choc." else "Ta double vie laisse une fissure qui pourra revenir."
            "GOUVERNEMENT", "POLITIQUE", "PAYS", "PRISON", "TRAQUE" -> "Les institutions enregistrent ton précédent et ajusteront leurs décisions futures."
            "MÉDIAS", "IDENTITÉ" -> "Le récit public et ton identité deviennent plus difficiles à contrôler."
            "POUVOIR", "TECH", "MYSTIQUE", "COSMIQUE" -> "Ta manière d'utiliser ton pouvoir devient une information exploitable par d'autres."
            else -> "Le monde conserve cette décision comme un précédent plutôt que comme un simple score."
        }
        val persistent = if ("${event.threadId}_COMPLETE" in after.flags || event.id.contains("_EP_"))
            "L'arc est conclu avec une fin persistante." else "La route ${choice.approach} conditionnera le prochain chapitre compatible de cet arc."
        val deltas = mutableListOf<String>()
        fun add(label: String, v: Int) { if (v != 0) deltas += "$label ${signed(v)}" }
        add("moralité", after.morality - before.morality); add("prestige", after.prestige - before.prestige)
        add("opinion", after.opinion - before.opinion); add("peur", after.fear - before.fear)
        add("puissance", after.power - before.power); add("influence", after.influence - before.influence)
        add("santé", after.health - before.health); add("exposition", after.identityExposure - before.identityExposure)
        return "$route $world $persistent ${if (deltas.isEmpty()) "Les effets sont surtout narratifs." else deltas.joinToString(" · ") + "."}"
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

    fun legacyScore(c: Campaign) = max(0, c.prestige + c.influence / 2 + c.power * 2 + abs(c.morality) * 2 + c.turn - c.civilianCasualties * 2)
}

internal fun clamp(v: Int, lo: Int, hi: Int) = min(hi, max(lo, v))
internal fun signed(v: Int) = if (v >= 0) "+$v" else v.toString()
internal fun mix(seed: Long, salt: Long): Long {
    var z = seed + salt + -7046029254386353131L
    z = (z xor (z ushr 30)) * -4658895280553007687L
    z = (z xor (z ushr 27)) * -7723592293110705685L
    return z xor (z ushr 31)
}
internal fun positiveMod(v: Long, n: Int): Int = if (n <= 1) 0 else (((v xor (v ushr 32)) and Long.MAX_VALUE) % n).toInt()
