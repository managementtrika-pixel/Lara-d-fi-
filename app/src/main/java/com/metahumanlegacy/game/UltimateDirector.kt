package com.metahumanlegacy.game

import java.util.Random
import kotlin.math.absoluteValue

internal data class UltimateResolution(
    val campaign: Campaign,
    val state: UltimateState,
    val outcome: String
)

/**
 * High-level wrapper around the existing deterministic story engine.
 * It keeps the authored annual choice as the only passage of time, while making the spaces between
 * those choices feel like an actual life: people age, cases progress, districts change, equipment
 * and costume history accumulate, media and law react, and power use creates a personal style.
 */
internal object UltimateGameEngine {
    fun event(c: Campaign, state: UltimateState, annual: AnnualActionState): EventNode =
        UltimateDirector.enrich(c, state, annual, GameEngine.event(c))

    fun resolve(c: Campaign, state: UltimateState, event: EventNode, choice: Choice): UltimateResolution {
        val base = GameEngine.resolve(c, event, choice)
        val after = UltimateDirector.afterChoice(c, base.campaign, state, event, choice)
        val text = buildString {
            append(base.outcome)
            if (after.echo.isNotBlank()) {
                append("\n\nMONDE VIVANT\n")
                append(after.echo)
            }
        }
        return UltimateResolution(after.campaign, after.state, text)
    }

    fun annualActions(c: Campaign, state: UltimateState, annual: AnnualActionState): List<AnnualActionCard> {
        val core = AnnualActionEngine.available(c, annual)
        val extra = UltimateDirector.extraAnnualActions(c, state, annual)
        return (core + extra).distinctBy { it.id }.take(18)
    }

    fun afterAnnualAction(c: Campaign, state: UltimateState, annual: AnnualActionState, card: AnnualActionCard): UltimateState =
        UltimateDirector.afterAnnualAction(c, state, annual, card)
}

internal object UltimateDirector {
    internal data class Aftermath(val campaign: Campaign, val state: UltimateState, val echo: String)

    private val crisisWords = listOf("CRISE", "RIVAL", "COMBAT", "MENACE", "CATASTROPHE", "ATTAQUE")
    private val investigationWords = listOf("ENQU", "MYST", "SECRET", "DOSSIER", "DISPAR", "CONSPIR")
    private val publicWords = listOf("MEDIA", "PUBLIC", "POLIT", "GOUVER", "JUSTICE", "LOI")
    private val relationWords = listOf("RELATION", "FAMIL", "PROCHE", "AMI", "MENTOR")

    fun enrich(c: Campaign, state: UltimateState, annual: AnnualActionState, base: EventNode): EventNode {
        if (base.kind == "FORMATIVE" || base.kind == "AWAKENING" || !c.powerRevealed) return base
        val notes = mutableListOf<String>()
        val extra = mutableListOf<Choice>()
        val category = base.category.uppercase()
        val district = state.district(c.district) ?: state.districts.firstOrNull()

        if (annual.investigation >= 35 && investigationWords.any(category::contains) && base.choices.size + extra.size < 7) {
            extra += Choice(
                label = "Recouper les indices avant de choisir une cible",
                opinion = 1, impact = 1, risk = (base.stakes - 1).coerceAtLeast(1),
                approach = "TRUTH", stakes = base.stakes, sourceCategory = base.category,
                identityDelta = -1, flag = "ultimate_skill_investigation"
            )
            notes += "Ton expérience d'enquête te permet de distinguer une piste crédible d'un récit simplement convaincant."
        }
        if (annual.rescue >= 35 && crisisWords.any(category::contains) && base.choices.size + extra.size < 7) {
            extra += Choice(
                label = "Organiser le sauvetage avant l'affrontement",
                moral = 2, opinion = 1, prestige = 1, impact = 1,
                risk = base.stakes.coerceAtLeast(1), approach = "CARE", stakes = base.stakes,
                sourceCategory = base.category, relationDelta = 1, flag = "ultimate_skill_rescue"
            )
            notes += "Tu sais désormais lire une zone de crise : sorties, personnes vulnérables, risques secondaires."
        }
        if (annual.presence >= 40 && publicWords.any(category::contains) && base.choices.size + extra.size < 7) {
            extra += Choice(
                label = "Prendre la parole sans laisser les autres écrire ta version",
                prestige = 1, opinion = 2, impact = 1, risk = base.stakes,
                approach = "ORDER", stakes = base.stakes, sourceCategory = base.category,
                flag = "ultimate_skill_presence"
            )
            notes += "Tu as assez de présence publique pour imposer quelques secondes de silence avant que les commentaires commencent."
        }
        if (annual.discipline >= 42 && crisisWords.any(category::contains) && base.choices.size + extra.size < 7) {
            extra += Choice(
                label = "Employer une technique maîtrisée plutôt que forcer",
                power = 1, prestige = 1, impact = 1, risk = (base.stakes - 1).coerceAtLeast(1),
                approach = "ORDER", stakes = base.stakes, sourceCategory = base.category,
                healthDelta = 1, flag = "ultimate_skill_discipline"
            )
            notes += "La répétition a transformé certains gestes en réflexes propres, moins spectaculaires mais beaucoup plus sûrs."
        }

        val trusted = state.relations.filter { it.trust >= 72 && it.status == "Présent" }.maxByOrNull { it.trust }
        if (trusted != null && relationWords.any(category::contains) && base.choices.size + extra.size < 7) {
            extra += Choice(
                label = "Faire confiance à ${trusted.name} et partager la décision",
                moral = 1, opinion = 1, relationDelta = 3, impact = 1, risk = base.stakes,
                approach = "CARE", stakes = base.stakes, sourceCategory = base.category,
                flag = "ultimate_relation_trust"
            )
            notes += "${trusted.name} n'est plus un figurant de ta carrière : assez d'années de confiance lui donnent un véritable poids dans ce moment."
        }

        if (state.legalStatus.contains("Autorisé") && publicWords.any(category::contains) && base.choices.size + extra.size < 7) {
            extra += Choice(
                label = "Activer le protocole officiel sans céder le contrôle de l'opération",
                prestige = 2, opinion = 1, impact = 1, risk = (base.stakes - 1).coerceAtLeast(1),
                approach = "ORDER", stakes = base.stakes, sourceCategory = base.category,
                flag = "ultimate_legal_protocol"
            )
        }
        if (state.legalStatus.contains("Recherché") && base.choices.size + extra.size < 7) {
            extra += Choice(
                label = "Intervenir sans laisser une trace exploitable par les autorités",
                fear = 1, impact = 1, risk = base.stakes + 1, approach = "TRUTH",
                stakes = base.stakes, sourceCategory = base.category, identityDelta = -3,
                flag = "ultimate_fugitive_method"
            )
            notes += "Ton statut légal transforme chaque intervention en second problème : réussir, puis réussir à partir."
        }

        if (state.nemesis.isNotBlank() && state.nemesisAdaptation >= 35 && category.contains("RIVAL")) {
            notes += "${state.nemesis} a déjà vu assez de tes habitudes pour anticiper ton style ${state.combatStyle.lowercase()}. Répéter le même plan augmente le risque."
        }
        if (district != null && district.damage >= 55) {
            notes += "${district.name} porte encore les traces d'anciennes crises. Les structures fragilisées et les habitants fatigués rendent la situation plus lourde qu'elle n'en a l'air."
        }
        if (district != null && district.sentiment <= -35) {
            notes += "Dans ${district.name}, ta présence ne rassure plus automatiquement les habitants."
        } else if (district != null && district.sentiment >= 45) {
            notes += "Dans ${district.name}, plusieurs personnes te reconnaissent comme quelqu'un qui revient quand les choses tournent mal."
        }
        state.cases.firstOrNull { !it.solved && it.stage >= 2 }?.let {
            if (investigationWords.any(category::contains)) notes += "Le dossier « ${it.title} » n'est toujours pas refermé ; certains détails de cette scène peuvent s'y raccrocher."
        }
        if (state.powerStrain >= 70 && crisisWords.any(category::contains)) {
            notes += "Ton pouvoir répond, mais ton corps connaît déjà la facture. Une surcharge ici pourrait laisser autre chose qu'une mauvaise nuit."
        }
        if (state.sponsor != "Aucun" && publicWords.any(category::contains)) {
            notes += "Ton partenaire ${state.sponsor} observe aussi cette scène : la célébrité finance certaines choses et en complique d'autres."
        }
        if (state.rareMarks.isNotEmpty() && positive(c.seed, c.turn * 97 + base.id.hashCode()) < 7) {
            notes += "Quelque chose d'anormal rappelle un détail que tu n'as jamais complètement expliqué : ${state.rareMarks.last()}."
        }

        if (crisisWords.any(category::contains) && base.choices.size + extra.size < 7) {
            val tactical = combatChoice(c, state, base)
            if (tactical.label !in (base.choices + extra).map { it.label }) extra += tactical
        }

        if (notes.isEmpty() && extra.isEmpty()) return base
        return base.copy(
            text = buildString {
                append(base.text)
                if (notes.isNotEmpty()) {
                    append("\n\n")
                    append(notes.distinct().take(4).joinToString("\n"))
                }
            },
            choices = (base.choices + extra).distinctBy { it.label }.take(7)
        )
    }

