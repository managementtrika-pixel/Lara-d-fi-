package com.metahumanlegacy.game

import kotlin.math.abs
import kotlin.math.max

/**
 * A persistence-free-on-purpose depth layer.
 *
 * The source of truth remains Campaign V4. Rich life-state is encoded in namespaced flags so
 * existing saves keep working without a schema migration. No formative affinity/expression/cost
 * value is ever changed here: the first decade still determines the power only through GameRules.
 */
internal object DepthDirector {
    private const val PREFIX = "deep:"

    internal data class Aftermath(
        val campaign: Campaign,
        val echo: String = ""
    )

    internal data class WitnessContext(
        val reliability: Int,
        val crowd: Int,
        val cameras: Int,
        val officialPresence: Int,
        val label: String
    )

    internal data class Dossier(
        val code: String,
        val legal: String,
        val media: String,
        val base: String,
        val strain: Int,
        val collateral: Int,
        val districtSentiment: Int,
        val familyTrust: Int,
        val familyAffection: Int,
        val familyFear: Int,
        val rivalGrudge: Int,
        val rivalAdmiration: Int,
        val mediaSuspicion: Int,
        val governmentSuspicion: Int,
        val familySuspicion: Int,
        val rivalSuspicion: Int,
        val techniques: List<String>,
        val injuries: List<String>,
        val promises: Int,
        val activePressures: List<String>,
        val mystery: String,
        val mysteryProgress: Int
    )

    fun enrichEvent(c: Campaign, base: EventNode): EventNode {
        if (base.kind == "FORMATIVE" || base.kind == "AWAKENING" || !c.powerRevealed) return base

        val witness = witness(c, base)
        val dossier = dossier(c)
        val callbacks = mutableListOf<String>()

        dueDeferred(c).firstOrNull()?.let { deferred ->
            callbacks += when (deferred.kind) {
                "PERSON" -> "Une ancienne décision revient dans le dossier : quelqu'un que tu croyais sorti de l'histoire est de nouveau concerné."
                "MEDIA" -> "Une vieille séquence ressort au moment le moins pratique. Le contexte d'autrefois ne survit pas intact à sa rediffusion."
                "RIVAL" -> "Une conséquence laissée en suspens a donné du temps à ton rival pour préparer sa réponse."
                "FAMILY" -> "Ce que tu avais remis à plus tard dans ta vie privée finit par réclamer une réponse."
                else -> "Une conséquence ancienne refait surface. Le monde n'avait pas oublié ce que toi, peut-être, tu avais rangé ailleurs."
            }
        }

        oldestMemory(c)?.let { memory ->
            if (c.turn - memory.turn >= 6 && positiveMod(mix(c.seed, c.turn * 73L + base.id.hashCode()), 100) < 34) {
                callbacks += "Écho de ta carrière : un choix fait à ${memory.age} ans colore encore la façon dont cette situation est comprise."
            }
        }

        val pressure = dossier.activePressures.take(2)
        if (pressure.isNotEmpty()) callbacks += "Pendant ce temps : ${pressure.joinToString(" · ")}"

        val imperfect = when {
            witness.reliability < 45 -> "Les informations sont fragmentaires et plusieurs témoignages se contredisent."
            witness.reliability < 70 -> "Les faits disponibles semblent crédibles, mais une partie de la scène reste mal documentée."
            else -> "Les faits principaux sont relativement bien établis, sans garantir que tout le monde les interprète de la même façon."
        }

        val witnessLine = when {
            witness.cameras >= 75 -> "La scène est très exposée : téléphones, caméras et témoins rendent chaque geste difficile à contrôler après coup."
            witness.officialPresence >= 70 -> "Des services officiels sont déjà présents. Ton intervention sera aussi évaluée comme un acte public."
            witness.crowd >= 65 -> "Beaucoup de témoins sont présents. Même une action discrète peut devenir une histoire collective."
            else -> "La scène reste relativement contenue ; ce qui s'y passe pourrait ne jamais devenir une version publique unique."
        }

        val enrichedText = buildString {
            append(base.text)
            append("\n\n")
            append(imperfect)
            append(' ')
            append(witnessLine)
            if (callbacks.isNotEmpty()) {
                append("\n\n")
                append(callbacks.joinToString("\n"))
            }
        }

        val tradeoffs = base.choices.map { choice -> addHighStakeTradeoff(base, choice) }
        val dynamic = dynamicChoices(c, base, dossier)
        val choices = (tradeoffs + dynamic).take(6)

        return base.copy(text = enrichedText, choices = choices)
    }

    fun afterChoice(before: Campaign, rawAfter: Campaign, event: EventNode, choice: Choice): Aftermath {
        // Never touch the hidden formative vectors. We can remember a formative life decision, but
        // its power math remains exclusively GameRules + PowerResolver.
        if (event.kind == "FORMATIVE") {
            val flags = rawAfter.flags.toMutableSet()
            remember(flags, before, event, choice)
            updateRelation(flags, "family", trust = choice.relationDelta, affection = choice.relationDelta, fear = 0, admiration = 0, grudge = if (choice.relationDelta < 0) -choice.relationDelta else 0, dependence = 0)
            return Aftermath(rawAfter.copy(flags = flags), "")
        }

        val flags = rawAfter.flags.toMutableSet()
        val echo = mutableListOf<String>()
        val healthLoss = (before.health - rawAfter.health).coerceAtLeast(0)
        val casualtyDelta = (rawAfter.civilianCasualties - before.civilianCasualties).coerceAtLeast(0)

        remember(flags, before, event, choice)
        updatePersonalCode(flags, choice)
        updateRecent(flags, event.category)
        updateRelations(flags, before, rawAfter, event, choice)
        updatePromises(flags, before, event, choice, echo)
        updateDeferred(flags, before, event, choice)
        resolveDueDeferred(flags, before.turn, echo)
        updateNpcAutonomy(flags, before, event, echo)
        updateNemesis(flags, before, event, choice, casualtyDelta, echo)
        updateClocks(flags, before, event, choice, echo)
        updateIdentityKnowledge(flags, before, event, choice, echo)
        updateMediaAndLaw(flags, rawAfter, echo)
        updateCollateral(flags, before, rawAfter, event, choice, echo)
        updateInjuries(flags, before, rawAfter, event, healthLoss, echo)
        updatePowerEvolution(flags, before, rawAfter, event, choice, echo)
        updateBase(flags, rawAfter, echo)
        updateDistrict(flags, rawAfter, event, choice, echo)
        updateMystery(flags, before, event, choice, echo)
        interestingFailure(before, rawAfter, event, choice)?.let(echo::add)

        var after = rawAfter.copy(flags = flags)

        // Deep state can create restrained systemic consequences, but never rewrites the authored
        // outcome or overrides the logical combat result.
        val strain = int(flags, "strain", 0)
        if (strain >= 88 && after.powerRevealed) {
            after = after.copy(
                health = (after.health - 2).coerceIn(0, 100),
                control = (after.control - 2).coerceIn(0, 100)
            )
            echo += "Ton coût de pouvoir n'est plus abstrait : la surcharge te suit jusque dans l'après-coup."
        } else if (strain >= 68 && after.powerRevealed && positiveMod(mix(after.seed, after.turn * 919L), 100) < 45) {
            after = after.copy(control = (after.control - 1).coerceIn(0, 100))
            echo += "La fatigue de ton pouvoir rend la maîtrise moins propre qu'elle ne l'était au début de la carrière."
        }

        val timelineEchoes = echo.distinct().take(3)
        if (timelineEchoes.isNotEmpty()) {
            after = after.copy(
                timeline = (after.timeline + timelineEchoes.map { "↳ $it" }).takeLast(180)
            )
        }
        return Aftermath(after, timelineEchoes.joinToString("\n\n"))
    }

