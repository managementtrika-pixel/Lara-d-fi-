package com.metahumanlegacy.game

import kotlin.math.abs

internal object NarrativeRepository {
    private val prologue by lazy { NarrativeCodec.prologue() }
    private val awakening by lazy { NarrativeCodec.awakening() }
    private val foundation by lazy { NarrativeCodec.foundation() }
    private val beats by lazy { NarrativeCodec.beats() }
    private val endings by lazy { NarrativeCodec.endings() }
    private val beatsByArc by lazy { beats.groupBy { it.arc } }
    private val arcStart by lazy { beats.filter { it.stage == 1 }.associateBy { it.arc } }

    fun event(c: Campaign): EventNode {
        return when {
            c.turn < 10 -> formativeNode(prologue[c.turn])
            c.turn == 10 -> awakeningNode(c)
            c.turn in 11..15 -> foundationNode(c, foundation[c.turn - 11])
            else -> metahumanEvent(c)
        }
    }

    fun byId(id: String, c: Campaign? = null): EventNode? {
        prologue.firstOrNull { it.id == id }?.let { return formativeNode(it) }
        if (awakening.id == id) return awakeningNode(c ?: dummyCampaign())
        foundation.firstOrNull { it.id == id }?.let { return foundationNode(c ?: dummyCampaign(), it) }
        beats.firstOrNull { it.id == id }?.let { return majorNode(c ?: dummyCampaign(), it) }
        return null
    }

    fun stats() = CatalogStats(
        prologue = prologue.size,
        foundation = foundation.size,
        majorBeats = beats.size,
        majorChoices = beats.sumOf { it.choices.size },
        arcs = beats.map { it.arc }.toSet().size,
        endings = endings.values.sumOf { it.size }
    )

    fun ids(): List<String> = prologue.map { it.id } + listOf(awakening.id) +
        foundation.map { it.id } + beats.map { it.id }

    fun endingText(arc: String, route: String): String = endings[arc]?.get(route)
        ?: "Cet arc se referme, mais ses conséquences restent dans ta carrière."

    private fun metahumanEvent(c: Campaign): EventNode {
        val active = c.threads.filterNot { "${it.id}_COMPLETE" in c.flags }
        active.firstOrNull { it.stage >= 6 }?.let { return endingNode(c, it) }

        val readyContinuations = active.mapNotNull { thread ->
            val nextStage = thread.stage + 1
            val beat = beatsByArc[thread.id]?.firstOrNull { it.stage == nextStage } ?: return@mapNotNull null
            val gap = continuationGap(c, thread)
            if (c.turn - thread.lastTurn >= gap) beat else null
        }

        val canOpenAnother = active.size < 4
        val startCandidates = if (canOpenAnother) {
            arcStart.values.filter { beat ->
                "${beat.arc}_COMPLETE" !in c.flags &&
                    active.none { it.id == beat.arc } &&
                    canStart(c, beat)
            }
        } else emptyList()

        val candidates = buildList {
            addAll(readyContinuations)
            addAll(startCandidates)
        }

        if (candidates.isNotEmpty()) {
            val chosen = candidates.maxByOrNull { beatScore(c, it, active) }!!
            return majorNode(c, chosen)
        }

        if (startCandidates.isNotEmpty()) return majorNode(c, startCandidates.first())

        active.minByOrNull { it.lastTurn }?.let { thread ->
            beatsByArc[thread.id]?.firstOrNull { it.stage == thread.stage + 1 }?.let {
                return majorNode(c, it)
            }
        }

        val fallback = arcStart.values.filter { canStartIgnoringCompletion(c, it) }
            .maxByOrNull { beatScore(c, it, active) }
            ?: arcStart.values.first()
        return majorNode(c, fallback)
    }

    private fun canStart(c: Campaign, b: MajorBeat): Boolean =
        c.powerRevealed &&
            c.age in b.minAge..b.maxAge &&
            c.scope.ordinal >= b.minScope.ordinal &&
            b.requiresFlags.all { it in c.flags }

    private fun canStartIgnoringCompletion(c: Campaign, b: MajorBeat): Boolean =
        c.powerRevealed && c.age >= b.minAge && b.requiresFlags.all { it in c.flags }

    private fun continuationGap(c: Campaign, t: StoryThread): Int {
        if (t.stage >= 5) return 1
        return 2 + positiveMod(mix(c.seed, t.id.hashCode().toLong() + t.stage * 97L), 6)
    }

    private fun beatScore(c: Campaign, b: MajorBeat, active: List<StoryThread>): Double {
        val isContinuation = active.any { it.id == b.arc }
        val continuationBoost = if (isContinuation) 50.0 + b.stage * 6 else 0.0
        val freshBoost = if (b.stage == 1) 16.0 else 0.0
        val tagPenalty = if (b.tags.any { familyLabel(it) == c.lastCategory }) -8.0 else 0.0
        val ageScore = -abs(c.age - b.minAge).coerceAtMost(20) * .35
        val jitter = positiveMod(mix(c.seed, c.turn * 991L + b.id.hashCode()), 10_000) / 10_000.0
        return continuationBoost + freshBoost + tagPenalty + ageScore + jitter
    }

    private fun formativeNode(ch: FormativeChapter) = EventNode(
        ch.id, ch.title, ch.text, ch.choices, "VIE", "ANNÉE FORMATIVE", 1,
        "PROLOGUE_NOBODY_DECADE", ch.age - 7, "FORMATIVE"
    )