    private fun combatChoice(c: Campaign, state: UltimateState, base: EventNode): Choice {
        val branch = state.powerBranch
        return when {
            state.powerStrain >= 65 -> Choice(
                "Gagner du temps, protéger la zone et économiser le pouvoir",
                moral = 1, opinion = 1, impact = 1, risk = base.stakes,
                approach = "CARE", stakes = base.stakes, sourceCategory = base.category,
                healthDelta = 1, flag = "ultimate_combat_conserve"
            )
            state.combatStyle.contains("Distance") -> Choice(
                "Maintenir la distance et contrôler l'environnement",
                prestige = 1, power = 1, impact = 1, risk = base.stakes,
                approach = "ORDER", stakes = base.stakes, sourceCategory = base.category,
                flag = "ultimate_combat_range"
            )
            state.combatStyle.contains("Mobile") -> Choice(
                "Changer constamment d'angle et refuser le duel frontal",
                prestige = 1, impact = 1, risk = (base.stakes - 1).coerceAtLeast(1),
                approach = "TRUTH", stakes = base.stakes, sourceCategory = base.category,
                flag = "ultimate_combat_mobile"
            )
            branch != "Non spécialisée" -> Choice(
                "Construire l'intervention autour de ta spécialité « $branch »",
                power = 1, prestige = 1, impact = 2, risk = base.stakes,
                approach = if (c.control >= 55) "ORDER" else "ASCEND", stakes = base.stakes,
                sourceCategory = base.category, flag = "ultimate_power_branch"
            )
            else -> Choice(
                "Utiliser le décor pour éviter un affrontement purement frontal",
                opinion = 1, impact = 1, risk = base.stakes,
                approach = "TRUTH", stakes = base.stakes, sourceCategory = base.category,
                flag = "ultimate_environment_combat"
            )
        }
    }

