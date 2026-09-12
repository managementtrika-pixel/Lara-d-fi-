package com.metahumanlegacy.game

internal data class DeepLifeUpdate(val state: DeepLifeState, val echo: String = "")

/** Turns V2 domain data into actual long-term consequences without replacing the authored story engine. */
internal object DeepLifeRuntime {
    fun afterChoice(
        before: Campaign,
        after: Campaign,
        event: EventNode,
        choice: Choice,
        current: DeepLifeState
    ): DeepLifeUpdate {
        var state = DeepLifeDirector.afterChoice(before, after, event, choice, current)
        val echoes = mutableListOf<String>()

        state = expireOpportunities(after, state, echoes)
        state = resolveDeferred(after, state, echoes)
        state = recordIdentityEvidence(before, after, event, choice, state, echoes)
        state = recordPersistentInjury(before, after, event, state, echoes)
        state = createDeferred(after, event, choice, state)
        state = generateOpportunities(after, state)
        state = evolvePower(after, event, choice, state, echoes)
        state = rebalanceDrama(after, state)
        state = DeepLifeDirector.revealPower(after, state)

        if (event.kind == "AWAKENING") {
            val roots = DeepLifeDirector.awakeningMemoryLines(state)
            if (roots.isNotEmpty()) {
                echoes += buildString {
                    append("CE QUI T'A FAÇONNÉ\n")
                    roots.forEach { append("• ").append(it).append('\n') }
                    append("Ces souvenirs n'ont pas choisi ton pouvoir à ta place : ils ont façonné la manière dont il s'est manifesté.")
                }
            }
        }

        return DeepLifeUpdate(state, echoes.distinct().take(5).joinToString("\n\n"))
    }

    fun afterAnnualAction(c: Campaign, card: AnnualActionCard, current: DeepLifeState): DeepLifeUpdate {
        var state = current
        val echo = mutableListOf<String>()
        val personality = state.personality.toMutableMap()
        fun shift(key: String, v: Int) { personality[key] = ((personality[key] ?: 0) + v).coerceIn(-100, 100) }
        when (card.category) {
            AnnualActionCategory.RELATION -> { shift("LOYAL", 1); shift("EMPATHIQUE", 1) }
            AnnualActionCategory.TRAINING -> shift("DISCIPLINÉ", 1)
            AnnualActionCategory.INVESTIGATION -> shift("CURIEUX", 1)
            AnnualActionCategory.RECOVERY -> shift("PRUDENT", 1)
            AnnualActionCategory.PUBLIC -> shift("AMBITIEUX", 1)
            AnnualActionCategory.INTERVENTION -> shift("PROTECTEUR", 1)
            AnnualActionCategory.CIVIL -> shift("ANCRÉ", 1)
        }
        state = state.copy(personality = personality)

        if (card.category == AnnualActionCategory.RECOVERY) {
            state = state.copy(
                injuries = state.injuries.map { it.copy(recovery = (it.recovery + 18).coerceAtMost(100)) }
                    .filterNot { it.recovery >= 100 && !it.chronic },
                drama = state.drama.copy(
                    recoveryNeed = (state.drama.recoveryNeed - 22).coerceAtLeast(0),
                    tension = (state.drama.tension - 10).coerceAtLeast(0)
                ),
                powerEvolution = state.powerEvolution?.let { it.copy(strain = (it.strain - 18).coerceAtLeast(0)) }
            )
            echo += "Tu récupères réellement : les blessures légères cicatrisent, la tension baisse et ton pouvoir cesse un peu de tirer sur la corde."
        }

        if (card.category == AnnualActionCategory.TRAINING && c.powerRevealed) {
            val p = state.powerEvolution
            if (p != null) {
                val gain = when {
                    p.mastery < 40 -> 4
                    p.mastery < 70 -> 2
                    p.mastery < 90 -> 1
                    else -> 0
                }
                val technique = techniqueUnlock(c, p, gain)
                state = state.copy(powerEvolution = p.copy(
                    mastery = (p.mastery + gain).coerceAtMost(100),
                    strain = (p.strain + 4).coerceAtMost(100),
                    unlockedTechniques = (p.unlockedTechniques + listOfNotNull(technique)).distinct()
                ))
                if (technique != null) echo += "Ton entraînement débloque une nouvelle manière d'utiliser ${c.powerFamily.lowercase()} : $technique."
            }
        }

        state = applyDirectRelationshipAction(c, card, state, echo)

        val matching = state.opportunities.firstOrNull { opportunityMatches(it, card) }
        if (matching != null) {
            state = state.copy(opportunities = state.opportunities.filterNot { it.id == matching.id })
            echo += "Tu as choisi de consacrer du temps à « ${matching.title} ». Une autre urgence devra attendre."
        }
        state = generateOpportunities(c, state)
        return DeepLifeUpdate(state, echo.distinct().joinToString("\n\n"))
    }

