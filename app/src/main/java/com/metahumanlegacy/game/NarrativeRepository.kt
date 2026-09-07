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

    private fun formativeNode(ch: FormativeChapter): EventNode {
        val canon = childhoodCanon(ch.age)
        val choices = ch.choices.mapIndexed { index, choice ->
            choice.copy(label = canon.labels.getOrElse(index) { choice.label })
        }
        return EventNode(
            ch.id, canon.title, canon.text, choices, "VIE", "ANNÉE FORMATIVE", 1,
            "PROLOGUE_NOBODY_DECADE", ch.age - 7, "FORMATIVE"
        )
    }

    private fun awakeningNode(c: Campaign): EventNode {
        val resolved = if (c.powerResolved) c else PowerResolver.resolve(c)
        val labels = mapOf(
            "CARE" to "Protéger quelqu’un avant de chercher une explication",
            "ORDER" to "Cacher ce qui vient d’arriver et reprendre le contrôle",
            "TRUTH" to "Observer la manifestation et comprendre ce qui l’a déclenchée",
            "ASCEND" to "Tester jusqu’où ce nouveau pouvoir peut aller"
        )
        val text = buildString {
            append("18 ans. Quelque chose que tes dix années précédentes ont préparé sans que tu le saches franchit enfin la limite. ")
            append(resolved.powerRevealText)
            append("\n\nCe n’est pas un pouvoir choisi dans un menu : ta famille, tes peurs, tes habitudes, tes prises de risque et la manière dont tu as appris à agir ont pesé sur cette manifestation.")
            append("\n\nTu sens déjà son coût : ")
            append(resolved.powerCostText)
            append(".")
        }
        return EventNode(
            awakening.id, "LA PREMIÈRE MANIFESTATION", text,
            awakening.choices.map { choice ->
                choice.copy(
                    label = labels[choice.approach] ?: choice.label,
                    threadId = "AWAKENING"
                )
            },
            "ÉVEIL", "PREMIÈRE MANIFESTATION", 3, "AWAKENING", 1, "AWAKENING"
        )
    }

    private data class ChildhoodCanon(
        val title: String,
        val text: String,
        val labels: List<String>
    )

    private fun childhoodCanon(age: Int): ChildhoodCanon = when (age) {
        8 -> ChildhoodCanon(
            "LE SAC DANS LA COUR",
            "À 8 ans, un camarade oublie son sac dans la cour. À l’intérieur, tu aperçois quelque chose que plusieurs enfants convoitent déjà. Un adulte n’est pas loin, mais personne ne t’a vu le ramasser. Ce n’est pas une question de héros ou de vilain : c’est la première fois que tu décides ce que vaut une règle quand personne ne regarde.",
            listOf(
                "Le rendre discrètement à son propriétaire",
                "Le confier à un adulte sans dire qui l’a trouvé",
                "Chercher d’abord à comprendre pourquoi tout le monde le veut",
                "Le garder un moment pour voir ce que ça peut t’apporter"
            )
        )
        9 -> ChildhoodCanon(
            "UNE PLACE À TABLE",
            "À 9 ans, l’ambiance à la maison est tendue. Une dispute d’adultes déborde sur le repas et quelqu’un que tu aimes s’isole. Tu ne peux pas réparer leur vie, mais tu peux choisir comment te comporter au milieu de ce malaise.",
            listOf(
                "Rester près de la personne qui s’est isolée",
                "Essayer de calmer la discussion avec des règles simples",
                "Écouter et retenir ce que chacun reproche vraiment à l’autre",
                "Profiter du chaos pour obtenir quelque chose qu’on t’aurait refusé"
            )
        )
        10 -> ChildhoodCanon(
            "LE DÉFI DU TOIT",
            "À 10 ans, des enfants plus âgés lancent un défi idiot : grimper sur une structure interdite derrière l’école. Refuser te fera passer pour quelqu’un de peureux ; accepter peut réellement mal finir. Le groupe attend ta réaction.",
            listOf(
                "Convaincre les autres de choisir un défi moins dangereux",
                "Refuser clairement et assumer de perdre la face",
                "Observer le trajet et chercher une manière sûre de redescendre ceux qui montent",
                "Monter le premier pour prouver que tu n’as peur de personne"
            )
        )
        11 -> ChildhoodCanon(
            "CE QUE TU AS VU",
            "À 11 ans, tu vois un élève apprécié accuser quelqu’un d’autre pour une bêtise qu’il a commise. La personne accusée risque une sanction. Dire la vérité peut te mettre tout le groupe à dos ; te taire évite les problèmes immédiats.",
            listOf(
                "Soutenir la personne accusée, même si tu deviens une cible",
                "Demander à parler seul avec un adulte et raconter les faits",
                "Confronter d’abord le vrai responsable pour lui laisser une chance d’avouer",
                "Te taire et utiliser plus tard ce que tu sais comme levier"
            )
        )
        12 -> ChildhoodCanon(
            "LA PORTE FERMÉE",
            "À 12 ans, un proche commence à cacher un problème qui l’affecte vraiment. Tu comprends qu’il ou elle ne veut pas en parler. Insister peut briser la confiance ; ignorer les signes peut laisser la situation empirer.",
            listOf(
                "Rester disponible sans forcer la confidence",
                "Prévenir un adulte fiable malgré le risque de vexer ton proche",
                "Chercher des indices pour comprendre avant d’agir",
                "Garder le secret parce que cette confiance peut devenir importante pour toi"
            )
        )
        13 -> ChildhoodCanon(
            "LE GROUPE",
            "À 13 ans, ton cercle d’amis change. Pour rester accepté, on te demande de participer à une humiliation publique contre quelqu’un de votre âge. Personne ne parle de violence ; justement, tout le monde prétend que ce n’est qu’une blague.",
            listOf(
                "Refuser et aller parler à la personne visée",
                "Couper court au plan en imposant une limite au groupe",
                "Comprendre qui pousse vraiment les autres et pourquoi",
                "Participer juste assez pour conserver ta place dans le groupe"
            )
        )
        14 -> ChildhoodCanon(
            "LE MESSAGE QUI TOURNE",
            "À 14 ans, une capture d’écran privée circule dans ton établissement. Tu peux la transférer, la supprimer, défendre la personne concernée ou essayer d’identifier l’origine de la fuite. Chaque option a un coût social.",
            listOf(
                "Prévenir la personne concernée et ne rien transférer",
                "Signaler la diffusion à un adulte et demander qu’elle soit stoppée",
                "Remonter la chaîne des partages pour trouver la source",
                "La conserver et l’utiliser pour gagner de l’influence dans le groupe"
            )
        )
        15 -> ChildhoodCanon(
            "APRÈS LES COURS",
            "À 15 ans, tu dois choisir entre aider régulièrement chez toi, t’investir dans une activité qui peut ouvrir des portes, ou rester disponible pour tes amis. Aucun choix n’est mauvais en soi, mais tu ne peux pas tout faire sans t’épuiser.",
            listOf(
                "Donner la priorité aux proches qui comptent sur toi",
                "Construire un emploi du temps strict pour tenir plusieurs engagements",
                "Choisir l’activité qui t’apprend le plus, même si certains te le reprochent",
                "Prendre l’option qui te donne le plus d’indépendance et de statut"
            )
        )
        16 -> ChildhoodCanon(
            "LA NUIT DU QUARTIER",
            "À 16 ans, une panne plonge plusieurs rues dans le noir pendant qu’un incident provoque un mouvement de panique. Tu n’as aucun pouvoir. Tu as seulement ton téléphone, tes jambes, ce que tu sais faire et les gens autour de toi.",
            listOf(
                "Aider les personnes les plus vulnérables à se mettre à l’abri",
                "Organiser les présents et répartir les tâches",
                "Chercher la cause de l’incident avant de suivre la foule",
                "Prendre des risques pour atteindre la zone que tout le monde évite"
            )
        )
        else -> ChildhoodCanon(
            "CE QUE TU VEUX DEVENIR",
            "À 17 ans, l’année suivante approche avec ses choix d’études, de travail, de départ ou de responsabilités. Une occasion inattendue te force à décider ce que tu privilégies vraiment : les autres, la stabilité, la compréhension ou ta propre ascension. Tu ignores encore qu’un autre changement t’attend à 18 ans.",
            listOf(
                "Choisir une voie qui te permet de rester utile aux personnes autour de toi",
                "Choisir la voie la plus stable et construire des bases solides",
                "Choisir ce qui te permettra de comprendre davantage le monde",
                "Choisir l’option la plus ambitieuse, même si elle te sépare des autres"
            )
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