    fun afterChoice(before: Campaign, afterCore: Campaign, rawState: UltimateState, event: EventNode, choice: Choice): Aftermath {
        // During the formative decade the ultimate layer records biography only. It never touches
        // campaign affinity/expression/cost vectors or adds formative choices.
        if (event.kind == "FORMATIVE" || event.kind == "AWAKENING" || !afterCore.powerRevealed) {
            val memory = "${before.age}|${event.id}|${event.category}|${choice.approach}|${choice.label}"
            val nextState = rawState.copy(
                memories = (rawState.memories + memory).takeLast(160),
                lastProcessedTurn = afterCore.turn,
                snapshots = addMilestoneSnapshot(afterCore, rawState)
            )
            return Aftermath(afterCore, nextState, "")
        }

        var state = rawState
        var campaign = afterCore
        val echo = mutableListOf<String>()
        val r = Random(mix(before.seed, before.turn.toLong() * 131L + event.id.hashCode()))
        val category = event.category.uppercase()
        val approach = choice.approach.ifBlank { "AMBIGU" }

        val memory = "${before.age}|${event.id}|${event.category}|$approach|${choice.label}"
        state = state.copy(memories = (state.memories + memory).takeLast(180), lastProcessedTurn = campaign.turn)

        state = advanceRelationships(before, campaign, state, event, choice, r, echo)
        state = advanceNpcLives(campaign, state, r, echo)
        state = advanceInvestigations(before, campaign, state, event, choice, r, echo)
        val combat = advanceCombatAndPower(before, campaign, state, event, choice, r, echo)
        campaign = combat.first
        state = combat.second
        state = advanceAppearanceAndGear(campaign, state, event, choice, echo)
        state = advanceCity(campaign, state, event, choice, r, echo)
        state = advanceEconomy(campaign, state, event, choice, r, echo)
        state = advanceMediaAndLaw(campaign, state, event, choice, echo)
        state = advanceMentorProtegeRomance(campaign, state, r, echo)
        state = advanceInternational(campaign, state, echo)
        state = advanceMysteryAndRare(campaign, state, event, r, echo)
        state = state.copy(snapshots = addMilestoneSnapshot(campaign, state))

        if (state.powerStrain >= 88 && crisisWords.any(category::contains)) {
            campaign = campaign.copy(health = (campaign.health - 3).coerceAtLeast(0))
            echo += "La surcharge dépasse le simple inconfort : ton corps ne récupère pas entièrement de cette utilisation."
        }
        if (state.cityCondition <= 25 && campaign.scope >= Scope.CITY) {
            echo += "La ville ne revient plus totalement à son état d'avant entre deux crises ; l'accumulation devient elle-même une partie de ton histoire."
        }

        return Aftermath(campaign, state, echo.distinct().take(6).joinToString("\n"))
    }

    private fun advanceRelationships(
        before: Campaign,
        c: Campaign,
        initial: UltimateState,
        event: EventNode,
        choice: Choice,
        r: Random,
        echo: MutableList<String>
    ): UltimateState {
        var state = initial
        val ids = mutableListOf("family", "friend")
        if (event.category.contains("RIVAL", true)) ids += "rival"
        if (event.category.contains("MEDIA", true)) ids += "journalist"
        ids.distinct().forEach { id ->
            val rel = state.relation(id) ?: return@forEach
            val care = choice.approach == "CARE"
            val truth = choice.approach == "TRUTH"
            val ascend = choice.approach == "ASCEND"
            val publicRisk = choice.identityDelta + if (event.stakes >= 3) 1 else 0
            val next = rel.copy(
                trust = rel.trust + when { care -> 2; truth -> 1; ascend && id != "rival" -> -1; else -> 0 },
                affection = rel.affection + if (care && id != "rival") 1 else 0,
                fear = rel.fear + if (c.fear > before.fear || ascend) 1 else 0,
                admiration = rel.admiration + if (c.prestige > before.prestige) 1 else 0,
                grudge = rel.grudge + if (id == "rival" && (choice.power > 0 || choice.fear > 0)) 2 else if (!care && id == "family") 1 else 0,
                dependence = rel.dependence + if (care && id == "family") 1 else 0,
                knowsIdentity = rel.knowsIdentity || (id == "family" && c.identityExposure >= 45) || (id == "rival" && c.identityExposure >= 30) || publicRisk >= 5,
                lastSeenTurn = c.turn
            ).clamped()
            state = state.replaceRelation(next)
        }
        val rival = state.relation("rival")
        if (state.nemesis.isBlank() && rival != null && rival.grudge >= 42) {
            state = state.copy(nemesis = rival.name, nemesisAdaptation = 8)
            echo += "${rival.name} cesse d'être un simple rival : assez de rancune et d'histoire commune viennent de créer une vraie Némésis."
        } else if (state.nemesis.isNotBlank() && event.category.contains("RIVAL", true)) {
            state = state.copy(nemesisAdaptation = (state.nemesisAdaptation + 4 + event.stakes).coerceIn(0, 100))
        }
        if (r.nextInt(100) < 8 && state.relations.any { it.status == "Présent" && it.id !in listOf("rival", "mentor") }) {
            val candidate = state.relations.filter { it.status == "Présent" && it.id !in listOf("rival", "mentor") }.maxByOrNull { it.affection }
            if (candidate != null && c.turn - candidate.lastSeenTurn > 10) {
                state = state.replaceRelation(candidate.copy(status = "Éloigné"))
                echo += "${candidate.name} prend de la distance. Les relations continuent d'exister même quand aucune crise ne les met au premier plan."
            }
        }
        return state
    }

    private fun advanceNpcLives(c: Campaign, initial: UltimateState, r: Random, echo: MutableList<String>): UltimateState {
        var state = initial
        if (c.turn % 12 != 0) return state
        state.relations.filter { it.status == "Éloigné" }.forEach { rel ->
            if (r.nextInt(100) < 30) {
                state = state.replaceRelation(rel.copy(status = "Revenu", trust = (rel.trust + 4).coerceAtMost(100), lastSeenTurn = c.turn))
                echo += "Après des années hors champ, ${rel.name} revient avec sa propre vie derrière lui·elle."
            }
        }
        return state
    }

    private fun advanceInvestigations(
        before: Campaign,
        c: Campaign,
        initial: UltimateState,
        event: EventNode,
        choice: Choice,
        r: Random,
        echo: MutableList<String>
    ): UltimateState {
        var state = initial
        val cat = event.category.uppercase()
        val isInvestigation = investigationWords.any(cat::contains) || choice.approach == "TRUTH"
        if (!isInvestigation) return state
        val active = state.cases.firstOrNull { !it.solved }
        if (active == null) {
            val id = "CASE_${event.id}_${c.turn}"
            val title = when {
                event.category.contains("RIVAL", true) -> "Les habitudes de ${state.nemesis.ifBlank { "ton rival" }}"
                event.text.contains("dispar", true) -> "La disparition sans réponse"
                else -> "Les incohérences de ${event.title.take(34)}"
            }
            val created = UltimateCase(id, title, stage = 1, evidence = 18, reliability = 45 + r.nextInt(35), falseLead = r.nextInt(100) < 18, openedTurn = before.turn)
            state = state.copy(cases = (state.cases + created).takeLast(12))
            echo += "Un vrai dossier s'ouvre : « $title ». Il pourra continuer au-delà de cette année."
        } else {
            val evidenceGain = 9 + if (choice.approach == "TRUTH") 8 else 2
            val nextStage = (active.stage + if (active.evidence + evidenceGain >= active.stage * 22 + 20) 1 else 0).coerceAtMost(active.maxStage)
            val solved = nextStage >= active.maxStage && active.evidence + evidenceGain >= 70
            val updated = active.copy(stage = nextStage, evidence = (active.evidence + evidenceGain).coerceIn(0, 100), solved = solved)
            state = state.copy(cases = state.cases.map { if (it.id == active.id) updated else it })
            when {
                solved && active.falseLead -> echo += "Le dossier « ${active.title} » se referme sur une correction importante : la piste qui semblait évidente était fausse."
                solved -> echo += "Le dossier « ${active.title} » atteint enfin une conclusion construite sur plusieurs années."
                nextStage > active.stage -> echo += "Le dossier « ${active.title} » avance d'un cran ; un détail ancien prend un nouveau sens."
            }
        }
        return state
    }

