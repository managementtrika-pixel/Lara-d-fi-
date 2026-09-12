package com.metahumanlegacy.game

internal object LifeSimulationDirector {
    fun bootstrap(c: Campaign, deep: DeepLifeState): LifeSimulationState {
        val relationships = deep.relationships.map {
            RelationshipLifeState(
                personId = it.id,
                secretKnowledge = if (it.knowsIdentity) SecretKnowledge.KNOWS else SecretKnowledge.UNAWARE
            )
        }
        val districtIds = listOf("quartier", "centre", "industriel", "residentiel", "peripherie")
        return LifeSimulationState(
            civil = CivilLifeState(
                employment = if (c.age < 18) EmploymentStatus.STUDENT else EmploymentStatus.UNEMPLOYED,
                jobTitle = if (c.age < 18) "Élève" else "Sans emploi",
                freeMoments = annualMoments(c.age)
            ),
            relationshipLives = relationships,
            districts = districtIds.map { DistrictLifeState(it) },
            calendarYear = c.age
        )
    }

    /** Keep one persistent simulation while refreshing only genuinely annual resources. */
    fun synced(c: Campaign, deep: DeepLifeState, state: LifeSimulationState): LifeSimulationState {
        val yearChanged = state.calendarYear != c.age
        val existingById = state.relationshipLives.associateBy { it.personId }
        val relationships = deep.relationships.filter { it.alive }.map { person ->
            val existing = existingById[person.id] ?: RelationshipLifeState(person.id)
            existing.copy(
                secretKnowledge = when {
                    person.knowsIdentity -> SecretKnowledge.KNOWS
                    existing.secretKnowledge == SecretKnowledge.KNOWS -> SecretKnowledge.KNOWS
                    else -> existing.secretKnowledge
                },
                availability = if (yearChanged) (existing.availability + 20).coerceAtMost(100) else existing.availability
            )
        }
        return state.copy(
            civil = if (yearChanged) state.civil.copy(freeMoments = annualMoments(c.age)) else state.civil,
            relationshipLives = relationships,
            calendarYear = c.age
        )
    }

    fun mergedIntoDeep(deep: DeepLifeState, simulation: LifeSimulationState): DeepLifeState {
        val knowledge = simulation.relationshipLives.associate { it.personId to it.secretKnowledge }
        val relationships = deep.relationships.map { person ->
            val knows = knowledge[person.id] in setOf(SecretKnowledge.KNOWS, SecretKnowledge.PROTECTS, SecretKnowledge.THREATENS)
            if (knows && !person.knowsIdentity) person.copy(knowsIdentity = true) else person
        }
        return deep.copy(lifeSimulation = simulation, relationships = relationships)
    }

    fun availableActions(c: Campaign, state: LifeSimulationState): List<LifeAction> {
        val civil = mutableListOf<LifeAction>()
        if (c.age >= 16 && state.civil.employment != EmploymentStatus.RETIRED) {
            civil += LifeAction(LifeActionType.WORK, label = "Travailler")
        }
        if (c.age <= 30) civil += LifeAction(LifeActionType.STUDY, label = "Étudier")
        civil += LifeAction(LifeActionType.REST, label = "Récupérer")
        if (c.age >= 18) civil += LifeAction(LifeActionType.MOVE_HOME, label = "Changer de logement")
        if (c.powerRevealed) {
            civil += LifeAction(LifeActionType.TRAIN_POWER, label = "Entraîner mon pouvoir")
            civil += LifeAction(LifeActionType.PATROL, targetId = "quartier", label = "Patrouiller")
            civil += LifeAction(LifeActionType.INVESTIGATE, targetId = "quartier", label = "Enquêter")
        }
        state.relationshipLives.filter { it.availability > 0 }.take(4).forEach { rel ->
            civil += LifeAction(LifeActionType.VISIT_PERSON, rel.personId, "Voir ${rel.personId}")
            if (c.powerRevealed && rel.secretKnowledge == SecretKnowledge.UNAWARE) {
                civil += LifeAction(LifeActionType.REVEAL_IDENTITY, rel.personId, "Révéler mon identité")
            }
        }
        return civil
    }