    fun perceptionSummary(state: DeepLifeState): String {
        val p = state.perception
        fun label(v: Int) = when {
            v >= 60 -> "admiré"
            v >= 25 -> "apprécié"
            v <= -60 -> "détesté"
            v <= -25 -> "contesté"
            else -> "partagé"
        }
        return "Quartier ${label(p.district)} · Ville ${label(p.city)} · Gouvernement ${label(p.government)} · Criminels: peur ${p.criminalFear.coerceIn(0, 100)}"
    }

    private fun applyDirectRelationshipAction(c: Campaign, card: AnnualActionCard, state: DeepLifeState, echo: MutableList<String>): DeepLifeState {
        if (!card.id.startsWith("direct_rel_")) return state
        val payload = card.id.removePrefix("direct_rel_")
        val action = payload.substringBefore('_')
        val personId = payload.substringAfter('_', "")
        if (personId.isBlank()) return state
        val person = state.relationships.firstOrNull { it.id == personId && it.alive } ?: return state
        val memory = CharacterMemory(
            id = "direct_${c.turn}_${person.id}_$action", turn = c.turn, age = c.age,
            personId = person.id, eventId = "DIRECT_RELATION", summary = card.title,
            emotion = when (action) { "apologize" -> "VULNÉRABILITÉ"; "distance" -> "RUPTURE"; else -> "ATTACHEMENT" },
            weight = when (action) { "apologize" -> 6; "distance" -> 7; else -> 4 },
            tags = setOf("RELATION", "DIRECT", action.uppercase())
        )
        val relationships = state.relationships.map { rel ->
            if (rel.id != person.id) rel else when (action) {
                "visit" -> rel.copy(
                    trust = (rel.trust + 5).coerceAtMost(100), affection = (rel.affection + 6).coerceAtMost(100),
                    grudge = (rel.grudge - 2).coerceAtLeast(0),
                    phase = if (rel.phase == RelationshipPhase.DISTANT && rel.trust + 5 >= 45) RelationshipPhase.FRIEND else rel.phase,
                    memories = (rel.memories + memory).takeLast(24)
                )
                "apologize" -> rel.copy(
                    trust = (rel.trust + 4).coerceAtMost(100), grudge = (rel.grudge - 10).coerceAtLeast(0),
                    phase = if (rel.phase == RelationshipPhase.HURT && rel.grudge - 10 < 45) RelationshipPhase.FRIEND else rel.phase,
                    memories = (rel.memories + memory).takeLast(24)
                )
                "help" -> rel.copy(
                    dependence = (rel.dependence + 4).coerceAtMost(100), trust = (rel.trust + 2).coerceAtMost(100),
                    admiration = (rel.admiration + 2).coerceAtMost(100), memories = (rel.memories + memory).takeLast(24)
                )
                "distance" -> rel.copy(
                    trust = (rel.trust - 12).coerceAtLeast(0), affection = (rel.affection - 10).coerceAtLeast(0),
                    grudge = (rel.grudge + 8).coerceAtMost(100), phase = RelationshipPhase.DISTANT,
                    memories = (rel.memories + memory).takeLast(24)
                )
                else -> rel
            }
        }
        echo += when (action) {
            "visit" -> "Tu choisis ${person.name} avant une urgence. Cette présence devient un vrai souvenir partagé."
            "apologize" -> "Tu présentes de vraies excuses à ${person.name}. La blessure ne disparaît pas, mais elle cesse d'être ignorée."
            "help" -> "Tu demandes l'aide de ${person.name}. Faire entrer quelqu'un dans le problème renforce le lien autant que sa dépendance."
            "distance" -> "Tu mets volontairement de la distance avec ${person.name}. Cette relation ne reste pas inchangée hors champ."
            else -> ""
        }
        return state.copy(
            relationships = relationships,
            memories = (state.memories + memory).sortedByDescending { it.weight }.take(120),
            drama = state.drama.copy(relationshipPressure = when (action) {
                "visit", "apologize" -> (state.drama.relationshipPressure - 6).coerceAtLeast(0)
                "distance" -> (state.drama.relationshipPressure + 8).coerceAtMost(100)
                else -> state.drama.relationshipPressure
            })
        )
    }

