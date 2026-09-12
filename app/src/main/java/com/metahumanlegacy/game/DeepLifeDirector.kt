package com.metahumanlegacy.game

internal object DeepLifeDirector {
    fun bootstrap(c: Campaign, u: UltimateState): DeepLifeState {
        val relations = u.relations.mapIndexed { index, r ->
            val core = when (r.id) {
                "family" -> PersonCore(setOf("LOYAUTÉ", "SÉCURITÉ"), setOf("TE_PERDRE"), setOf("FAMILLE_UNIE"), setOf("ABANDON"))
                "friend" -> PersonCore(setOf("LOYAUTÉ", "HONNÊTETÉ"), setOf("ÊTRE_UN_POIDS"), setOf("EXISTER_PAR_SOI_MÊME"), setOf("MANIPULATION"))
                "journalist" -> PersonCore(setOf("VÉRITÉ", "INDÉPENDANCE"), setOf("ÊTRE_MANIPULÉ"), setOf("RÉVÉLER_CE_QUI_COMPTE"), setOf("CENSURE"))
                "rival" -> PersonCore(setOf("MÉRITE", "RECONNAISSANCE"), setOf("ÊTRE_OUBLIÉ"), setOf("TE_DÉPASSER"), setOf("HUMILIATION"))
                "mentor" -> PersonCore(setOf("DISCIPLINE", "TRANSMISSION"), setOf("ÉCHOUER_À_TRANSMETTRE"), setOf("LAISSER_UN_HÉRITAGE"), setOf("IRRESPONSABILITÉ"))
                else -> PersonCore(setOf("AUTONOMIE", "SOLIDARITÉ"), setOf("ISOLEMENT"), setOf("TROUVER_SA_PLACE"))
            }
            DeepRelationship(
                id = r.id,
                name = r.name,
                role = r.role,
                core = core,
                phase = when (r.id) {
                    "family" -> RelationshipPhase.CLOSE
                    "friend" -> RelationshipPhase.FRIEND
                    "mentor" -> RelationshipPhase.MENTOR
                    "rival" -> RelationshipPhase.RIVAL
                    else -> RelationshipPhase.ACQUAINTANCE
                },
                trust = r.trust,
                affection = r.affection,
                fear = r.fear,
                admiration = r.admiration,
                grudge = r.grudge,
                dependence = r.dependence,
                knowsIdentity = r.knowsIdentity,
                memories = if (index < 2) listOf(
                    CharacterMemory(
                        id = "origin_${r.id}", turn = 0, age = 8, personId = r.id,
                        eventId = "ORIGIN", summary = "${r.name} faisait déjà partie de ta vie avant le masque.",
                        emotion = "ATTACHEMENT", weight = 4, tags = setOf("ORIGIN", "RELATION")
                    )
                ) else emptyList()
            )
        }
        return DeepLifeState(seed = c.seed, relationships = relations, personality = initialPersonality(c))
    }

    fun afterChoice(before: Campaign, after: Campaign, event: EventNode, choice: Choice, state: DeepLifeState): DeepLifeState {
        val memoryWeight = (event.stakes + choice.impact + choice.risk / 2).coerceIn(1, 10)
        val memory = CharacterMemory(
            id = "${before.turn}_${event.id}_${choice.label.hashCode()}",
            turn = before.turn,
            age = before.age,
            eventId = event.id,
            summary = "${event.title} — ${choice.label}",
            emotion = emotion(choice),
            weight = memoryWeight,
            tags = setOfNotNull(choice.approach.takeIf { it.isNotBlank() }, event.category.takeIf { it.isNotBlank() }, choice.flag)
        )
        val memories = (state.memories + memory).sortedByDescending { it.weight }.take(120)
        val perception = state.perception.copy(
            district = clamp(state.perception.district + choice.opinion),
            city = clamp(state.perception.city + choice.opinion / 2 + choice.prestige / 2),
            national = clamp(state.perception.national + if (after.scope >= Scope.COUNTRY) choice.opinion else 0),
            government = clamp(state.perception.government + (after.governmentStanding - before.governmentStanding)),
            civilians = clamp(state.perception.civilians + choice.moral + choice.opinion),
            criminalFear = clamp(state.perception.criminalFear + choice.fear + if (choice.approach == "ASCEND") 1 else 0),
            civilianFear = clamp(state.perception.civilianFear + (choice.fear / 3) + if (choice.healthDelta < -5) 1 else 0),
            governmentFear = clamp(state.perception.governmentFear + if (choice.power >= 3 || choice.risk >= 4) 1 else 0)
        )
        val personality = state.personality.toMutableMap()
        fun shift(key: String, amount: Int) { personality[key] = clampTrait((personality[key] ?: 0) + amount) }
        when (choice.approach) {
            "CARE" -> { shift("EMPATHIQUE", 2); shift("PROTECTEUR", 1) }
            "ORDER" -> { shift("DISCIPLINÉ", 2); shift("AUTORITAIRE", 1) }
            "TRUTH" -> { shift("CURIEUX", 2); shift("SECRET", 1) }
            "ASCEND" -> { shift("AMBITIEUX", 2); shift("TÉMÉRAIRE", 1) }
        }
        if (choice.risk >= 4) shift("TÉMÉRAIRE", 1)
        if (choice.relationDelta >= 2) shift("LOYAL", 1)

        val drama = state.drama.copy(
            tension = (state.drama.tension + event.stakes * 4 + choice.risk * 2 - if (event.kind == "QUIET") 18 else 0).coerceIn(0, 100),
            recoveryNeed = (state.drama.recoveryNeed + event.stakes * 2 + choice.risk - if (event.kind == "QUIET") 12 else 0).coerceIn(0, 100),
            personalPressure = (state.drama.personalPressure + if (choice.relationDelta < 0) 8 else 0).coerceIn(0, 100),
            worldPressure = (state.drama.worldPressure + if (event.stakes >= 4) 7 else 1).coerceIn(0, 100),
            recentMajorEvents = (state.drama.recentMajorEvents + event.id).takeLast(8)
        )
        return state.copy(memories = memories, perception = perception, personality = personality, drama = drama)
    }