    fun perform(c: Campaign, state: LifeSimulationState, action: LifeAction): LifeActionResult {
        if (state.civil.freeMoments <= 0) return LifeActionResult(state, "Plus de temps", "Cette année est déjà remplie. Tes choix de temps ont un coût.")
        val spent = state.civil.copy(freeMoments = state.civil.freeMoments - 1)
        return when (action.type) {
            LifeActionType.WORK -> {
                val income = 900 + spent.careerProgress * 35
                val civil = spent.copy(
                    employment = EmploymentStatus.EMPLOYED,
                    jobTitle = if (spent.jobTitle in setOf("Élève", "Sans emploi")) "Employé·e" else spent.jobTitle,
                    monthlyIncome = maxOf(spent.monthlyIncome, income),
                    savings = spent.savings + income,
                    careerProgress = (spent.careerProgress + 2).coerceAtMost(100),
                    stress = (spent.stress + 5).coerceAtMost(100)
                )
                result(state.copy(civil = civil), action, "Une journée qui compte", "Tu gagnes de quoi avancer, mais ton travail prend du temps et de l'énergie.")
            }
            LifeActionType.STUDY -> {
                val civil = spent.copy(education = (spent.education + 4).coerceAtMost(100), stress = (spent.stress + 2).coerceAtMost(100))
                result(state.copy(civil = civil), action, "Tu investis dans ta vie civile", "Tes études ouvrent progressivement de meilleures possibilités professionnelles.")
            }
            LifeActionType.REST -> {
                val civil = spent.copy(stress = (spent.stress - 18).coerceAtLeast(0))
                val power = state.powerRules.copy(fatigue = (state.powerRules.fatigue - 20).coerceAtLeast(0), overload = (state.powerRules.overload - 10).coerceAtLeast(0))
                result(state.copy(civil = civil, powerRules = power), action, "Tu lèves le pied", "Le corps récupère et la pression retombe.")
            }
            LifeActionType.TRAIN_POWER -> {
                val p = state.powerRules
                if (p.overload >= 90) {
                    return LifeActionResult(state, "Corps en surcharge", "Tu dois récupérer avant de pousser ton pouvoir davantage.")
                }
                val next = p.copy(
                    control = (p.control + 3).coerceAtMost(100),
                    precision = (p.precision + 2).coerceAtMost(100),
                    fatigue = (p.fatigue + 9).coerceAtMost(100),
                    overload = (p.overload + if (p.fatigue > 70) 8 else 2).coerceAtMost(100)
                )
                result(state.copy(civil = spent, powerRules = next), action, "Ton pouvoir devient plus précis", "Tu progresses réellement, au prix d'une fatigue qui peut limiter tes prochains choix.")
            }
            LifeActionType.PATROL, LifeActionType.INVESTIGATE -> {
                val id = action.targetId ?: "quartier"
                val districts = state.districts.map { d ->
                    if (d.id != id) d else d.copy(
                        safety = (d.safety + if (action.type == LifeActionType.PATROL) 4 else 1).coerceAtMost(100),
                        criminalControl = (d.criminalControl - if (action.type == LifeActionType.PATROL) 3 else 1).coerceAtLeast(0),
                        localTrust = (d.localTrust + 2).coerceAtMost(100),
                        mediaHeat = (d.mediaHeat + 2).coerceAtMost(100)
                    )
                }
                val secret = state.secretIdentity.copy(exposure = (state.secretIdentity.exposure + 2).coerceAtMost(100))
                result(state.copy(civil = spent, districts = districts, secretIdentity = secret), action, "Le quartier réagit", "Ta présence change progressivement la sécurité, la confiance locale et l'attention portée sur toi.")
            }
            LifeActionType.VISIT_PERSON, LifeActionType.APOLOGIZE, LifeActionType.ASK_HELP,
            LifeActionType.REVEAL_IDENTITY, LifeActionType.DISTANCE_PERSON -> relationshipAction(state.copy(civil = spent), action)
            LifeActionType.MOVE_HOME -> {
                val nextHousing = when (spent.housing) {
                    HousingTier.FAMILY_HOME -> HousingTier.ROOM
                    HousingTier.ROOM -> HousingTier.STUDIO
                    HousingTier.STUDIO -> HousingTier.APARTMENT
                    HousingTier.APARTMENT -> HousingTier.HOUSE
                    HousingTier.HOUSE, HousingTier.BASE -> HousingTier.BASE
                }
                val cost = when (nextHousing) {
                    HousingTier.FAMILY_HOME -> 0
                    HousingTier.ROOM -> 300
                    HousingTier.STUDIO -> 550
                    HousingTier.APARTMENT -> 850
                    HousingTier.HOUSE -> 1400
                    HousingTier.BASE -> 2200
                }
                if (spent.savings < cost && nextHousing != HousingTier.ROOM) {
                    return LifeActionResult(state, "Projet trop cher", "Tu n'as pas encore les économies nécessaires pour ce logement.")
                }
                result(
                    state.copy(civil = spent.copy(housing = nextHousing, housingCost = cost, savings = (spent.savings - cost).coerceAtLeast(0))),
                    action,
                    "Tu changes de lieu de vie",
                    "Ton quotidien et ce que les autres peuvent découvrir sur toi changent avec ton logement."
                )
            }
        }
    }