    private fun advanceCombatAndPower(
        before: Campaign,
        c: Campaign,
        initial: UltimateState,
        event: EventNode,
        choice: Choice,
        r: Random,
        echo: MutableList<String>
    ): Pair<Campaign, UltimateState> {
        var campaign = c
        var state = initial
        val cat = event.category.uppercase()
        val combat = crisisWords.any(cat::contains) || choice.power > 0 || event.stakes >= 3
        if (!combat) {
            state = state.copy(powerStrain = (state.powerStrain - 2).coerceAtLeast(0))
            return campaign to state
        }
        val style = when (choice.approach) {
            "CARE" -> "Protecteur"
            "ORDER" -> if (campaign.control >= 50) "Technique à distance" else "Contrôle tactique"
            "TRUTH" -> "Mobile / opportuniste"
            "ASCEND" -> "Pression frontale"
            else -> state.combatStyle
        }
        var strain = state.powerStrain + event.stakes * 3 + choice.power * 2 + choice.risk / 2
        if (choice.flag == "ultimate_combat_conserve") strain -= 8
        if (choice.flag == "ultimate_skill_discipline") strain -= 5
        strain = strain.coerceIn(0, 100)
        state = state.copy(combatStyle = style, powerStrain = strain)

        val branch = when {
            state.powerBranch != "Non spécialisée" -> state.powerBranch
            campaign.control >= 60 && choice.approach == "ORDER" -> "Précision"
            choice.approach == "CARE" && campaign.power >= 45 -> "Protection"
            choice.approach == "TRUTH" && campaign.control >= 45 -> "Mobilité"
            choice.approach == "ASCEND" && campaign.power >= 55 -> "Décharge maximale"
            else -> state.powerBranch
        }
        if (branch != state.powerBranch) {
            state = state.copy(powerBranch = branch)
            echo += "Ton pouvoir commence à se spécialiser naturellement : branche « $branch »."
        }

        val techniqueThreshold = 40 + state.techniques.size * 12
        if (campaign.control + campaign.power / 2 >= techniqueThreshold && state.techniques.size < 5) {
            val candidate = techniqueName(campaign, branch, style, state.techniques.size)
            if (candidate !in state.techniques) {
                state = state.copy(techniques = state.techniques + candidate)
                echo += "Une manière de faire revient assez souvent pour devenir une technique signature : « $candidate »."
            }
        }

        if (event.stakes >= 4 && choice.risk >= 6 && r.nextInt(100) < (12 + strain / 5)) {
            val injury = namedInjury(campaign, event, state.injuries.size)
            if (injury !in state.injuries) {
                state = state.copy(injuries = (state.injuries + injury).takeLast(8))
                campaign = campaign.copy(health = (campaign.health - 2).coerceAtLeast(0))
                echo += "Cette intervention laisse une séquelle qui restera dans ton dossier : $injury."
            }
        }
        return campaign to state
    }

    private fun techniqueName(c: Campaign, branch: String, style: String, index: Int): String {
        val root = when {
            c.powerFamily.contains("Énergie", true) -> listOf("Arc", "Pulse", "Surtension", "Couronne", "Éclair")
            c.powerFamily.contains("Grav", true) -> listOf("Ancre", "Puits", "Orbital", "Chute", "Horizon")
            c.powerFamily.contains("Mati", true) -> listOf("Forge", "Mue", "Trame", "Éclat", "Bastion")
            c.powerFamily.contains("Mental", true) || c.powerFamily.contains("Psy", true) -> listOf("Écho", "Silence", "Prisme", "Onde", "Verrou")
            else -> listOf("Impact", "Voile", "Vector", "Seuil", "Résonance")
        }[index.coerceIn(0, 4)]
        val suffix = when (branch) {
            "Protection" -> "Aegis"
            "Précision" -> "Zéro"
            "Mobilité" -> "Fantôme"
            "Décharge maximale" -> "Rouge"
            else -> style.substringBefore(' ')
        }
        return "$root $suffix"
    }

    private fun namedInjury(c: Campaign, event: EventNode, index: Int): String = when (index % 6) {
        0 -> "Épaule fragilisée — ${event.title.take(28)}"
        1 -> "Cicatrice au flanc — ${c.age} ans"
        2 -> "Traumatisme auditif récurrent"
        3 -> "Brûlure persistante à l'avant-bras"
        4 -> "Genou instable après une mauvaise réception"
        else -> "Marque de surcharge liée à ${c.powerFamily}"
    }