    fun afterAnnualAction(c: Campaign, state: AnnualActionState, card: AnnualActionCard): Campaign {
        val flags = c.flags.toMutableSet()
        setInt(flags, "skill_rescue", state.rescue)
        setInt(flags, "skill_investigation", state.investigation)
        setInt(flags, "skill_presence", state.presence)
        setInt(flags, "skill_discipline", state.discipline)
        addListFlag(flags, "interlude", "${c.turn},${safe(card.id)}", 18)

        when (card.category) {
            AnnualActionCategory.RELATION -> updateRelation(flags, "family", trust = 2, affection = 3, fear = -1, admiration = 0, grudge = -1, dependence = 1)
            AnnualActionCategory.INVESTIGATION -> {
                setInt(flags, "mystery_progress", (int(flags, "mystery_progress", 0) + max(1, card.investigation / 2)).coerceIn(0, 12))
                setInt(flags, "clock_faction", (int(flags, "clock_faction", 0) - 1).coerceAtLeast(0))
            }
            AnnualActionCategory.INTERVENTION -> {
                updateRelation(flags, "media", trust = 1, affection = 0, fear = 0, admiration = 2, grudge = 0, dependence = 0)
                setInt(flags, "district_sentiment", (int(flags, "district_sentiment", 0) + 2).coerceIn(-100, 100))
            }
            AnnualActionCategory.TRAINING -> if (c.powerRevealed) {
                val strain = (int(flags, "strain", 0) + if (card.power > 0) 3 else -2).coerceIn(0, 100)
                setInt(flags, "strain", strain)
                setInt(flags, "use_order", int(flags, "use_order", 0) + max(1, card.discipline / 2))
            }
            AnnualActionCategory.RECOVERY -> setInt(flags, "strain", (int(flags, "strain", 0) - 8).coerceAtLeast(0))
            AnnualActionCategory.PUBLIC -> updateRelation(flags, "media", trust = if (card.opinion >= 0) 2 else -1, affection = 1, fear = 0, admiration = 1, grudge = 0, dependence = 0)
            AnnualActionCategory.CIVIL -> updateRelation(flags, "family", trust = 1, affection = 1, fear = 0, admiration = 0, grudge = 0, dependence = 0)
        }

        if (card.id == "civil_barber") replace(flags, "look", "CIVIL_REFRESHED_${c.turn}")
        if (card.id == "meta_media_silence") {
            setInt(flags, "sus_media", (int(flags, "sus_media", 0) - 5).coerceAtLeast(0))
            setInt(flags, "clock_identity", (int(flags, "clock_identity", 0) - 1).coerceAtLeast(0))
        }
        if (card.id == "meta_watch_faction" || card.id == "meta_tip") {
            setInt(flags, "clock_faction", (int(flags, "clock_faction", 0) - 2).coerceAtLeast(0))
        }
        if (card.id == "meta_family" || card.id == "civil_family") {
            openPromises(flags).firstOrNull()?.let { old ->
                flags.remove(old.raw)
                addListFlag(flags, "promise_kept", "${c.turn},PRESENCE", 10)
            }
        }
        return c.copy(flags = flags)
    }

    fun dossier(c: Campaign): Dossier {
        val flags = c.flags
        val mysteryId = string(flags, "mystery", seededMystery(c.seed))
        return Dossier(
            code = codeLabel(flags),
            legal = string(flags, "legal", legalStatus(c)),
            media = string(flags, "media_frame", mediaFrame(c)),
            base = string(flags, "base", baseStage(c)),
            strain = int(flags, "strain", 0),
            collateral = int(flags, "collateral", c.civilianCasualties * 4),
            districtSentiment = int(flags, "district_sentiment", c.opinion / 2),
            familyTrust = rel(flags, "family", "trust", c.familyBond),
            familyAffection = rel(flags, "family", "affection", c.familyBond),
            familyFear = rel(flags, "family", "fear", 0),
            rivalGrudge = rel(flags, "rival", "grudge", if (c.rivalStanding < 0) -c.rivalStanding else 0),
            rivalAdmiration = rel(flags, "rival", "admiration", c.rivalStanding.coerceAtLeast(0)),
            mediaSuspicion = int(flags, "sus_media", c.identityExposure / 2),
            governmentSuspicion = int(flags, "sus_government", c.identityExposure / 3),
            familySuspicion = int(flags, "sus_family", if (c.powerRevealed) 40 else 0),
            rivalSuspicion = int(flags, "sus_rival", if (c.powerRevealed) 55 else 0),
            techniques = listValues(flags, "technique").map { humanTechnique(it.substringAfter(',')) }.distinct().take(5),
            injuries = listValues(flags, "injury").map(::humanInjury).distinct().takeLast(5),
            promises = openPromises(flags).size,
            activePressures = activePressures(flags),
            mystery = mysteryLabel(mysteryId),
            mysteryProgress = int(flags, "mystery_progress", 0)
        )
    }

    fun legacyTitle(c: Campaign, base: String): String {
        val d = dossier(c)
        val epithet = when {
            d.promises == 0 && countList(c.flags, "promise_kept") >= 3 -> "Parole tenue"
            d.collateral >= 45 -> "Victoire à un prix immense"
            d.familyTrust >= 80 && c.familyBond >= 70 -> "Une légende qui rentrait encore chez elle"
            d.rivalGrudge >= 75 -> "Le nom que ses ennemis n'ont jamais oublié"
            d.media == "ICÔNE POPULAIRE" -> "Icône d'une génération"
            d.media == "MENACE PUBLIQUE" -> "Le visage d'une époque de peur"
            d.code.contains("PROTÉGER") -> "Protecteur jusqu'au bout"
            d.code.contains("VÉRITÉ") -> "Celui qui refusait les versions faciles"
            d.code.contains("CONTRÔLE") -> "La force tenue en laisse"
            d.code.contains("ASCENSION") -> "Toujours plus haut"
            else -> "Une vie impossible à résumer"
        }
        return "$base · $epithet"
    }