    private fun recordIdentityEvidence(before: Campaign, after: Campaign, event: EventNode, choice: Choice, state: DeepLifeState, echo: MutableList<String>): DeepLifeState {
        val delta = after.identityExposure - before.identityExposure
        if (!after.powerRevealed || delta <= 0) return state
        val kind = when {
            event.category.contains("MEDIA", true) -> "VIDÉO"
            event.category.contains("SANT", true) -> "BLESSURE"
            choice.power >= 3 -> "SIGNATURE_POUVOIR"
            else -> "TÉMOIGNAGE"
        }
        val holder = if (state.relationships.any { it.id == "journalist" }) "journalist" else "public"
        val evidence = IdentityEvidence(
            id = "ev_${after.turn}_${event.id}_$kind", kind = kind,
            strength = (delta * 8 + event.stakes * 3).coerceIn(5, 100), holderId = holder,
            discoveredTurn = after.turn, description = when (kind) {
                "VIDÉO" -> "Une séquence exploitable rapproche ta silhouette civile de ton identité métahumaine."
                "BLESSURE" -> "Une blessure visible ressemble trop à celle observée après l'intervention."
                "SIGNATURE_POUVOIR" -> "La signature de ton pouvoir a été captée avec assez de précision pour être comparée."
                else -> "Un témoin ajoute un détail cohérent au puzzle de ton identité."
            }
        )
        val evidenceList = (state.identityEvidence + evidence).distinctBy { it.id }.takeLast(40)
        val journalistStrength = evidenceList.filter { it.holderId == "journalist" }.sumOf { it.strength }
        var relationships = state.relationships
        if (journalistStrength >= 120) {
            relationships = relationships.map { if (it.id == "journalist" && !it.knowsIdentity) it.copy(knowsIdentity = true) else it }
            if (state.relationships.any { it.id == "journalist" && !it.knowsIdentity }) {
                echo += "Les indices se recoupent : ${state.relationships.first { it.id == "journalist" }.name} comprend désormais qui se cache derrière le masque."
            }
        }
        return state.copy(identityEvidence = evidenceList, relationships = relationships)
    }

    private fun recordPersistentInjury(before: Campaign, after: Campaign, event: EventNode, state: DeepLifeState, echo: MutableList<String>): DeepLifeState {
        val lost = before.health - after.health
        if (lost < 5 || event.kind == "FORMATIVE") return state
        val bodyParts = listOf("épaule", "côtes", "genou", "poignet", "dos", "cheville")
        val part = bodyParts[positiveMod(mix(after.seed, after.turn.toLong() * 97 + event.id.hashCode()), bodyParts.size)]
        val injury = PersistentInjury(
            id = "inj_${after.turn}_${event.id}", bodyPart = part, severity = lost.coerceIn(1, 10),
            originEvent = event.title, originAge = after.age, chronic = lost >= 9 || after.age >= 55,
            recovery = 0
        )
        echo += "Cette fois, ce n'est pas seulement une jauge de santé : $part touché lors de « ${event.title} »."
        return state.copy(injuries = (state.injuries + injury).distinctBy { it.id }.takeLast(20))
    }

