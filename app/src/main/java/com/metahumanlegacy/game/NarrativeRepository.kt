package com.metahumanlegacy.game

import kotlin.math.abs

internal object NarrativeRepository {
    private val authored: List<Authored> by lazy { NarrativeCodec.catalog() }
    private val extras: Map<String, ExtraEffect> by lazy { NarrativeCodec.effects() }

    fun event(c: Campaign): EventNode {
        require(authored.isNotEmpty()) { "Narrative catalog is empty" }
        val activeIds = c.threads.map { it.id }.toSet()
        val eligible = authored.filter { a ->
            val requiredScope = scopeOf(a.scopeMin)
            val started = a.arc in activeIds
            c.age in a.minAge..a.maxAge &&
                (a.originReq == "*" || a.originReq == "-" || c.origin.equals(a.originReq, true)) &&
                (a.requiredFlag == "-" || a.requiredFlag == "*" || a.requiredFlag in c.flags) &&
                (a.forbiddenFlag == "-" || a.forbiddenFlag == "*" || a.forbiddenFlag !in c.flags) &&
                (!a.once || "seen:${a.id}" !in c.flags) &&
                (started || requiredScope.ordinal <= c.scope.ordinal + 1)
        }
        val pool = if (eligible.isNotEmpty()) eligible else authored.filter {
            it.id.endsWith("_S1") && "seen:${it.id}" !in c.flags && it.minAge <= c.age + 2
        }.ifEmpty { authored.filter { "seen:${it.id}" !in c.flags } }.ifEmpty { authored }
        val chosen = pool.maxByOrNull { eventScore(c, it, activeIds) } ?: pool.first()
        return toNode(chosen)
    }

    fun byId(id: String): EventNode? = authored.firstOrNull { it.id == id }?.let(::toNode)
    fun ids(): List<String> = authored.map { it.id }
    fun effectsCount(): Int = extras.size

    fun stats(): CatalogStats {
        fun <T> dup(values: List<T>) = values.groupingBy { it }.eachCount().count { it.value > 1 }
        return CatalogStats(
            authored.size, authored.sumOf { it.choices.size }, authored.map { it.arc }.toSet().size,
            authored.count { "_EP_" in it.id }, dup(authored.map { it.id }),
            dup(authored.map { it.title }), dup(authored.map { it.text })
        )
    }

    private fun eventScore(c: Campaign, a: Authored, activeIds: Set<String>): Double {
        val stage = stageOf(a.id)
        val active = a.arc in activeIds
        val scopeGap = scopeOf(a.scopeMin).ordinal - c.scope.ordinal
        val activeBoost = if (active) 100.0 + stage * 8.0 else 0.0
        val continuation = if (a.requiredFlag != "-" && a.requiredFlag in c.flags) 45.0 else 0.0
        val freshArc = if (a.id.endsWith("_S1")) 18.0 else 0.0
        val repeatPenalty = if (familyLabel(a.family) == c.lastCategory) -15.0 else 0.0
        val routeEcho = if (c.lastApproach.isNotBlank() && a.requiredFlag.endsWith("_${c.lastApproach}")) 6.0 else 0.0
        val scopeScore = when { scopeGap <= 0 -> 12.0; scopeGap == 1 -> 2.0; else -> -40.0 }
        val ageScore = -abs(c.age - a.minAge).coerceAtMost(12) * 0.6
        val rarity = when (a.rarity) { "RARE" -> 2.0; "LEGENDARY" -> 4.0; else -> 0.0 }
        val jitter = positiveMod(mix(c.seed, c.turn.toLong() * 991L + a.id.hashCode()), 10000) / 10000.0
        return a.weight * 10 + activeBoost + continuation + freshArc + repeatPenalty + routeEcho + scopeScore + ageScore + rarity + jitter
    }

    private fun toNode(a: Authored): EventNode {
        val stakes = when (a.phase) { "CRISIS", "CLIMAX" -> 3; "TENSION", "AFTERMATH" -> 2; else -> 1 }
        return EventNode(
            a.id, a.title, a.text, a.choices.map { toChoice(a, it, stakes) },
            familyLabel(a.family), a.phase, stakes, a.arc, stageOf(a.id)
        )
    }

    private fun toChoice(a: Authored, x: AuthChoice, stakes: Int): Choice {
        val route = routeFor(a.id, x.flags, x.tagToken)
        val effectRoute = if ("_EP_" in a.id) "CONTINUE" else route
        val extra = extras["${a.id}::$effectRoute"]
        val flags = (x.flags + (extra?.flags ?: emptyList())).distinct()
        return Choice(
            x.label, x.moral, x.prestige, x.opinion, x.fear, extra?.power ?: 0,
            extra?.influence ?: 0, x.risk, route, stakes, familyLabel(a.family), a.arc,
            extra?.relation ?: 0, flags.joinToString("+"), extra?.identity ?: 0, extra?.health ?: 0
        )
    }

    private fun routeFor(eventId: String, flags: List<String>, tag: String): String {
        val routes = listOf("CARE", "ORDER", "TRUTH", "ASCEND")
        for (route in routes) if (flags.any { it.endsWith("_$route") || it.contains("_${route}_S") || it.contains("_ENDING_$route") }) return route
        if ("_EP_" in eventId) routes.firstOrNull { eventId.substringAfter("_EP_").startsWith(it) }?.let { return it }
        return when (tag) {
            "care_choice", "rescue" -> "CARE"
            "truth_choice", "infiltration" -> "TRUTH"
            "ascend_choice", "domination", "humiliate", "betray" -> "ASCEND"
            else -> "ORDER"
        }
    }

    private fun stageOf(id: String): Int = if ("_EP_" in id) 6 else Regex("_S([1-5])(?:_|$)").find(id)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
}

internal fun familyLabel(family: String): String = when (family.uppercase()) {
    "POLITICS" -> "POLITIQUE"; "MEDIA" -> "MÉDIAS"; "GOVERNMENT" -> "GOUVERNEMENT"
    "FAMILY" -> "FAMILLE"; "IDENTITY" -> "IDENTITÉ"; "POWER" -> "POUVOIR"
    "COSMIC" -> "COSMIQUE"; "CITY" -> "VILLE"; "COUNTRY" -> "PAYS"; "CIVILIAN" -> "CIVIL"
    "HEALTH" -> "SANTÉ"; "CRISIS" -> "CRISE"; "YOUTH" -> "JEUNESSE"; "MYSTIC" -> "MYSTIQUE"
    "MANHUNT" -> "TRAQUE"; "LEGACY" -> "HÉRITAGE"; else -> family.uppercase()
}

internal fun scopeOf(raw: String): Scope = try { Scope.valueOf(raw.uppercase()) } catch (_: Throwable) { Scope.STREET }