    fun legacyScoreBonus(c: Campaign): Int {
        val d = dossier(c)
        return (d.techniques.size * 8 + countList(c.flags, "promise_kept") * 10 +
            (abs(d.districtSentiment) / 4) + (d.mysteryProgress * 2) - d.promises * 4 - d.collateral / 3)
    }

    fun legacySummary(c: Campaign): String {
        val d = dossier(c)
        val technique = d.techniques.firstOrNull()?.let { " Sa technique la plus reconnue fut « $it »." }.orEmpty()
        val injury = d.injuries.lastOrNull()?.let { " Son corps gardait encore la trace de $it." }.orEmpty()
        return "${d.code}. Statut final : ${d.legal.lowercase()}. Les médias l'ont surtout raconté comme ${d.media.lowercase()}. " +
            "Dans ${c.district}, sa mémoire locale finit à ${sentimentWord(d.districtSentiment)}.$technique$injury"
    }

    private fun dynamicChoices(c: Campaign, event: EventNode, d: Dossier): List<Choice> {
        val out = mutableListOf<Choice>()
        val rescue = int(c.flags, "skill_rescue", 0)
        val investigation = int(c.flags, "skill_investigation", 0)
        val presence = int(c.flags, "skill_presence", 0)
        val discipline = int(c.flags, "skill_discipline", 0)

        if (rescue >= 24 && event.category in setOf("CRISE", "SANTÉ", "CIVIL", "VILLE", "TRAUMA")) {
            out += Choice(
                label = "Appliquer une méthode de secours déjà éprouvée",
                moral = 1, opinion = 1, impact = 1, risk = max(1, event.stakes),
                approach = "CARE", stakes = event.stakes, sourceCategory = event.category,
                relationDelta = 1, healthDelta = if (rescue >= 55) 1 else 0,
                flag = "depth_skill_rescue"
            )
        }
        if (investigation >= 24 && event.category in setOf("RIVAL", "FACTION", "MÉDIAS", "GOUVERNEMENT", "POLITIQUE", "IDENTITÉ", "TRAQUE")) {
            out += Choice(
                label = "Recouper ce que les versions officielles ne font pas coïncider",
                opinion = 1, impact = 1, risk = max(1, event.stakes - 1),
                approach = "TRUTH", stakes = event.stakes, sourceCategory = event.category,
                identityDelta = if (investigation >= 60) -1 else 0,
                flag = "depth_skill_investigation"
            )
        }
        if (presence >= 28 && event.category in setOf("MÉDIAS", "FAMILLE", "CIVIL", "IDENTITÉ", "POLITIQUE", "GOUVERNEMENT")) {
            out += Choice(
                label = "Prendre la parole sans transformer la scène en spectacle",
                moral = 1, prestige = 1, opinion = 2, risk = max(1, event.stakes - 1),
                approach = "CARE", stakes = event.stakes, sourceCategory = event.category,
                relationDelta = 1, identityDelta = 1, flag = "depth_skill_presence"
            )
        }
        if (discipline >= 28 && event.category in setOf("POUVOIR", "CRISE", "RIVAL", "TRAQUE", "SANTÉ")) {
            out += Choice(
                label = "Rester dans un protocole de maîtrise travaillé hors caméra",
                prestige = 1, power = 1, impact = 1, risk = max(1, event.stakes - 1),
                approach = "ORDER", stakes = event.stakes, sourceCategory = event.category,
                healthDelta = if (discipline >= 60) 1 else 0,
                flag = "depth_skill_discipline"
            )
        }
        if ((event.category == "IDENTITÉ" || event.category == "MÉDIAS") && investigation + presence >= 65 && c.identityExposure >= 25) {
            out += Choice(
                label = "Construire un alibi crédible avant que la version publique ne se fige",
                prestige = -1, opinion = 0, risk = 2, approach = "TRUTH", stakes = event.stakes,
                sourceCategory = event.category, identityDelta = -5, flag = "depth_cover_story"
            )
        }
        if (d.promises > 0 && event.category in setOf("FAMILLE", "CIVIL", "RIVAL", "SANTÉ", "CRISE")) {
            out += Choice(
                label = "Tenir la promesse faite plus tôt, même si cela te coûte maintenant",
                moral = 2, prestige = -1, opinion = 0, risk = event.stakes + 1,
                approach = "CARE", stakes = event.stakes, sourceCategory = event.category,
                relationDelta = 4, healthDelta = if (event.stakes >= 3) -1 else 0,
                flag = "depth_keep_promise"
            )
        }
        d.techniques.firstOrNull()?.let { tech ->
            if (event.stakes >= 3 && c.powerRevealed) {
                out += Choice(
                    label = "Utiliser ta technique signature : $tech",
                    prestige = 1, power = 1, impact = 2, risk = max(2, event.stakes),
                    approach = dominantApproach(c.flags), stakes = event.stakes,
                    sourceCategory = event.category, flag = "depth_signature_technique"
                )
            }
        }
        if (event.stakes >= 3 && out.size < 2) {
            out += Choice(
                label = "Absorber personnellement le coût pour limiter les dégâts autour de toi",
                moral = 1, prestige = 1, opinion = 1, impact = 1, risk = event.stakes + 1,
                approach = "CARE", stakes = event.stakes, sourceCategory = event.category,
                healthDelta = -3, identityDelta = 1, flag = "depth_personal_cost"
            )
        }
        return out.distinctBy { it.label }
    }

    private fun addHighStakeTradeoff(event: EventNode, choice: Choice): Choice {
        if (event.kind != "MAJOR" || event.stakes < 3) return choice
        return when (choice.approach) {
            "CARE" -> choice.copy(identityDelta = choice.identityDelta + 1)
            "ORDER" -> choice.copy(opinion = choice.opinion - 1)
            "TRUTH" -> choice.copy(identityDelta = choice.identityDelta + 1, risk = choice.risk + 1)
            "ASCEND" -> choice.copy(fear = choice.fear + 1, risk = choice.risk + 1)
            else -> choice
        }
    }

    private fun remember(flags: MutableSet<String>, c: Campaign, event: EventNode, choice: Choice) {
        val importance = event.stakes + if (event.kind == "AWAKENING" || event.kind == "ENDING") 3 else 0
        if (importance >= 2 || event.kind == "FORMATIVE") {
            addListFlag(flags, "memory", "${c.turn},${c.age},${safe(event.id)},${safe(event.category)},${safe(choice.approach)}", 26)
        }
    }