    private fun createDeferred(c: Campaign, event: EventNode, choice: Choice, state: DeepLifeState): DeepLifeState {
        if (!choice.deferredHook && event.stakes < 3 && kotlin.math.abs(choice.relationDelta) < 2) return state
        val delay = 4 + positiveMod(mix(c.seed, event.id.hashCode().toLong() + c.turn), 18)
        val d = DeferredConsequence(
            id = "deferred_${c.turn}_${event.id}_${choice.label.hashCode()}", sourceEvent = event.id,
            earliestTurn = c.turn + delay, latestTurn = c.turn + delay + 20,
            triggerTags = setOf(event.category, choice.approach).filter { it.isNotBlank() }.toSet(),
            payload = deferredText(c, event, choice), resolved = false
        )
        return state.copy(deferred = (state.deferred + d).distinctBy { it.id }.takeLast(48))
    }

    private fun resolveDeferred(c: Campaign, state: DeepLifeState, echo: MutableList<String>): DeepLifeState {
        var changed = false
        val next = state.deferred.map { d ->
            if (!d.resolved && c.turn in d.earliestTurn..d.latestTurn && positiveMod(mix(c.seed, c.turn * 911L + d.id.hashCode()), 100) < 28) {
                echo += d.payload
                changed = true
                d.copy(resolved = true)
            } else d
        }.filterNot { it.resolved && c.turn - it.earliestTurn > 12 }
        return if (changed || next.size != state.deferred.size) state.copy(deferred = next) else state
    }

    private fun expireOpportunities(c: Campaign, state: DeepLifeState, echo: MutableList<String>): DeepLifeState {
        val expired = state.opportunities.filter { c.turn > it.expiresTurn }
        if (expired.isEmpty()) return state
        var next = state.copy(opportunities = state.opportunities - expired.toSet())
        expired.forEach { opportunity ->
            if (opportunity.ignoredPayload.isNotBlank()) echo += opportunity.ignoredPayload
            when (opportunity.category) {
                "RELATION" -> {
                    val target = opportunity.personId
                    next = next.copy(
                        relationships = next.relationships.map { person ->
                            if (target != null && person.id == target) person.copy(
                                trust = (person.trust - 5).coerceAtLeast(0),
                                affection = (person.affection - 3).coerceAtLeast(0),
                                grudge = (person.grudge + 4).coerceAtMost(100),
                                phase = if (person.trust - 5 <= 20) RelationshipPhase.DISTANT else person.phase
                            ) else person
                        },
                        drama = next.drama.copy(
                            relationshipPressure = (next.drama.relationshipPressure + 6).coerceAtMost(100),
                            personalPressure = (next.drama.personalPressure + 4).coerceAtMost(100)
                        )
                    )
                }
                "RECOVERY" -> next = next.copy(
                    powerEvolution = next.powerEvolution?.let { it.copy(strain = (it.strain + 10).coerceAtMost(100)) },
                    drama = next.drama.copy(recoveryNeed = (next.drama.recoveryNeed + 12).coerceAtMost(100))
                )
                "INVESTIGATION" -> next = next.copy(
                    identityEvidence = next.identityEvidence.map { evidence ->
                        if (evidence.holderId == opportunity.personId || opportunity.personId == null)
                            evidence.copy(strength = (evidence.strength + 8).coerceAtMost(100))
                        else evidence
                    }
                )
                "INTERVENTION" -> next = next.copy(
                    perception = next.perception.copy(
                        district = (next.perception.district - 4).coerceIn(-100, 100),
                        city = (next.perception.city - 2).coerceIn(-100, 100)
                    ),
                    drama = next.drama.copy(worldPressure = (next.drama.worldPressure + 5).coerceAtMost(100))
                )
            }
        }
        return next
    }

