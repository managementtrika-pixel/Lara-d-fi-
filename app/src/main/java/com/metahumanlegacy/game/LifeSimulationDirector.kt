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
                freeMoments = if (c.age < 18) 2 else 3
            ),
            relationshipLives = relationships,
            districts = districtIds.map { DistrictLifeState(it) },
            calendarYear = c.age
        )
    }

    fun availableActions(c: Campaign, state: LifeSimulationState): List<LifeAction> {
        val civil = mutableListOf<LifeAction>()
        if (c.age >= 16 && state.civil.employment != EmploymentStatus.RETIRED) {
            civil += LifeAction(LifeActionType.WORK, label = "Travailler")
        }
        if (c.age <= 30) civil += LifeAction(LifeActionType.STUDY, label = "Étudier")
        civil += LifeAction(LifeActionType.REST, label = "Récupérer")
        if (c.powerRevealed) {
            civil += LifeAction(LifeActionType.TRAIN_POWER, label = "Entraîner mon pouvoir")
            civil += LifeAction(LifeActionType.PATROL, targetId = "quartier", label = "Patrouiller")
            civil += LifeAction(LifeActionType.INVESTIGATE, targetId = "quartier", label = "Enquêter")
        }
        state.relationshipLives.take(4).forEach { rel ->
            civil += LifeAction(LifeActionType.VISIT_PERSON, rel.personId, "Voir ${rel.personId}")
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
                result(state.copy(civil = spent, districts = districts), action, "Le quartier réagit", "Ta présence change progressivement la sécurité, la confiance locale et l'attention portée sur toi.")
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
                result(state.copy(civil = spent.copy(housing = nextHousing)), action, "Tu changes de lieu de vie", "Ton quotidien et ce que les autres peuvent découvrir sur toi changent avec ton logement.")
            }
        }
    }

    private fun relationshipAction(state: LifeSimulationState, action: LifeAction): LifeActionResult {
        val id = action.targetId ?: return LifeActionResult(state, "Personne introuvable", "Cette action demande une personne précise.")
        val next = state.relationshipLives.map { rel ->
            if (rel.personId != id) rel else when (action.type) {
                LifeActionType.VISIT_PERSON -> rel.copy(availability = (rel.availability - 5).coerceAtLeast(0), lastContactTurn = state.calendarYear)
                LifeActionType.APOLOGIZE -> rel.copy(promises = (rel.promises + "Excuses reçues").takeLast(8), lastContactTurn = state.calendarYear)
                LifeActionType.ASK_HELP -> rel.copy(promises = (rel.promises + "Aide demandée").takeLast(8), lastContactTurn = state.calendarYear)
                LifeActionType.REVEAL_IDENTITY -> rel.copy(secretKnowledge = SecretKnowledge.KNOWS, sharedSecrets = (rel.sharedSecrets + "Identité métahumaine").distinct())
                LifeActionType.DISTANCE_PERSON -> rel.copy(bond = if (rel.bond == BondStatus.PARTNER) BondStatus.SEPARATED else BondStatus.NONE, availability = 100)
                else -> rel
            }
        }
        val text = when (action.type) {
            LifeActionType.REVEAL_IDENTITY -> "Tu confies quelque chose qui ne pourra plus être repris."
            LifeActionType.APOLOGIZE -> "Tu affrontes ce qui s'est passé au lieu de laisser le silence décider."
            LifeActionType.ASK_HELP -> "Tu acceptes de ne pas tout porter seul·e."
            LifeActionType.DISTANCE_PERSON -> "Tu crées volontairement de la distance, avec les conséquences que cela implique."
            else -> "Tu consacres du temps à cette personne. La relation existe aussi entre les crises."
        }
        return result(state.copy(relationshipLives = next), action, "Un moment personnel", text)
    }

    private fun result(state: LifeSimulationState, action: LifeAction, headline: String, detail: String): LifeActionResult =
        LifeActionResult(state.copy(actionLog = (state.actionLog + "${state.calendarYear}: ${action.label}").takeLast(80)), headline, detail)
}