    private fun advanceAppearanceAndGear(c: Campaign, initial: UltimateState, event: EventNode, choice: Choice, echo: MutableList<String>): UltimateState {
        var state = initial
        val desiredEra = when {
            !c.powerRevealed -> 0
            c.prestige >= 75 || c.influence >= 600 -> 4
            c.prestige >= 50 || c.influence >= 260 -> 3
            c.prestige >= 25 || c.influence >= 90 -> 2
            else -> 1
        }
        if (desiredEra > state.costumeEra) {
            val firstHeroEra = state.costumeEra == 0
            val presentation = if (firstHeroEra || state.heroPresentation == "À découvrir") inferPresentation(c) else state.heroPresentation
            val palette = if (firstHeroEra || state.costumePalette == "Non définie") inferPalette(c) else state.costumePalette
            state = state.copy(
                costumeEra = desiredEra,
                heroPresentation = presentation,
                costumePalette = palette,
                maskStyle = if (firstHeroEra) "Masque minimal" else state.maskStyle,
                emblem = if (desiredEra >= 2 && state.emblem == "Aucun") "Comète" else state.emblem
            )
            echo += when (desiredEra) {
                1 -> "La période des vêtements civils adaptés touche à sa fin : une première identité de terrain prend forme."
                2 -> "Ton équipement cesse d'être un prototype ; une silhouette reconnaissable commence à exister."
                3 -> "Ton costume entre dans son époque iconique : le monde peut désormais reconnaître ta silhouette avant ton visage."
                else -> "Ton apparence de vétéran assume toute l'histoire accumulée : cicatrices, symboles et équipement ne ressemblent plus à ceux des débuts."
            }
        }
        val base = when {
            c.influence >= 700 -> 4
            c.influence >= 340 -> 3
            c.influence >= 150 -> 2
            c.identityExposure >= 45 || c.influence >= 60 -> 1
            else -> 0
        }
        if (base > state.baseStage) {
            val type = if (base >= 3 && state.baseType == "Logement civil") inferBaseType(c) else state.baseType
            state = state.copy(baseStage = base, baseType = type)
            echo += "Ton lieu de vie évolue avec la carrière : ${state.copy(baseStage = base).homeLabel()}."
        }
        if (choice.flag?.contains("mask", true) == true && state.maskStyle == "Aucun") state = state.copy(maskStyle = "Demi-masque")
        if (event.stakes >= 4 && state.costumeEra >= 1 && "Costume endommagé à ${c.age} ans" !in state.iconicItems) {
            state = state.copy(iconicItems = (state.iconicItems + "Costume endommagé à ${c.age} ans").takeLast(12))
        }
        return state
    }

    private fun inferPresentation(c: Campaign): String = when {
        c.fear >= 55 -> "Intimidant"
        c.governmentStanding >= 45 -> "Institutionnel"
        c.identityExposure <= 20 -> "Mystérieux"
        c.prestige >= 55 -> "Flamboyant"
        c.control >= 50 -> "Tactique"
        else -> "Sobre"
    }

    private fun inferPalette(c: Campaign): String = when {
        c.powerFamily.contains("Mental", true) || c.powerFamily.contains("Myst", true) -> "Violet / noir"
        c.powerFamily.contains("Énergie", true) || c.powerFamily.contains("Foudre", true) -> "Bleu / or"
        c.powerFamily.contains("Nature", true) -> "Vert / cuivre"
        c.fear >= 55 -> "Noir / argent"
        c.opinion >= 55 -> "Ivoire / or"
        else -> "Personnalisée au pouvoir"
    }

    private fun inferBaseType(c: Campaign): String = when {
        c.control >= 65 -> "Laboratoire"
        c.governmentStanding >= 45 -> "Centre de surveillance"
        c.fear >= 55 -> "Bunker"
        c.familyBond >= 70 -> "Refuge"
        else -> "Atelier"
    }

    private fun advanceCity(c: Campaign, initial: UltimateState, event: EventNode, choice: Choice, r: Random, echo: MutableList<String>): UltimateState {
        var state = initial
        if (state.districts.isEmpty()) return state
        val index = positive(c.seed, event.id.hashCode() + c.turn * 31) % state.districts.size
        var d = state.districts[index]
        val violent = choice.approach == "ASCEND" || choice.power >= 2 || choice.risk >= 7
        val caring = choice.approach == "CARE"
        val damageDelta = when {
            event.stakes >= 4 && violent -> 9
            event.stakes >= 3 && violent -> 5
            event.stakes >= 4 -> 3
            else -> -1
        }
        val reconstructionGain = if (event.category.contains("RECON", true) || caring) 2 else 0
        d = d.copy(
            sentiment = (d.sentiment + if (caring) 3 else if (violent) -2 else 1).coerceIn(-100, 100),
            damage = (d.damage + damageDelta - reconstructionGain).coerceIn(0, 100),
            crime = (d.crime + if (c.fear >= 55) -2 else if (event.category.contains("CRIM", true)) 3 else 0).coerceIn(0, 100),
            reconstruction = (d.reconstruction + reconstructionGain).coerceIn(0, 100),
            restricted = d.damage >= 75,
            landmark = if (event.stakes >= 4 && d.landmark.isBlank()) "${event.title.take(38)} — ${c.age} ans" else d.landmark
        )
        if (d.damage >= 55 && d.faction == "Aucune" && r.nextInt(100) < 20) d = d.copy(faction = "Faction opportuniste")
        state = state.replaceDistrict(d)
        val condition = (state.cityCondition - damageDelta / 2 + reconstructionGain).coerceIn(0, 100)
        val tech = (state.cityTech + if (c.modifier.contains("technologique", true) && c.turn % 8 == 0) 2 else 0).coerceIn(0, 100)
        state = state.copy(cityCondition = condition, cityTech = tech)
        if (d.landmark.isNotBlank() && d.landmark.contains(event.title.take(12))) echo += "${d.name} garde désormais une trace nommée de cet épisode : ${d.landmark}."
        if (d.restricted) echo += "Une partie de ${d.name} devient temporairement zone restreinte ; la carte de la ville porte désormais cette crise."
        return state
    }