    private fun generateOpportunities(c: Campaign, state: DeepLifeState): DeepLifeState {
        val pool = mutableListOf<Opportunity>()
        val family = state.relationships.firstOrNull { it.id == "family" && it.alive }
        if (family != null && c.turn % 5 == 0) pool += Opportunity(
            "opp_family_${c.turn}", "Prendre du temps avec ${family.name}", "RELATION", c.turn + 1, 2, family.id,
            ignoredPayload = "${family.name} finit par arrêter d'attendre que tu trouves du temps : la vie avance aussi quand tu n'es pas là."
        )
        if (c.powerRevealed && state.powerEvolution?.strain ?: 0 >= 55) pool += Opportunity(
            "opp_recovery_${c.turn}", "Traiter la surcharge avant la prochaine crise", "RECOVERY", c.turn + 1, 4,
            ignoredPayload = "Tu repousses encore la récupération. La fatigue de ton pouvoir devient une dette, pas un simple inconfort."
        )
        if (c.powerRevealed && c.turn % 7 == 0) pool += Opportunity(
            "opp_city_${c.turn}", "Répondre à une demande du quartier", "INTERVENTION", c.turn + 2, 3, district = c.district,
            ignoredPayload = "Le quartier trouve une autre solution sans toi. Ta réputation n'est pas la même chose que ta présence."
        )
        if (state.identityEvidence.size >= 3 && c.turn % 6 == 0) pool += Opportunity(
            "opp_identity_${c.turn}", "Brouiller une piste sur ton identité", "INVESTIGATION", c.turn + 1, 4,
            personId = "journalist", ignoredPayload = "Une piste que tu aurais pu brouiller reste dans le dossier. Quelqu'un continue de recouper les détails."
        )
        return state.copy(opportunities = (state.opportunities + pool).distinctBy { it.id }.filter { c.turn <= it.expiresTurn }.takeLast(10))
    }

    private fun evolvePower(c: Campaign, event: EventNode, choice: Choice, state: DeepLifeState, echo: MutableList<String>): DeepLifeState {
        val p = state.powerEvolution ?: return state
        if (!c.powerRevealed) return state
        val use = (choice.power + if (choice.approach == "ASCEND") 2 else 0 + if (event.category.contains("POUVOIR", true)) 2 else 0).coerceAtLeast(0)
        if (use == 0) return state
        val weaknessExtra = when (c.weakness) {
            "Fatigue extrême", "Surcharge" -> 4
            "Concentration", "Instabilité émotionnelle" -> 2
            else -> 1
        }
        var next = p.copy(
            mastery = (p.mastery + if (choice.approach in setOf("ORDER", "TRUTH")) 1 else 0).coerceAtMost(100),
            strain = (p.strain + use * 3 + weaknessExtra).coerceIn(0, 100)
        )
        val branch = when {
            c.age >= 45 && next.mastery >= 75 -> "Maîtrise vétérane"
            c.age >= 30 && next.mastery >= 60 -> "Technique signature"
            c.age >= 22 && next.mastery >= 45 -> "Spécialisation"
            else -> next.branch
        }
        if (branch != next.branch) {
            next = next.copy(branch = branch)
            echo += "Ton pouvoir entre dans une nouvelle phase : $branch. Il ressemble de moins en moins à la manifestation brute de tes 18 ans."
        }
        return state.copy(powerEvolution = next)
    }

    private fun rebalanceDrama(c: Campaign, state: DeepLifeState): DeepLifeState {
        val d = state.drama
        val ageRecovery = if (c.age >= 60) 1 else 2
        return state.copy(drama = d.copy(
            tension = (d.tension - ageRecovery).coerceIn(0, 100),
            recoveryNeed = (d.recoveryNeed + state.injuries.count { it.recovery < 70 }).coerceIn(0, 100),
            relationshipPressure = state.relationships.count { it.phase in setOf(RelationshipPhase.HURT, RelationshipPhase.DISTANT, RelationshipPhase.RIVAL) }
                .times(8).coerceIn(0, 100)
        ))
    }