    private data class Memory(val turn: Int, val age: Int, val event: String, val category: String, val approach: String)
    private fun oldestMemory(c: Campaign): Memory? = listValues(c.flags, "memory").mapNotNull { raw ->
        val p = raw.split(',')
        if (p.size < 5) null else Memory(p[0].toIntOrNull() ?: 0, p[1].toIntOrNull() ?: 18, p[2], p[3], p[4])
    }.filter { c.turn - it.turn >= 5 }.minByOrNull { it.turn }

    private fun updatePersonalCode(flags: MutableSet<String>, choice: Choice) {
        val key = when (choice.approach) {
            "CARE" -> "code_care"
            "ORDER" -> "code_order"
            "TRUTH" -> "code_truth"
            "ASCEND" -> "code_ascend"
            else -> "code_other"
        }
        setInt(flags, key, int(flags, key, 0) + 1)
    }

    private fun codeLabel(flags: Set<String>): String {
        val scores = listOf(
            "PROTÉGER AVANT DE GAGNER" to int(flags, "code_care", 0),
            "CONTRÔLER AVANT D'IMPROVISER" to int(flags, "code_order", 0),
            "CHERCHER LA VÉRITÉ AVANT LE CONFORT" to int(flags, "code_truth", 0),
            "POUSSER L'ASCENSION PLUS LOIN" to int(flags, "code_ascend", 0)
        )
        val best = scores.maxByOrNull { it.second }
        return if (best == null || best.second < 2) "CODE ENCORE INDÉFINI" else best.first
    }

    private fun dominantApproach(flags: Set<String>): String = listOf(
        "CARE" to int(flags, "code_care", 0),
        "ORDER" to int(flags, "code_order", 0),
        "TRUTH" to int(flags, "code_truth", 0),
        "ASCEND" to int(flags, "code_ascend", 0)
    ).maxByOrNull { it.second }?.first ?: "CARE"

    private fun updateRecent(flags: MutableSet<String>, category: String) {
        val old = string(flags, "recent", "").split(',').filter { it.isNotBlank() }
        val next = (old + safe(category)).takeLast(5)
        replace(flags, "recent", next.joinToString(","))
    }

    private fun updateRelations(flags: MutableSet<String>, before: Campaign, after: Campaign, event: EventNode, choice: Choice) {
        val rel = choice.relationDelta
        when (event.category) {
            "FAMILLE", "CIVIL", "JEUNESSE", "TRAUMA", "SANTÉ" -> updateRelation(
                flags, "family", trust = rel + if (choice.approach == "CARE") 2 else 0,
                affection = rel + if (choice.approach == "CARE") 1 else 0,
                fear = if (choice.approach == "ASCEND") 2 else -1,
                admiration = if (after.prestige > before.prestige) 1 else 0,
                grudge = if (rel < 0) -rel + 1 else -1,
                dependence = if (choice.approach == "CARE") 1 else 0
            )
            "RIVAL" -> updateRelation(
                flags, "rival", trust = if (choice.approach == "TRUTH") 1 else -1,
                affection = 0, fear = if (choice.approach == "ASCEND") 3 else 1,
                admiration = if (choice.stakes >= 2) 2 else 1,
                grudge = if (choice.approach in setOf("ASCEND", "ORDER")) 3 else 1,
                dependence = 0
            )
            "MÉDIAS", "IDENTITÉ" -> updateRelation(
                flags, "media", trust = if (choice.approach == "TRUTH") 3 else if (choice.approach == "ASCEND") -2 else 1,
                affection = if (choice.approach == "CARE") 2 else 0,
                fear = if (after.fear > before.fear) 2 else 0,
                admiration = if (after.prestige > before.prestige) 2 else 0,
                grudge = if (choice.approach == "ASCEND") 1 else 0,
                dependence = 0
            )
            "GOUVERNEMENT", "POLITIQUE", "PAYS", "PRISON", "TRAQUE" -> updateRelation(
                flags, "government", trust = if (choice.approach == "ORDER") 2 else if (choice.approach == "ASCEND") -2 else 0,
                affection = 0, fear = if (after.fear > before.fear) 1 else 0,
                admiration = if (choice.approach == "ORDER") 1 else 0,
                grudge = if (choice.approach == "ASCEND") 2 else 0,
                dependence = if (after.influence > 250) 1 else 0
            )
            "FACTION", "MENTOR", "SIDEKICK", "REFUGE" -> updateRelation(
                flags, "faction", trust = if (choice.approach in setOf("CARE", "ORDER")) 1 else 0,
                affection = if (choice.approach == "CARE") 1 else 0,
                fear = if (choice.approach == "ASCEND") 2 else 0,
                admiration = if (choice.stakes >= 2) 1 else 0,
                grudge = if (choice.approach == "ASCEND") 2 else 0,
                dependence = 1
            )
        }
    }

    private fun updateRelation(
        flags: MutableSet<String>, who: String,
        trust: Int, affection: Int, fear: Int, admiration: Int, grudge: Int, dependence: Int
    ) {
        val defaults = when (who) {
            "family" -> listOf(50, 55, 0, 10, 0, 20)
            "rival" -> listOf(0, 0, 20, 10, 25, 0)
            else -> listOf(0, 0, 10, 10, 0, 0)
        }
        fun bump(metric: String, delta: Int, default: Int) {
            if (delta == 0) return
            setInt(flags, "rel_${who}_$metric", (int(flags, "rel_${who}_$metric", default) + delta).coerceIn(0, 100))
        }
        bump("trust", trust, defaults[0])
        bump("affection", affection, defaults[1])
        bump("fear", fear, defaults[2])
        bump("admiration", admiration, defaults[3])
        bump("grudge", grudge, defaults[4])
        bump("dependence", dependence, defaults[5])
    }

    private fun rel(flags: Set<String>, who: String, metric: String, default: Int) = int(flags, "rel_${who}_$metric", default).coerceIn(0, 100)

    private data class Promise(val raw: String, val turn: Int, val kind: String)
    private fun openPromises(flags: Set<String>): List<Promise> = flags.filter { it.startsWith("${PREFIX}promise=") }.mapNotNull { raw ->
        val p = raw.substringAfter('=').split(',')
        if (p.size < 2) null else Promise(raw, p[0].toIntOrNull() ?: 0, p[1])
    }.sortedBy { it.turn }