    private fun advanceEconomy(c: Campaign, initial: UltimateState, event: EventNode, choice: Choice, r: Random, echo: MutableList<String>): UltimateState {
        var state = initial
        val baseIncome = when {
            c.civilianPath.contains("Commerce", true) -> 520
            c.civilianPath.contains("Technologie", true) -> 460
            c.civilianPath.contains("scient", true) -> 420
            c.civilianPath.contains("service", true) -> 350
            else -> 300
        }
        val heroIncome = if (state.sponsor != "Aucun") 240 + c.prestige * 2 else 0
        val cost = 120 + state.baseStage * 110 + if (event.stakes >= 4) 180 else 0 + if (choice.approach == "ASCEND") 90 else 0
        var credits = state.credits + baseIncome / 4 + heroIncome / 4 - cost
        var debt = state.debt
        if (credits < 0) { debt += -credits; credits = 0 }
        if (credits > 3500 && debt > 0) {
            val payment = minOf(debt, 200)
            debt -= payment; credits -= payment
        }
        var sponsor = state.sponsor
        if (sponsor == "Aucun" && c.prestige >= 55 && c.mediaStanding >= 15 && r.nextInt(100) < 12) {
            sponsor = listOf("Aster Dynamics", "Northstar Relief", "Vox Athletic", "Helix Mobility")[r.nextInt(4)]
            echo += "$sponsor propose de financer une partie de ton activité. L'argent simplifie le matériel et complique l'indépendance."
        }
        if (sponsor != "Aucun" && (c.opinion <= -45 || c.civilianCasualties >= 8) && r.nextInt(100) < 35) {
            echo += "$sponsor coupe publiquement le partenariat après la dernière polémique."
            sponsor = "Aucun"
        }
        val tier = when {
            credits >= 12000 -> 4
            credits >= 6000 -> 3
            credits >= 2800 -> 2
            else -> 1
        }
        return state.copy(credits = credits.coerceAtMost(99999), debt = debt.coerceAtMost(99999), sponsor = sponsor, incomeTier = tier)
    }

    private fun advanceMediaAndLaw(c: Campaign, initial: UltimateState, event: EventNode, choice: Choice, echo: MutableList<String>): UltimateState {
        val frame = when {
            c.opinion >= 65 && c.prestige >= 55 -> "Icône populaire"
            c.opinion >= 35 -> "Protecteur controversé"
            c.fear >= 70 -> "Menace publique"
            c.governmentStanding >= 55 -> "Agent officieux"
            c.mediaStanding <= -45 -> "Figure sous soupçon"
            c.identityExposure >= 65 -> "Célébrité involontaire"
            else -> "Sujet débattu"
        }
        val legal = when {
            c.civilianCasualties >= 12 || c.governmentStanding <= -65 -> "Recherché internationalement"
            c.governmentStanding <= -35 -> "Vigilante recherché"
            c.governmentStanding >= 65 && c.prestige >= 55 -> "Héros autorisé"
            c.governmentStanding >= 35 -> "Vigilante toléré"
            else -> "Statut non défini"
        }
        val law = when {
            c.scope >= Scope.COUNTRY && c.fear >= 55 -> "Loi de contrôle métahumain renforcée"
            c.scope >= Scope.REGION && c.opinion >= 50 -> "Cadre de coopération métahumaine"
            c.identityExposure >= 70 -> "Registre d'identité débattu"
            else -> initial.metaLaw
        }
        if (frame != initial.mediaFrame) echo += "Le récit médiatique change : on te décrit désormais surtout comme « $frame »."
        if (legal != initial.legalStatus) echo += "Ton statut légal évolue : $legal."
        if (law != initial.metaLaw) echo += "Les institutions réagissent à l'époque que tu aides à créer : $law."
        val generational = (c.opinion / 3 - c.fear / 4 + if (c.age >= 50) 5 else 0).coerceIn(-100, 100)
        return initial.copy(mediaFrame = frame, legalStatus = legal, metaLaw = law, generationOpinion = generational)
    }

    private fun advanceMentorProtegeRomance(c: Campaign, initial: UltimateState, r: Random, echo: MutableList<String>): UltimateState {
        var state = initial
        if (state.mentor.isBlank() && c.age <= 38 && c.control < 48 && c.prestige >= 12) {
            val candidate = state.relation("mentor")
            if (candidate != null && r.nextInt(100) < 16) {
                state = state.copy(mentor = candidate.name).replaceRelation(candidate.copy(status = "Mentor", trust = 62, admiration = 35).clamped())
                echo += "${candidate.name} ne reste plus une connaissance : une relation de mentorat commence, avec ses conseils et ses futurs désaccords."
            }
        }
        if (state.protege.isBlank() && c.age >= 36 && c.prestige >= 45 && c.control >= 50 && r.nextInt(100) < 10) {
            val name = listOf("Iris", "Kade", "Milo", "Sana", "Ezra", "Nox", "Ari")[r.nextInt(7)]
            state = state.copy(protege = name, relations = state.relations + UltimateRelation("protege", name, "Protégé·e", ageOffset = -18, trust = 55, affection = 40, admiration = 72, dependence = 38, lastSeenTurn = c.turn))
            echo += "$name te demande de l'aider à comprendre ce qu'il·elle devient. Tu passes pour la première fois de l'autre côté du mentorat."
        }
        if (state.romance.isBlank() && c.age in 22..55 && r.nextInt(100) < 7) {
            val friend = state.relation("friend")
            if (friend != null && friend.affection >= 58 && friend.trust >= 50) {
                state = state.copy(romance = friend.name).replaceRelation(friend.copy(role = "Relation intime", affection = (friend.affection + 8).coerceAtMost(100)))
                echo += "Avec ${friend.name}, quelque chose change au-delà de l'amitié. Cette relation devra désormais survivre à ta vie publique autant qu'à ta vie privée."
            }
        }
        return state
    }

    private fun advanceInternational(c: Campaign, initial: UltimateState, echo: MutableList<String>): UltimateState {
        val attention = when (c.scope) {
            Scope.WORLD -> 95
            Scope.COUNTRY -> 70
            Scope.REGION -> 48
            Scope.CITY -> 28
            else -> 10
        }.coerceAtLeast(initial.internationalAttention - 1)
        if (attention >= 70 && initial.internationalAttention < 70) echo += "Ta carrière cesse d'être seulement une histoire locale : gouvernements et métahumains étrangers commencent à suivre tes décisions."
        return initial.copy(internationalAttention = attention)
    }