    private fun techniqueUnlock(c: Campaign, p: PowerEvolution, gain: Int): String? {
        if (gain <= 0) return null
        val thresholds = listOf(40, 55, 70, 85)
        val crossed = thresholds.firstOrNull { p.mastery < it && p.mastery + gain >= it } ?: return null
        return when (p.architecture) {
            PowerArchitecture.PROJECTOR -> when (crossed) { 40 -> "Tir focalisé"; 55 -> "Zone contrôlée"; 70 -> "Déviation"; else -> "Décharge signature" }
            PowerArchitecture.MENTAL -> when (crossed) { 40 -> "Lecture ciblée"; 55 -> "Écran mental"; 70 -> "Projection émotionnelle"; else -> "Réseau psychique" }
            PowerArchitecture.BODY -> when (crossed) { 40 -> "Ancrage"; 55 -> "Impact contrôlé"; 70 -> "Récupération active"; else -> "Forme de pointe" }
            PowerArchitecture.MOBILITY -> when (crossed) { 40 -> "Extraction rapide"; 55 -> "Trajectoire impossible"; 70 -> "Transport assisté"; else -> "Mouvement signature" }
            PowerArchitecture.MATTER -> when (crossed) { 40 -> "Façonnage fin"; 55 -> "Renforcement"; 70 -> "Construction rapide"; else -> "Architecture instantanée" }
            PowerArchitecture.TECH -> when (crossed) { 40 -> "Diagnostic tactique"; 55 -> "Drones coordonnés"; 70 -> "Contre-mesures"; else -> "Système signature" }
            PowerArchitecture.OCCULT -> when (crossed) { 40 -> "Sceau stable"; 55 -> "Rituel court"; 70 -> "Protection liée"; else -> "Invocation signature" }
            PowerArchitecture.COSMIC -> when (crossed) { 40 -> "Courbure locale"; 55 -> "Champ stabilisé"; 70 -> "Ancrage spatial"; else -> "Phénomène signature" }
            PowerArchitecture.ADAPTIVE -> when (crossed) { 40 -> "Réponse ciblée"; 55 -> "Mémoire corporelle"; 70 -> "Mutation contrôlée"; else -> "Adaptation signature" }
        } + " — ${c.powerFamily}"
    }

    private fun opportunityMatches(o: Opportunity, card: AnnualActionCard): Boolean = when (o.category) {
        "RELATION" -> card.category == AnnualActionCategory.RELATION
        "RECOVERY" -> card.category == AnnualActionCategory.RECOVERY
        "INTERVENTION" -> card.category == AnnualActionCategory.INTERVENTION
        "INVESTIGATION" -> card.category == AnnualActionCategory.INVESTIGATION
        else -> false
    }

    private fun deferredText(c: Campaign, event: EventNode, choice: Choice): String = when (choice.approach) {
        "CARE" -> "Des années après « ${event.title} », quelqu'un que tu avais choisi de protéger revient dans ton histoire. Ce que tu avais considéré comme une petite décision ne l'était pas pour cette personne."
        "ORDER" -> "La méthode imposée lors de « ${event.title} » est devenue une référence pour d'autres. Une décision tactique s'est transformée en précédent."
        "TRUTH" -> "Un détail découvert lors de « ${event.title} » refait surface. L'information que tu avais refusé d'ignorer n'avait pas encore livré toute sa portée."
        "ASCEND" -> "La démonstration de puissance de « ${event.title} » a créé un imitateur — ou un adversaire — qui a eu le temps d'apprendre de toi."
        else -> "Une conséquence ancienne de « ${event.title} » revient au moment où tu pensais cette histoire terminée."
    }
}