    private fun updatePromises(flags: MutableSet<String>, before: Campaign, event: EventNode, choice: Choice, echo: MutableList<String>) {
        if (choice.flag?.contains("depth_keep_promise") == true) {
            openPromises(flags).firstOrNull()?.let { promise ->
                flags.remove(promise.raw)
                addListFlag(flags, "promise_kept", "${before.turn},${promise.kind}", 12)
                updateRelation(flags, "family", trust = 5, affection = 3, fear = -1, admiration = 2, grudge = -3, dependence = 0)
                echo += "Tu tiens une promesse ancienne. Le jeu ne la transforme pas en trophée : quelqu'un s'en souviendra simplement."
            }
            return
        }
        val label = choice.label.lowercase()
        val promiseLike = listOf("promet", "protéger", "garder le secret", "revenir", "ne jamais", "assumer").any(label::contains)
        if (promiseLike && openPromises(flags).size < 4) {
            val kind = when (event.category) {
                "FAMILLE", "CIVIL", "SANTÉ" -> "FAMILY"
                "RIVAL" -> "RIVAL"
                "IDENTITÉ", "MÉDIAS" -> "SECRET"
                else -> "DUTY"
            }
            addListFlag(flags, "promise", "${before.turn},$kind", 4)
            echo += "Cette phrase devient une promesse. Elle n'est pas oubliée au prochain écran."
        }
        val stale = openPromises(flags).filter { before.turn - it.turn >= 12 }
        if (stale.isNotEmpty() && choice.approach == "ASCEND") {
            val p = stale.first()
            flags.remove(p.raw)
            addListFlag(flags, "promise_broken", "${before.turn},${p.kind}", 12)
            updateRelation(flags, "family", trust = -5, affection = -2, fear = 1, admiration = -1, grudge = 5, dependence = 0)
            echo += "Une promesse laissée trop longtemps sans réponse finit par ressembler à une décision."
        }
    }

    private data class Deferred(val raw: String, val dueTurn: Int, val kind: String, val source: String)
    private fun dueDeferred(c: Campaign): List<Deferred> = deferred(c.flags).filter { it.dueTurn <= c.turn }
    private fun deferred(flags: Set<String>): List<Deferred> = flags.filter { it.startsWith("${PREFIX}defer=") }.mapNotNull { raw ->
        val p = raw.substringAfter('=').split(',')
        if (p.size < 3) null else Deferred(raw, p[0].toIntOrNull() ?: 0, p[1], p[2])
    }

    private fun updateDeferred(flags: MutableSet<String>, before: Campaign, event: EventNode, choice: Choice) {
        val important = event.stakes >= 2 || choice.deferredHook
        if (!important) return
        val roll = positiveMod(mix(before.seed, before.turn * 257L + event.id.hashCode() + choice.label.hashCode()), 100)
        if (roll >= 46) return
        val delay = 3 + positiveMod(mix(before.seed, before.turn * 911L + event.id.hashCode()), 8)
        val kind = when (event.category) {
            "MÉDIAS", "IDENTITÉ" -> "MEDIA"
            "RIVAL" -> "RIVAL"
            "FAMILLE", "CIVIL", "SANTÉ" -> "FAMILY"
            else -> "PERSON"
        }
        addListFlag(flags, "defer", "${before.turn + delay},$kind,${safe(event.id)}", 8)
    }

    private fun resolveDueDeferred(flags: MutableSet<String>, turn: Int, echo: MutableList<String>) {
        val due = deferred(flags).filter { it.dueTurn <= turn }
        due.take(2).forEach { item ->
            flags.remove(item.raw)
            addListFlag(flags, "deferred_resolved", "$turn,${item.kind},${item.source}", 14)
            echo += when (item.kind) {
                "MEDIA" -> "Une vieille image ressort et oblige le présent à répondre au passé."
                "RIVAL" -> "Ton rival transforme un ancien épisode en levier contre toi."
                "FAMILY" -> "Un proche remet sur la table quelque chose que ta carrière avait laissé en suspens."
                else -> "Une personne touchée autrefois par tes décisions revient dans ta trajectoire."
            }
        }
    }

    private fun updateNpcAutonomy(flags: MutableSet<String>, before: Campaign, event: EventNode, echo: MutableList<String>) {
        val roll = positiveMod(mix(before.seed, before.turn * 443L + event.category.hashCode()), 100)
        if (roll >= 28) return
        val pulse = when (positiveMod(mix(before.seed, before.turn * 197L), 6)) {
            0 -> "Un proche change quelque chose dans sa propre vie sans attendre ton avis."
            1 -> "Ton rival noue une alliance qui n'a rien à voir avec ta dernière action."
            2 -> "Une faction déplace ses priorités pendant que ton attention est ailleurs."
            3 -> "Une personne autrefois secondaire devient soudain plus importante dans ton environnement."
            4 -> "Quelqu'un qui t'admirait commence à prendre ses distances et à construire sa propre opinion."
            else -> "Un allié agit de son côté et crée un fait accompli que tu découvriras trop tard pour l'empêcher."
        }
        addListFlag(flags, "npc_pulse", "${before.turn},${positiveMod(mix(before.seed, before.turn * 311L), 6)}", 16)
        echo += pulse
    }

    private fun updateNemesis(flags: MutableSet<String>, before: Campaign, event: EventNode, choice: Choice, casualtyDelta: Int, echo: MutableList<String>) {
        if (!before.powerRevealed) return
        if (event.category == "RIVAL" || choice.stakes >= 3) {
            val key = "nemesis_${choice.approach.lowercase()}"
            val count = int(flags, key, 0) + 1
            setInt(flags, key, count)
            val adaptation = int(flags, "nemesis_adaptation", 0) + if (event.category == "RIVAL") 2 else 1
            setInt(flags, "nemesis_adaptation", adaptation.coerceIn(0, 100))
            if (count == 3) echo += "Ton adversaire a vu cette manière d'agir assez souvent pour commencer à l'anticiper."
            if (count == 6) echo += "Ce qui était ta solution favorite est désormais une information connue de ton ennemi."
        }
        if (string(flags, "nemesis_origin", "").isBlank()) {
            val origin = when {
                casualtyDelta > 0 -> "COLLATERAL"
                choice.approach == "ASCEND" && choice.risk >= 6 -> "HUMILIATION"
                event.category == "FACTION" && choice.approach == "TRUTH" -> "EXPOSURE"
                else -> ""
            }
            if (origin.isNotBlank() && positiveMod(mix(before.seed, before.turn * 607L), 100) < 48) {
                replace(flags, "nemesis_origin", origin)
                echo += "Quelqu'un qui n'était pas encore ton ennemi vient d'obtenir une raison de le devenir."
            }
        }
    }