    fun architectureFor(power: String): PowerArchitecture = when {
        power.contains("Télépath", true) || power.contains("Illusion", true) || power.contains("mentale", true) -> PowerArchitecture.MENTAL
        power.contains("Vitesse", true) || power.contains("Vol", true) || power.contains("Portail", true) -> PowerArchitecture.MOBILITY
        power.contains("Force", true) || power.contains("Résistance", true) || power.contains("Régén", true) -> PowerArchitecture.BODY
        power.contains("Techn", true) || power.contains("Armure", true) || power.contains("Drone", true) || power.contains("Interface", true) -> PowerArchitecture.TECH
        power.contains("Magie", true) || power.contains("Malédiction", true) || power.contains("astral", true) || power.contains("Rêve", true) -> PowerArchitecture.OCCULT
        power.contains("cosm", true) || power.contains("Gravité", true) || power.contains("Espace", true) -> PowerArchitecture.COSMIC
        power.contains("Matière", true) || power.contains("Cristal", true) || power.contains("Métal", true) || power.contains("Transmutation", true) -> PowerArchitecture.MATTER
        power.contains("Adapt", true) || power.contains("Métamorphose", true) || power.contains("Densité", true) -> PowerArchitecture.ADAPTIVE
        else -> PowerArchitecture.PROJECTOR
    }

    fun revealPower(c: Campaign, state: DeepLifeState): DeepLifeState {
        if (!c.powerResolved || state.powerEvolution != null) return state
        return state.copy(
            powerEvolution = PowerEvolution(
                architecture = architectureFor(c.powerFamily), manifestation = c.powerFamily,
                mastery = c.control, strain = 0
            )
        )
    }

    fun awakeningMemoryLines(state: DeepLifeState, limit: Int = 4): List<String> = state.memories
        .filter { it.age < 18 }
        .sortedWith(compareByDescending<CharacterMemory> { it.weight }.thenBy { it.turn })
        .take(limit)
        .sortedBy { it.age }
        .map { "${it.age} ans — ${it.summary.substringAfter("— ", it.summary)}" }

    private fun initialPersonality(c: Campaign): Map<String, Int> = buildMap {
        put("EMPATHIQUE", if (c.temperament.contains("Empath", true)) 20 else 0)
        put("PRUDENT", if (c.temperament.contains("Prudent", true)) 20 else 0)
        put("TÉMÉRAIRE", if (c.temperament.contains("Impuls", true)) 20 else 0)
        put("CURIEUX", if (c.temperament.contains("Curieux", true)) 20 else 0)
        put("LOYAL", 10)
        put("PROTECTEUR", 10)
    }

    private fun emotion(choice: Choice): String = when {
        choice.relationDelta >= 2 -> "ATTACHEMENT"
        choice.healthDelta <= -5 -> "DOULEUR"
        choice.risk >= 4 -> "PEUR"
        choice.approach == "ASCEND" -> "EXALTATION"
        choice.approach == "TRUTH" -> "DOUTE"
        else -> "DÉTERMINATION"
    }

    private fun clamp(value: Int) = value.coerceIn(-100, 100)
    private fun clampTrait(value: Int) = value.coerceIn(-100, 100)
}