    private fun advanceMysteryAndRare(c: Campaign, initial: UltimateState, event: EventNode, r: Random, echo: MutableList<String>): UltimateState {
        var stage = initial.mysteryStage
        val cat = event.category.uppercase()
        if (investigationWords.any(cat::contains) && c.turn % 9 == 0) stage = (stage + 1).coerceAtMost(8)
        var marks = initial.rareMarks
        val rareRoll = positive(c.seed xor 0xD1B54A32D192ED03UL.toLong(), c.turn * 1009 + event.id.hashCode())
        if (rareRoll < 2 && marks.size < 6) {
            val mark = listOf(
                "le même symbole aperçu dans deux lieux sans lien apparent",
                "un signal impossible enregistré quelques secondes avant une crise",
                "un témoin qui semble connaître ton pouvoir avant sa manifestation publique",
                "une archive où ton alias apparaît avant que tu ne l'aies choisi",
                "une zone de la ville absente de certains plans officiels",
                "un adversaire qui reconnaît une technique que tu viens seulement d'inventer"
            )[r.nextInt(6)]
            marks = marks + mark
            echo += "Événement rare : $mark. Rien ne garantit que tu comprendras cette anomalie pendant cette vie."
        }
        return initial.copy(mysteryStage = stage, rareMarks = marks)
    }

    private fun addMilestoneSnapshot(c: Campaign, state: UltimateState): List<String> {
        val milestone = when {
            c.age == 18 && c.powerRevealed -> "Éveil"
            c.age in listOf(35, 50, 65) -> "${c.age} ans"
            c.scope == Scope.WORLD && state.snapshots.none { it.contains("Monde") } -> "Portée Monde"
            else -> null
        } ?: return state.snapshots
        val entry = "${c.age}|$milestone|${state.hair}|${if (c.powerRevealed) state.heroPresentation else state.civilianStyle}"
        return if (entry in state.snapshots) state.snapshots else (state.snapshots + entry).takeLast(12)
    }

    fun extraAnnualActions(c: Campaign, state: UltimateState, annual: AnnualActionState): List<AnnualActionCard> {
        if (annual.remaining <= 0 || (!c.powerRevealed && c.turn >= 10)) return emptyList()
        if (!c.powerRevealed && c.age < 18) return emptyList()
        val all = mutableListOf<AnnualActionCard>()
        all += AnnualActionCard("ultimate_hair", "Changer vraiment de style", "Coiffeur, barbe, tenue civile : faire évoluer ton apparence sans transformer ça en événement annuel majeur.", AnnualActionCategory.CIVIL, "alt_02", "Présence · Apparence", "Ton portrait de cette période ne sera plus exactement le même.", presence = 1)
        all += AnnualActionCard("ultimate_finances", "Mettre tes affaires en ordre", "Budget, assurances, dettes et revenus : la partie de la double vie qui ne fait aucune couverture de journal.", AnnualActionCategory.CIVIL, "prestige_01", "Finances · Discipline", "Tu évites que les urgences financières deviennent une crise supplémentaire.", discipline = 1)
        all += AnnualActionCard("ultimate_friend", "Retrouver quelqu'un que tu négliges", "Choisir une personne avant qu'une relation ne devienne seulement un souvenir dans la Chronique.", AnnualActionCategory.RELATION, "relation_family", "Liens", "Une conversation ordinaire empêche parfois une relation extraordinaire de disparaître.", familyBond = 2, presence = 2)
        if (c.powerRevealed) {
            all += AnnualActionCard("ultimate_case", "Travailler un dossier froid", "Reprendre des indices anciens sans attendre qu'une nouvelle catastrophe te les remette sous le nez.", AnnualActionCategory.INVESTIGATION, "alt_03", "Enquête · Mémoire", "Une vieille incohérence reprend du relief.", requiresPower = true, investigation = 4)
            all += AnnualActionCard("ultimate_costume", "Retravailler le costume", "Masque, matière, mobilité et lisibilité : améliorer la silhouette sans ouvrir un mini-jeu d'équipement.", AnnualActionCategory.TRAINING, "alt_04", "Identité · Discipline", "Ton équipement ressemble davantage à une extension de tes habitudes qu'à un déguisement.", requiresPower = true, discipline = 2, control = 1)
            all += AnnualActionCard("ultimate_base", "Améliorer le refuge", "Sécurité, soins, stockage, surveillance : investir dans l'endroit où toute la carrière revient entre deux crises.", AnnualActionCategory.CIVIL, "scope_district", "QG · Finances", "Ton lieu de vie gagne une fonction de plus et un peu de tranquillité en moins.", requiresPower = true, influence = 1)
            all += AnnualActionCard("ultimate_interview", "Accepter une vraie interview", "Pas une phrase au milieu d'une catastrophe : une conversation où tes mots auront leur propre vie publique.", AnnualActionCategory.PUBLIC, "relation_media", "Présence · Médias", "Tu donnes au public une version plus humaine de toi, au prix d'un peu plus d'exposition.", requiresPower = true, presence = 4, opinion = 2, mediaStanding = 2, identityExposure = 2)
            all += AnnualActionCard("ultimate_mentor", "Voir le mentor / le protégé", "Apprendre, transmettre ou simplement comparer deux manières de survivre à cette vie.", AnnualActionCategory.RELATION, "relation_family", "Liens · Maîtrise", "La relation avance hors des grandes scènes et rend les prochaines plus compliquées.", requiresPower = true, discipline = 2, control = 1)
            all += AnnualActionCard("ultimate_district", "Retourner dans un quartier marqué", "Voir les réparations, les habitants et ce qu'une ancienne intervention a vraiment laissé.", AnnualActionCategory.INTERVENTION, "scope_city", "Ville · Présence", "Le quartier ne redevient pas un décor neutre simplement parce que la fumée a disparu.", requiresPower = true, presence = 2, rescue = 1, opinion = 1)
            if (state.powerStrain >= 35) all += AnnualActionCard("ultimate_recovery", "Faire une vraie récupération", "Sommeil, kiné, contrôle du pouvoir et temps sans performance.", AnnualActionCategory.RECOVERY, "alt_07", "Santé · Surcharge", "Tu retires un peu de dette à ton corps avant qu'elle ne te soit réclamée en pleine crise.", requiresPower = true, health = 4, discipline = 2)
            if (state.legalStatus.contains("Recherché")) all += AnnualActionCard("ultimate_cover", "Construire un alibi", "Mettre de l'ordre dans les traces, horaires et témoins qui relient trop facilement tes deux vies.", AnnualActionCategory.INVESTIGATION, "alt_06", "Identité · Enquête", "Ton histoire civile redevient légèrement plus crédible.", requiresPower = true, investigation = 3, identityExposure = -3)
        }
        val r = Random(mix(c.seed, c.turn * 271L + annual.used))
        return all.map { it to r.nextLong() }.sortedBy { it.second }.map { it.first }.take(8)
    }