    private fun updateClocks(flags: MutableSet<String>, before: Campaign, event: EventNode, choice: Choice, echo: MutableList<String>) {
        if (!before.powerRevealed) return
        val clockDefs = listOf(
            Triple("clock_rival", setOf("RIVAL", "TRAQUE"), 5),
            Triple("clock_faction", setOf("FACTION", "MENTOR", "REFUGE"), 6),
            Triple("clock_identity", setOf("IDENTITÉ", "MÉDIAS"), 6),
            Triple("clock_crisis", setOf("CRISE", "VILLE", "PAYS"), 7)
        )
        clockDefs.forEachIndexed { index, (key, addressed, limit) ->
            var value = int(flags, key, 0)
            if (event.category in addressed) value = (value - 2).coerceAtLeast(0)
            else if (positiveMod(mix(before.seed, before.turn * (337L + index * 97L)), 100) < 48) value++
            if (choice.approach == "ASCEND" && key == "clock_identity") value++
            if (value >= limit) {
                echo += when (key) {
                    "clock_rival" -> "La pression de ton rival atteint un point où l'ignorer n'est plus neutre."
                    "clock_faction" -> "Une faction profite de l'espace que tu lui as laissé et consolide sa position."
                    "clock_identity" -> "Les soupçons autour de ton identité commencent à se recouper entre personnes qui ne se parlaient pas encore."
                    else -> "Une crise laissée en arrière-plan finit par produire ses propres conséquences."
                }
                value = max(2, limit / 2)
                addListFlag(flags, "clock_burst", "${before.turn},$key", 12)
            }
            setInt(flags, key, value.coerceIn(0, limit))
        }
    }

    private fun activePressures(flags: Set<String>): List<String> {
        val out = mutableListOf<String>()
        fun add(key: String, threshold: Int, label: String) {
            val v = int(flags, key, 0)
            if (v >= threshold) out += "$label ($v)"
        }
        add("clock_rival", 3, "un rival prépare sa prochaine réponse")
        add("clock_faction", 4, "une faction gagne du terrain")
        add("clock_identity", 4, "les soupçons sur l'identité se recoupent")
        add("clock_crisis", 5, "une crise de fond se rapproche du point de rupture")
        return out
    }

    private fun updateIdentityKnowledge(flags: MutableSet<String>, before: Campaign, event: EventNode, choice: Choice, echo: MutableList<String>) {
        if (!before.powerRevealed) return
        val w = witness(before, event)
        val base = choice.identityDelta + if (event.category == "IDENTITÉ") 3 else 0 + if (w.cameras > 70) 2 else 0
        fun bump(key: String, delta: Int) {
            setInt(flags, key, (int(flags, key, 0) + delta).coerceIn(0, 100))
        }
        bump("sus_media", max(0, base + w.cameras / 35))
        bump("sus_government", max(0, base + w.officialPresence / 40))
        bump("sus_family", max(0, if (event.category in setOf("FAMILLE", "CIVIL")) 3 else base / 2))
        bump("sus_rival", max(0, if (event.category == "RIVAL") 5 else base / 2))
        if (choice.flag?.contains("depth_cover_story") == true) {
            bump("sus_media", -8)
            bump("sus_government", -4)
            echo += "Ton alibi ne rend pas le passé faux ; il empêche simplement plusieurs soupçons de s'emboîter tout de suite."
        }
        val firstKnower = listOf("sus_family" to "un proche", "sus_rival" to "ton rival", "sus_media" to "un journaliste", "sus_government" to "une cellule officielle")
            .firstOrNull { int(flags, it.first, 0) >= 82 && !flags.contains("${PREFIX}knows_${it.first.substringAfter("sus_")}=YES") }
        if (firstKnower != null) {
            replace(flags, "knows_${firstKnower.first.substringAfter("sus_")}", "YES")
            echo += "${firstKnower.second.replaceFirstChar { it.uppercase() }} n'est plus seulement dans le soupçon : cette personne ou institution a probablement compris qui tu es."
        }
    }

    private fun witness(c: Campaign, event: EventNode): WitnessContext {
        val salt = c.turn * 1009L + event.id.hashCode() * 17L
        val reliability = 30 + positiveMod(mix(c.seed, salt + 1), 71)
        val crowd = positiveMod(mix(c.seed, salt + 2), 101)
        val cameras = (positiveMod(mix(c.seed, salt + 3), 101) + if (c.modifier == "Médias omniprésents") 20 else 0).coerceIn(0, 100)
        val official = (positiveMod(mix(c.seed, salt + 4), 101) + if (event.category in setOf("GOUVERNEMENT", "POLITIQUE", "TRAQUE")) 25 else 0).coerceIn(0, 100)
        val label = when {
            cameras >= 75 -> "TRÈS EXPOSÉE"
            crowd >= 65 -> "TRÈS OBSERVÉE"
            official >= 65 -> "ENCADRÉE"
            else -> "INCERTAINE"
        }
        return WitnessContext(reliability, crowd, cameras, official, label)
    }

    private fun updateMediaAndLaw(flags: MutableSet<String>, c: Campaign, echo: MutableList<String>) {
        val oldMedia = string(flags, "media_frame", "")
        val newMedia = mediaFrame(c)
        replace(flags, "media_frame", newMedia)
        if (oldMedia.isNotBlank() && oldMedia != newMedia) echo += "Le récit médiatique change : on te décrit désormais surtout comme « ${newMedia.lowercase()} »."

        val oldLegal = string(flags, "legal", "")
        val newLegal = legalStatus(c)
        replace(flags, "legal", newLegal)
        if (oldLegal.isNotBlank() && oldLegal != newLegal) echo += "Ton statut de fait change : $newLegal. Les mêmes actions n'auront plus exactement le même sens institutionnel."
    }

    private fun mediaFrame(c: Campaign): String = when {
        !c.powerRevealed -> "INCONNU"
        c.fear >= 75 && c.opinion <= -25 -> "MENACE PUBLIQUE"
        c.opinion >= 65 && c.prestige >= 50 -> "ICÔNE POPULAIRE"
        c.governmentStanding >= 45 -> "PARTENAIRE DU SYSTÈME"
        c.identityExposure >= 75 -> "CÉLÉBRITÉ SOUS PRESSION"
        c.mediaStanding <= -35 -> "VIGILANTE INCONTRÔLABLE"
        c.opinion >= 20 -> "PROTECTEUR DISCUTÉ"
        c.fear >= 45 -> "FIGURE INQUIÉTANTE"
        else -> "PHÉNOMÈNE AMBIGU"
    }

    private fun legalStatus(c: Campaign): String = when {
        !c.powerRevealed -> "CIVIL"
        c.governmentStanding <= -65 || (c.fear >= 80 && c.opinion <= -45) -> "MENACE NATIONALE"
        c.governmentStanding <= -35 -> "RECHERCHÉ"
        c.governmentStanding >= 70 -> "AGENT OFFICIEL"
        c.governmentStanding >= 35 -> "COLLABORATEUR AUTORISÉ"
        c.prestige >= 35 && c.opinion >= 15 -> "VIGILANTE TOLÉRÉ"
        else -> "STATUT NON RÉSOLU"
    }