    private fun awakeningNode(c: Campaign): EventNode {
        val resolved = if (c.powerResolved) c else PowerResolver.resolve(c)
        val text = awakening.text.replace("{power_reveal_text}", resolved.powerRevealText)
        return EventNode(
            awakening.id, awakening.title, text,
            awakening.choices.map { it.copy(threadId = "AWAKENING") },
            "ÉVEIL", "PREMIÈRE MANIFESTATION", 3, "AWAKENING", 1, "AWAKENING"
        )
    }

    private fun foundationNode(c: Campaign, f: FoundationScene): EventNode {
        val text = contextualize(f.text, c)
        return EventNode(
            f.id, f.title, text,
            f.choices.map { it.copy(threadId = "FOUNDATION") },
            "FONDATION", "PREMIERS PAS", 1, "FOUNDATION", c.turn - 10, "FOUNDATION"
        )
    }

    private fun majorNode(c: Campaign, b: MajorBeat): EventNode {
        val category = b.tags.firstOrNull()?.let(::familyLabel) ?: "DESTIN"
        val stakes = when (b.stage) { 1 -> 1; 2, 3 -> 2; else -> 3 }
        val callback = formativeEcho(c, b)
        val text = contextualize(b.text, c) + callback
        return EventNode(
            b.id, b.title, text,
            b.choices.map {
                it.copy(
                    stakes = stakes,
                    sourceCategory = category,
                    threadId = b.arc
                )
            },
            category, if (b.stage >= 4) "CLIMAX" else "ARC", stakes, b.arc, b.stage, "MAJOR"
        )
    }

    private fun endingNode(c: Campaign, thread: StoryThread): EventNode {
        val route = endingRoute(c, thread)
        val text = endingText(thread.id, route)
        val choice = Choice(
            label = "Continuer la destinée",
            approach = "CONTINUE",
            sourceCategory = "ÉPILOGUE",
            threadId = thread.id,
            flag = "${thread.id}_COMPLETE+${thread.id}_ENDING_$route"
        )
        return EventNode(
            "${thread.id}_EP_$route", "Conclusion", text, listOf(choice),
            "ÉPILOGUE", route, 1, thread.id, 6, "ENDING"
        )
    }

    private fun endingRoute(c: Campaign, thread: StoryThread): String {
        val routes = listOf("CARE", "ORDER", "TRUTH", "ASCEND")
        if (thread.lastApproach in routes) return thread.lastApproach
        val maxScore = routes.maxOf { thread.score(it) }
        val tied = routes.filter { thread.score(it) == maxScore }
        return tied[positiveMod(mix(c.seed, thread.id.hashCode().toLong()), tied.size)]
    }

    private fun contextualize(text: String, c: Campaign): String = text
        .replace("{power_reveal_text}", c.powerRevealText)
        .replace("{power_cost_text}", c.powerCostText)
        .replace("{power_signature}", c.powerSignature)
        .replace("{alias}", c.alias.ifBlank { c.name })
        .replace("{city}", c.city)

    private fun formativeEcho(c: Campaign, b: MajorBeat): String {
        if (b.callbacks.isEmpty() || c.flags.none { it.startsWith("PF") }) return ""
        val tokenRegex = Regex("PF\\d{2}_[A-Z0-9_]+")
        for (callback in b.callbacks) {
            val tokens = tokenRegex.findAll(callback).map { it.value }.toList()
            val matched = tokens.firstOrNull { it in c.flags }
            if (matched != null) {
                val year = matched.substring(2, 4).toIntOrNull() ?: 1
                val age = 7 + year
                return "\n\nÀ $age ans, bien avant que ton pouvoir ne se révèle, tu avais déjà pris une décision qui revient aujourd'hui dans cette histoire."
            }
        }
        if (b.stage == 1 && positiveMod(mix(c.seed, b.id.hashCode().toLong()), 3) == 0) {
            return "\n\nCette situation réveille quelque chose de tes dix années d'avant le masque : tu n'avais aucun pouvoir alors, mais tu faisais déjà des choix."
        }
        return ""
    }

    private fun dummyCampaign() = Campaign(
        seed = 1L, name = "Debug", modifier = "Debug",
        flags = setOf("POWER_REVEALED"), powerFamily = "Énergie",
        powerRevealText = "Une manifestation apparaît.", powerCostText = "un coût existe."
    )
}

internal fun familyLabel(family: String): String = when (family.uppercase()) {
    "POLITICS" -> "POLITIQUE"; "MEDIA" -> "MÉDIAS"; "GOVERNMENT" -> "GOUVERNEMENT"
    "FAMILY" -> "FAMILLE"; "IDENTITY" -> "IDENTITÉ"; "POWER" -> "POUVOIR"
    "COSMIC" -> "COSMIQUE"; "CITY" -> "VILLE"; "COUNTRY" -> "PAYS"; "CIVILIAN" -> "CIVIL"
    "HEALTH" -> "SANTÉ"; "CRISIS" -> "CRISE"; "YOUTH" -> "JEUNESSE"; "MYSTIC" -> "MYSTIQUE"
    "MANHUNT" -> "TRAQUE"; "LEGACY" -> "HÉRITAGE"; "MENTOR" -> "MENTOR"; "RIVAL" -> "RIVAL"
    else -> family.uppercase()
}

internal fun scopeOf(raw: String): Scope = try { Scope.valueOf(raw.uppercase()) } catch (_: Throwable) { Scope.STREET }