    fun afterAnnualAction(c: Campaign, initial: UltimateState, annual: AnnualActionState, card: AnnualActionCard): UltimateState {
        var state = initial
        when (card.id) {
            "civil_barber", "ultimate_hair" -> {
                val nextHair = UltimateCatalog.hairs[(UltimateCatalog.hairs.indexOf(state.hair).coerceAtLeast(0) + 1) % UltimateCatalog.hairs.size]
                state = state.copy(hair = nextHair, snapshots = (state.snapshots + "${c.age}|Nouveau style|$nextHair|${state.civilianStyle}").takeLast(12))
            }
            "ultimate_finances" -> {
                val payment = minOf(state.debt, 350)
                state = state.copy(debt = state.debt - payment, credits = state.credits + 180 - payment)
            }
            "ultimate_friend", "civil_family", "meta_family" -> {
                val friend = state.relation("friend")
                if (friend != null) state = state.replaceRelation(friend.copy(trust = friend.trust + 5, affection = friend.affection + 6, status = "Présent", lastSeenTurn = c.turn).clamped())
            }
            "ultimate_case", "meta_missing", "meta_tip", "meta_watch_faction" -> {
                val active = state.cases.firstOrNull { !it.solved }
                if (active != null) state = state.copy(cases = state.cases.map { if (it.id == active.id) active.copy(evidence = (active.evidence + 10).coerceAtMost(100), stage = (active.stage + 1).coerceAtMost(active.maxStage)) else it })
            }
            "ultimate_costume" -> state = state.copy(costumeEra = maxOf(1, state.costumeEra), iconicItems = (state.iconicItems + "Révision costume — ${c.age} ans").distinct().takeLast(12))
            "ultimate_base" -> state = state.copy(baseStage = (state.baseStage + 1).coerceAtMost(4), credits = (state.credits - 300).coerceAtLeast(0), baseType = if (state.baseType == "Logement civil") inferBaseType(c) else state.baseType)
            "ultimate_interview", "meta_charity" -> state = state.copy(mediaFrame = if (c.opinion >= 30) "Figure accessible" else state.mediaFrame)
            "ultimate_mentor" -> {
                val id = if (state.protege.isNotBlank()) "protege" else "mentor"
                val rel = state.relation(id)
                if (rel != null) state = state.replaceRelation(rel.copy(trust = rel.trust + 5, affection = rel.affection + 3, admiration = rel.admiration + 2, lastSeenTurn = c.turn).clamped())
            }
            "ultimate_district", "meta_cleanup", "civil_volunteer" -> {
                val d = state.district(c.district) ?: state.districts.firstOrNull()
                if (d != null) state = state.replaceDistrict(d.copy(sentiment = (d.sentiment + 5).coerceAtMost(100), damage = (d.damage - 4).coerceAtLeast(0), reconstruction = (d.reconstruction + 5).coerceAtMost(100)))
            }
            "ultimate_recovery", "meta_physio", "civil_rest" -> state = state.copy(powerStrain = (state.powerStrain - 14).coerceAtLeast(0))
            "ultimate_cover", "meta_media_silence" -> {
                val journalist = state.relation("journalist")
                if (journalist != null) state = state.replaceRelation(journalist.copy(trust = (journalist.trust - 1).coerceAtLeast(0)))
            }
        }
        // Keep annual skills mechanically connected to the main story without affecting formative vectors.
        val skillMemory = "skill|${c.turn}|${annual.rescue}|${annual.investigation}|${annual.presence}|${annual.discipline}|${card.id}"
        return state.copy(memories = (state.memories + skillMemory).takeLast(180))
    }

    fun legacySummary(c: Campaign, s: UltimateState): String {
        val rel = s.relations.maxByOrNull { it.affection + it.trust }
        val district = s.districts.maxByOrNull { it.sentiment }
        val solved = s.cases.count { it.solved }
        val promise = when {
            c.morality >= 45 -> "Le monde retient surtout les personnes que tu as refusé d'abandonner."
            c.fear >= 65 -> "Le monde se souvient de l'ordre que ta présence imposait avant même ton arrivée."
            else -> "Le monde n'arrive jamais tout à fait à décider si ton héritage fut une protection, une rupture ou les deux."
        }
        return buildString {
            append(promise)
            rel?.let { append(" ${it.name} reste la relation la plus profondément liée à cette carrière.") }
            if (s.nemesis.isNotBlank()) append(" ${s.nemesis} demeure le nom le plus souvent associé à tes affrontements.")
            if (district != null) append(" ${district.name} est le quartier où ton souvenir est le plus favorable.")
            if (solved > 0) append(" $solved dossier${if (solved > 1) "s" else ""} ont trouvé une conclusion grâce à des années d'enquête.")
            if (s.techniques.isNotEmpty()) append(" Tes techniques signatures — ${s.techniques.joinToString(", ")} — survivent dans les récits de ceux qui t'ont vu agir.")
            if (s.iconicItems.isNotEmpty()) append(" Des objets de différentes époques de ta carrière finissent archivés comme des morceaux d'histoire.")
            append(" ${s.cityArchetype}, ${s.climate.lowercase()}, ne ressemble plus tout à fait à la ville de ton enfance.")
        }
    }

    private fun mix(a: Long, b: Long): Long {
        var z = a xor (b + 0x9E3779B97F4A7C15UL.toLong())
        z = (z xor (z ushr 30)) * 0xBF58476D1CE4E5B9UL.toLong()
        z = (z xor (z ushr 27)) * 0x94D049BB133111EBUL.toLong()
        return z xor (z ushr 31)
    }

    private fun positive(seed: Long, salt: Int): Int = (mix(seed, salt.toLong()) % 100L).toInt().absoluteValue
}