    private fun updateCollateral(flags: MutableSet<String>, before: Campaign, after: Campaign, event: EventNode, choice: Choice, echo: MutableList<String>) {
        val old = int(flags, "collateral", before.civilianCasualties * 4)
        val casualty = (after.civilianCasualties - before.civilianCasualties).coerceAtLeast(0)
        val structural = if (event.stakes >= 3 && choice.power > 0) max(0, choice.risk - before.control / 20) else 0
        val next = (old + casualty * 12 + structural + if (choice.approach == "ASCEND" && event.stakes >= 3) 2 else 0).coerceIn(0, 100)
        setInt(flags, "collateral", next)
        if (next / 20 > old / 20 && next >= 20) echo += "Ta victoire laisse assez de dégâts autour d'elle pour devenir une partie du récit, pas seulement du décor."
    }

    private fun updateInjuries(flags: MutableSet<String>, before: Campaign, after: Campaign, event: EventNode, loss: Int, echo: MutableList<String>) {
        if (loss < 3) return
        val kind = when {
            event.category == "POUVOIR" -> "surcharge nerveuse"
            event.category == "RIVAL" -> "épaule fragilisée"
            event.category == "CRISE" -> "cicatrice de l'intervention"
            event.category == "SANTÉ" -> "séquelle persistante"
            else -> "blessure de terrain"
        }
        val severity = when { loss >= 10 -> "sévère"; loss >= 6 -> "marquée"; else -> "légère" }
        addListFlag(flags, "injury", "${before.turn},${safe(kind)},$severity", 8)
        echo += "Cette blessure reçoit désormais un nom dans ton dossier : $kind ($severity). Elle ne disparaît pas parce que la barre de santé remonte."
        if (after.age >= 50) setInt(flags, "strain", (int(flags, "strain", 0) + 2).coerceIn(0, 100))
    }

    private fun updatePowerEvolution(flags: MutableSet<String>, before: Campaign, after: Campaign, event: EventNode, choice: Choice, echo: MutableList<String>) {
        if (!after.powerRevealed) return
        val useKey = "use_${choice.approach.lowercase()}"
        val uses = int(flags, useKey, 0) + if (event.category in setOf("POUVOIR", "RIVAL", "CRISE", "TRAQUE") || choice.power != 0) 1 else 0
        setInt(flags, useKey, uses)

        var strain = int(flags, "strain", 0)
        strain += when {
            event.category == "POUVOIR" && choice.risk >= 5 -> 6
            choice.approach == "ASCEND" && event.stakes >= 3 -> 5
            choice.power >= 3 -> 4
            event.stakes >= 3 -> 2
            else -> -1
        }
        if (choice.approach == "ORDER" && after.control >= 45) strain -= 2
        setInt(flags, "strain", strain.coerceIn(0, 100))

        if (uses in setOf(3, 6, 10)) {
            val tech = techniqueSlug(after.powerFamily, choice.approach, uses)
            if (listValues(flags, "technique").none { it.substringAfter(',') == tech }) {
                addListFlag(flags, "technique", "${before.turn},$tech", 5)
                echo += "À force d'utiliser ton pouvoir de cette manière, une technique reconnaissable émerge : « ${humanTechnique(tech)} »."
            }
        }
        if (int(flags, "strain", 0) >= 60 && before.weakness != "Inconnue") {
            addListFlag(flags, "weakness_flare", "${before.turn},${safe(before.weakness)}", 12)
        }
    }

    private fun techniqueSlug(family: String, approach: String, tier: Int): String {
        val f = safe(family).take(14)
        val a = safe(approach).take(8)
        val suffix = when (tier) { 3 -> "FORME"; 6 -> "SIGNATURE"; else -> "MAITRISE" }
        return "${f}_${a}_$suffix"
    }

    private fun humanTechnique(raw: String): String {
        val p = raw.split('_').filter { it.isNotBlank() }
        if (p.isEmpty()) return "Technique signature"
        val approach = when {
            "CARE" in p -> "Garde"
            "ORDER" in p -> "Verrou"
            "TRUTH" in p -> "Percée"
            "ASCEND" in p -> "Apogée"
            else -> "Signature"
        }
        val core = p.first().lowercase().replaceFirstChar { it.uppercase() }
        return "$approach $core"
    }

    private fun updateBase(flags: MutableSet<String>, c: Campaign, echo: MutableList<String>) {
        val old = string(flags, "base", "")
        val next = baseStage(c)
        replace(flags, "base", next)
        if (old.isNotBlank() && old != next) echo += "Ton lieu de vie change avec la carrière : ${baseHuman(next)}. Ce n'est pas un menu de construction ; c'est une conséquence de ce que ta vie exige."
    }

    private fun baseStage(c: Campaign): String = when {
        !c.powerRevealed -> "CIVIL_HOME"
        c.identityExposure >= 80 && c.fear >= 55 -> "HIDDEN_REFUGE"
        c.influence >= 650 -> "SANCTUARY"
        c.influence >= 280 || c.prestige >= 65 -> "HEADQUARTERS"
        c.influence >= 90 || c.prestige >= 30 -> "SAFEHOUSE"
        else -> "SECURED_HOME"
    }

    private fun baseHuman(raw: String) = when (raw) {
        "CIVIL_HOME" -> "un domicile encore ordinaire"
        "SECURED_HOME" -> "un logement discrètement sécurisé"
        "SAFEHOUSE" -> "un refuge fonctionnel"
        "HEADQUARTERS" -> "un véritable quartier général"
        "SANCTUARY" -> "un sanctuaire à l'échelle de ta légende"
        "HIDDEN_REFUGE" -> "un refuge conçu d'abord pour disparaître"
        else -> raw.lowercase()
    }

    private fun updateDistrict(flags: MutableSet<String>, c: Campaign, event: EventNode, choice: Choice, echo: MutableList<String>) {
        var value = int(flags, "district_sentiment", c.opinion / 2)
        value += when {
            event.category in setOf("VILLE", "CIVIL", "CRISE") && choice.approach == "CARE" -> 3
            event.category in setOf("VILLE", "CIVIL") && choice.approach == "ASCEND" -> -3
            choice.opinion > 0 -> 1
            choice.fear > 1 -> -1
            else -> 0
        }
        value = value.coerceIn(-100, 100)
        val oldBand = sentimentBand(int(flags, "district_sentiment", 0))
        val newBand = sentimentBand(value)
        setInt(flags, "district_sentiment", value)
        if (oldBand != newBand && abs(value) >= 30) echo += "Dans ${c.district}, ta réputation locale bascule : ${sentimentWord(value)}. La ville garde une mémoire géographique de tes actes."
        if (event.stakes >= 3 && event.category in setOf("VILLE", "CRISE", "RIVAL")) {
            addListFlag(flags, "landmark", "${c.turn},${safe(c.district)},${safe(event.id)}", 10)
        }
    }