    private fun relationshipAction(state: LifeSimulationState, action: LifeAction): LifeActionResult {
        val id = action.targetId ?: return LifeActionResult(state, "Personne introuvable", "Cette action demande une personne précise.")
        val target = state.relationshipLives.firstOrNull { it.personId == id }
            ?: return LifeActionResult(state, "Personne introuvable", "Cette personne ne fait plus partie de ta vie actuelle.")
        if (target.availability <= 0) return LifeActionResult(state, "Indisponible", "Cette personne n'a plus de place disponible pour toi cette année.")
        val next = state.relationshipLives.map { rel ->
            if (rel.personId != id) rel else when (action.type) {
                LifeActionType.VISIT_PERSON -> rel.copy(availability = (rel.availability - 25).coerceAtLeast(0), lastContactTurn = state.calendarYear)
                LifeActionType.APOLOGIZE -> rel.copy(promises = (rel.promises + "Excuses reçues").takeLast(8), availability = (rel.availability - 15).coerceAtLeast(0), lastContactTurn = state.calendarYear)
                LifeActionType.ASK_HELP -> rel.copy(promises = (rel.promises + "Aide demandée").takeLast(8), availability = (rel.availability - 30).coerceAtLeast(0), lastContactTurn = state.calendarYear)
                LifeActionType.REVEAL_IDENTITY -> rel.copy(secretKnowledge = SecretKnowledge.KNOWS, sharedSecrets = (rel.sharedSecrets + "Identité métahumaine").distinct(), availability = (rel.availability - 20).coerceAtLeast(0))
                LifeActionType.DISTANCE_PERSON -> rel.copy(bond = if (rel.bond == BondStatus.PARTNER) BondStatus.SEPARATED else BondStatus.NONE, availability = 100)
                else -> rel
            }
        }
        val knownBy = if (action.type == LifeActionType.REVEAL_IDENTITY) state.secretIdentity.knownBy + (id to SecretKnowledge.KNOWS) else state.secretIdentity.knownBy
        val text = when (action.type) {
            LifeActionType.REVEAL_IDENTITY -> "Tu confies quelque chose qui ne pourra plus être repris."
            LifeActionType.APOLOGIZE -> "Tu affrontes ce qui s'est passé au lieu de laisser le silence décider."
            LifeActionType.ASK_HELP -> "Tu acceptes de ne pas tout porter seul·e."
            LifeActionType.DISTANCE_PERSON -> "Tu crées volontairement de la distance, avec les conséquences que cela implique."
            else -> "Tu consacres du temps à cette personne. La relation existe aussi entre les crises."
        }
        return result(state.copy(relationshipLives = next, secretIdentity = state.secretIdentity.copy(knownBy = knownBy)), action, "Un moment personnel", text)
    }

    private fun annualMoments(age: Int): Int = when {
        age < 12 -> 2
        age < 18 -> 3
        age < 65 -> 4
        else -> 3
    }

    private fun result(state: LifeSimulationState, action: LifeAction, headline: String, detail: String): LifeActionResult =
        LifeActionResult(state.copy(actionLog = (state.actionLog + "${state.calendarYear}: ${action.label}").takeLast(80)), headline, detail)
}