    private fun sentimentBand(v: Int) = when { v >= 55 -> 2; v >= 20 -> 1; v <= -55 -> -2; v <= -20 -> -1; else -> 0 }
    private fun sentimentWord(v: Int) = when {
        v >= 60 -> "aimée et revendiquée"
        v >= 25 -> "plutôt protectrice"
        v > -25 -> "divisée"
        v > -60 -> "méfiante"
        else -> "profondément hostile"
    }

    private fun updateMystery(flags: MutableSet<String>, before: Campaign, event: EventNode, choice: Choice, echo: MutableList<String>) {
        if (!before.powerRevealed) return
        if (string(flags, "mystery", "").isBlank()) replace(flags, "mystery", seededMystery(before.seed))
        var progress = int(flags, "mystery_progress", 0)
        val investigation = int(flags, "skill_investigation", 0)
        val advances = choice.approach == "TRUTH" || event.category in setOf("FACTION", "RIVAL", "POUVOIR") || investigation >= 35
        if (advances && positiveMod(mix(before.seed, before.turn * 887L + event.id.hashCode()), 100) < 42) progress++
        if (investigation >= 60 && event.category in setOf("FACTION", "POUVOIR")) progress++
        progress = progress.coerceIn(0, 10)
        val old = int(flags, "mystery_progress", 0)
        setInt(flags, "mystery_progress", progress)
        if (progress > old && progress in setOf(2, 5, 8)) {
            echo += when (progress) {
                2 -> "Un détail sans explication rejoint un autre détail ancien. Pour l'instant, ce n'est encore qu'une coïncidence."
                5 -> "Le motif caché de ${mysteryLabel(string(flags, "mystery", seededMystery(before.seed))).lowercase()} devient trop cohérent pour être ignoré."
                else -> "Des éléments séparés par des années commencent à former une seule histoire. Tu n'avais simplement pas encore assez de pièces."
            }
        }
        if (progress == 10 && old < 10) {
            addListFlag(flags, "mystery_solved", "${before.turn},${string(flags, "mystery", seededMystery(before.seed))}", 4)
            echo += "Le mystère de ${mysteryLabel(string(flags, "mystery", seededMystery(before.seed))).lowercase()} trouve enfin une réponse. Certaines scènes anciennes changent de sens rétroactivement."
        }
    }

    private fun seededMystery(seed: Long) = when (positiveMod(mix(seed, 0x44AA55L), 5)) {
        0 -> "ECHO_ARCHIVE"
        1 -> "GLASS_SIGNAL"
        2 -> "HOLLOW_NETWORK"
        3 -> "NINTH_WITNESS"
        else -> "BLACK_MERIDIAN"
    }

    private fun mysteryLabel(id: String) = when (id) {
        "ECHO_ARCHIVE" -> "l'Archive Écho"
        "GLASS_SIGNAL" -> "le Signal de Verre"
        "HOLLOW_NETWORK" -> "le Réseau Creux"
        "NINTH_WITNESS" -> "le Neuvième Témoin"
        "BLACK_MERIDIAN" -> "le Méridien Noir"
        else -> "un motif non identifié"
    }

    private fun interestingFailure(before: Campaign, after: Campaign, event: EventNode, choice: Choice): String? {
        val healthLoss = before.health - after.health
        val highRisk = choice.risk >= 5 || event.stakes >= 3
        if (!highRisk) return null
        val roll = positiveMod(mix(before.seed, before.turn * 1237L + choice.label.hashCode()), 100)
        return when {
            healthLoss >= 4 -> "Tu obtiens quelque chose, mais l'échec partiel laisse un coût concret. Le jeu ne remet pas la scène à zéro."
            roll < 18 && choice.approach == "TRUTH" -> "Tu n'obtiens pas le contrôle de la situation, mais tu repars avec une information que tu n'aurais jamais trouvée dans une victoire propre."
            roll < 18 && choice.approach == "CARE" -> "L'objectif immédiat t'échappe en partie, mais une personne directement touchée n'oublie pas que tu l'as choisie avant le symbole."
            roll < 18 && choice.approach == "ORDER" -> "Le résultat est imparfait, mais le cadre que tu imposes empêche la situation de se désagréger complètement."
            roll < 18 && choice.approach == "ASCEND" -> "Tu prends l'avantage sans obtenir la victoire totale ; quelqu'un d'autre paiera peut-être plus tard l'espace que tu viens d'ouvrir."
            else -> null
        }
    }

    private fun humanInjury(raw: String): String {
        val p = raw.split(',')
        val name = p.getOrElse(1) { "blessure_de_terrain" }.replace('_', ' ')
        val severity = p.getOrElse(2) { "marquée" }
        return "$name ($severity)"
    }

    private fun addListFlag(flags: MutableSet<String>, key: String, value: String, max: Int) {
        val prefix = "$PREFIX$key="
        val entries = flags.filter { it.startsWith(prefix) }.sortedBy { it }
        if (entries.size >= max) entries.take(entries.size - max + 1).forEach(flags::remove)
        flags += "$prefix$value"
    }

    private fun countList(flags: Set<String>, key: String) = flags.count { it.startsWith("$PREFIX$key=") }
    private fun listValues(flags: Set<String>, key: String): List<String> = flags.filter { it.startsWith("$PREFIX$key=") }.map { it.substringAfter('=') }

    private fun int(flags: Set<String>, key: String, default: Int): Int = flags.firstOrNull { it.startsWith("$PREFIX$key=") }
        ?.substringAfter('=')?.toIntOrNull() ?: default

    private fun string(flags: Set<String>, key: String, default: String): String = flags.firstOrNull { it.startsWith("$PREFIX$key=") }
        ?.substringAfter('=') ?: default

    private fun setInt(flags: MutableSet<String>, key: String, value: Int) = replace(flags, key, value.toString())

    private fun replace(flags: MutableSet<String>, key: String, value: String) {
        flags.removeAll { it.startsWith("$PREFIX$key=") }
        flags += "$PREFIX$key=$value"
    }

    private fun safe(raw: String): String = raw.uppercase()
        .replace('É', 'E').replace('È', 'E').replace('Ê', 'E')
        .replace('À', 'A').replace('Â', 'A').replace('Ç', 'C')
        .replace('Ù', 'U').replace('Û', 'U').replace('Ô', 'O').replace('Î', 'I')
        .map { if (it.isLetterOrDigit() || it == '_' || it == '-') it else '_' }.joinToString("")
        .replace(Regex("_+"), "_").trim('_')
}
