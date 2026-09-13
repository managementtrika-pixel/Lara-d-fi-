package com.metahumanlegacy.game

internal object LifeSimulationDirector {
    fun bootstrap(c: Campaign, deep: DeepLifeState): LifeSimulationState {
        val relationships = deep.relationships.map {
            RelationshipLifeState(
                personId = it.id,
                closeness = initialCloseness(it),
                secretKnowledge = if (it.knowsIdentity) SecretKnowledge.KNOWS else SecretKnowledge.UNAWARE
            )
        }
        val districtIds = listOf("quartier", "centre", "industriel", "residentiel", "peripherie")
        val basePower = PowerRulesState(
            control = if (c.powerRevealed) c.control.coerceAtLeast(10) else 10,
            precision = if (c.powerRevealed) (c.control / 2).coerceAtLeast(10) else 10
        )
        return LifeSimulationState(
            civil = CivilLifeState(
                employment = if (c.age < 18) EmploymentStatus.STUDENT else EmploymentStatus.UNEMPLOYED,
                jobTitle = if (c.age < 18) "Élève" else "Sans emploi",
                freeMoments = annualMoments(c.age)
            ),
            relationshipLives = relationships,
            districts = districtIds.map { DistrictLifeState(it) },
            powerRules = basePower.copy(techniques = LifePowerTechniqueCatalog.seeded(c, basePower.techniques)),
            calendarYear = c.age
        )
    }

    /** Keep one persistent simulation while refreshing only genuinely annual resources. */
    fun synced(c: Campaign, deep: DeepLifeState, state: LifeSimulationState): LifeSimulationState {
        val yearChanged = state.calendarYear != c.age
        val migratingLegacyRelationships = state.schemaVersion < 2
        val existingById = state.relationshipLives.associateBy { it.personId }
        val relationships = deep.relationships.filter { it.alive }.map { person ->
            val existing = existingById[person.id] ?: RelationshipLifeState(person.id, closeness = initialCloseness(person))
            existing.copy(
                closeness = if (migratingLegacyRelationships) maxOf(existing.closeness, initialCloseness(person)) else existing.closeness,
                secretKnowledge = when {
                    person.knowsIdentity -> SecretKnowledge.KNOWS
                    existing.secretKnowledge == SecretKnowledge.KNOWS -> SecretKnowledge.KNOWS
                    else -> existing.secretKnowledge
                },
                availability = if (yearChanged) (existing.availability + 20).coerceAtMost(100) else existing.availability
            )
        }
        val seededTechniques = LifePowerTechniqueCatalog.seeded(c, state.powerRules.techniques)
        val power = state.powerRules.copy(
            control = if (c.powerRevealed) maxOf(state.powerRules.control, c.control) else state.powerRules.control,
            techniques = if (yearChanged) LifePowerTechniqueCatalog.tickCooldowns(seededTechniques) else seededTechniques
        )
        return state.copy(
            schemaVersion = 2,
            civil = if (yearChanged) state.civil.copy(freeMoments = annualMoments(c.age)) else state.civil,
            relationshipLives = relationships,
            powerRules = power,
            calendarYear = c.age
        )
    }

    fun mergedIntoDeep(deep: DeepLifeState, simulation: LifeSimulationState): DeepLifeState {
        val lifeById = simulation.relationshipLives.associateBy { it.personId }
        val relationships = deep.relationships.map { person ->
            val life = lifeById[person.id] ?: return@map person
            val knows = life.secretKnowledge in setOf(SecretKnowledge.KNOWS, SecretKnowledge.PROTECTS, SecretKnowledge.THREATENS)
            val phase = when {
                life.closeness >= 75 -> RelationshipPhase.TRUSTED
                life.closeness >= 50 -> RelationshipPhase.CLOSE
                life.closeness >= 25 && person.phase in setOf(RelationshipPhase.STRANGER, RelationshipPhase.ACQUAINTANCE, RelationshipPhase.FRIEND) -> RelationshipPhase.FRIEND
                life.closeness <= -20 -> RelationshipPhase.DISTANT
                else -> person.phase
            }
            val trust = when {
                life.closeness >= 75 -> maxOf(person.trust, 82)
                life.closeness >= 50 -> maxOf(person.trust, 72)
                life.closeness >= 25 -> maxOf(person.trust, 60)
                life.closeness <= -20 -> minOf(person.trust, 35)
                else -> person.trust
            }
            val affection = when {
                life.closeness >= 75 -> maxOf(person.affection, 78)
                life.closeness >= 50 -> maxOf(person.affection, 68)
                life.closeness >= 25 -> maxOf(person.affection, 58)
                life.closeness <= -20 -> minOf(person.affection, 35)
                else -> person.affection
            }
            person.copy(
                phase = phase,
                trust = trust,
                affection = affection,
                knowsIdentity = person.knowsIdentity || knows
            )
        }
        val techniqueNames = simulation.powerRules.techniques.filter { it.unlocked }.map { it.name }
        val evolvedPower = deep.powerEvolution?.let {
            it.copy(
                mastery = maxOf(it.mastery, simulation.powerRules.control).coerceAtMost(100),
                strain = maxOf(it.strain, simulation.powerRules.overload).coerceAtMost(100),
                unlockedTechniques = (it.unlockedTechniques + techniqueNames.map { name -> "$name — ${it.manifestation}" }).distinct()
            )
        }
        return deep.copy(lifeSimulation = simulation, relationships = relationships, powerEvolution = evolvedPower)
    }

    fun availableActions(c: Campaign, state: LifeSimulationState): List<LifeAction> {
        val civil = mutableListOf<LifeAction>()
        if (c.age >= 16 && state.civil.employment != EmploymentStatus.RETIRED) civil += LifeAction(LifeActionType.WORK, label = "Travailler")
        if (c.age <= 30) civil += LifeAction(LifeActionType.STUDY, label = "Étudier")
        civil += LifeAction(LifeActionType.REST, label = "Récupérer")
        if (c.age >= 18) civil += LifeAction(LifeActionType.MOVE_HOME, label = "Changer de logement")
        if (c.powerRevealed) {
            civil += LifeAction(LifeActionType.TRAIN_POWER, label = "Entraîner mon pouvoir")
            state.powerRules.techniques.filter { it.unlocked && it.cooldownTurns == 0 }.take(3).forEach { technique ->
                civil += LifeAction(LifeActionType.USE_TECHNIQUE, targetId = technique.id, label = "Utiliser · ${technique.name}")
            }
            civil += LifeAction(LifeActionType.PATROL, targetId = "quartier", label = "Patrouiller")
            civil += LifeAction(LifeActionType.INVESTIGATE, targetId = "quartier", label = "Enquêter")
        }
        state.relationshipLives.filter { it.availability > 0 }.take(4).forEach { rel ->
            civil += LifeAction(LifeActionType.VISIT_PERSON, rel.personId, "Voir ${rel.personId}")
            if (rel.closeness >= 35) civil += LifeAction(LifeActionType.ASK_HELP, rel.personId, "Demander de l'aide")
            if (c.powerRevealed && rel.secretKnowledge == SecretKnowledge.UNAWARE && rel.closeness >= 30) {
                civil += LifeAction(LifeActionType.REVEAL_IDENTITY, rel.personId, "Révéler mon identité")
            }
            if (rel.closeness >= 20) civil += LifeAction(LifeActionType.DISTANCE_PERSON, rel.personId, "Prendre de la distance")
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
                    monthlyIncome = maxOf(spent.monthlyIncome, income), savings = spent.savings + income,
                    careerProgress = (spent.careerProgress + 2).coerceAtMost(100), stress = (spent.stress + 5).coerceAtMost(100)
                )
                result(state.copy(civil = civil), action, "Une journée qui compte", "Tu gagnes de quoi avancer, mais ton travail prend du temps et de l'énergie.")
            }
            LifeActionType.STUDY -> {
                val civil = spent.copy(education = (spent.education + 4).coerceAtMost(100), stress = (spent.stress + 2).coerceAtMost(100))
                result(state.copy(civil = civil), action, "Tu investis dans ta vie civile", "Tes études ouvrent progressivement de meilleures possibilités professionnelles.")
            }
            LifeActionType.REST -> {
                val civil = spent.copy(stress = (spent.stress - 18).coerceAtLeast(0))
                val power = state.powerRules.copy(
                    fatigue = (state.powerRules.fatigue - 20).coerceAtLeast(0),
                    overload = (state.powerRules.overload - 10).coerceAtLeast(0),
                    techniques = LifePowerTechniqueCatalog.tickCooldowns(state.powerRules.techniques)
                )
                result(state.copy(civil = civil, powerRules = power), action, "Tu lèves le pied", "Le corps récupère, la pression retombe et les techniques exigeantes redeviennent disponibles.")
            }
            LifeActionType.TRAIN_POWER -> {
                val p = state.powerRules
                if (p.overload >= 90) return LifeActionResult(state, "Corps en surcharge", "Tu dois récupérer avant de pousser ton pouvoir davantage.")
                val (next, unlocked) = LifePowerTechniqueCatalog.train(c, p)
                val detail = buildString {
                    append("Tu progresses réellement : contrôle ${next.control}, précision ${next.precision}. La fatigue reste un coût réel.")
                    if (unlocked != null) append(" ").append(unlocked)
                }
                result(state.copy(civil = spent, powerRules = next), action, unlocked?.substringBefore('.') ?: "Ton pouvoir devient plus précis", detail)
            }
            LifeActionType.USE_TECHNIQUE -> {
                val techniqueId = action.targetId ?: return LifeActionResult(state, "Technique introuvable", "Cette technique n'existe plus dans ton répertoire actuel.")
                val use = LifePowerTechniqueCatalog.use(c, state.powerRules, techniqueId)
                    ?: return LifeActionResult(state, "Technique indisponible", "Cette technique est verrouillée ou demande encore de la récupération.")
                val districts = state.districts.map { district ->
                    if (district.id != "quartier") district else district.copy(
                        safety = (district.safety + use.districtSafety).coerceAtMost(100),
                        criminalControl = (district.criminalControl - use.districtCrime).coerceAtLeast(0),
                        localTrust = (district.localTrust + 2).coerceAtMost(100),
                        mediaHeat = (district.mediaHeat + use.exposureDelta).coerceAtMost(100)
                    )
                }
                val power = state.powerRules.copy(
                    fatigue = (state.powerRules.fatigue + use.fatigueDelta).coerceAtMost(100),
                    overload = (state.powerRules.overload + use.overloadDelta).coerceAtMost(100),
                    techniques = use.techniques
                )
                val secret = state.secretIdentity.copy(exposure = (state.secretIdentity.exposure + use.exposureDelta).coerceAtMost(100))
                result(state.copy(civil = spent, powerRules = power, districts = districts, secretIdentity = secret), action, use.headline, use.detail)
            }
            LifeActionType.PATROL, LifeActionType.INVESTIGATE -> {
                val id = action.targetId ?: "quartier"
                val districts = state.districts.map { d ->
                    if (d.id != id) d else d.copy(
                        safety = (d.safety + if (action.type == LifeActionType.PATROL) 4 else 1).coerceAtMost(100),
                        criminalControl = (d.criminalControl - if (action.type == LifeActionType.PATROL) 3 else 1).coerceAtLeast(0),
                        localTrust = (d.localTrust + 2).coerceAtMost(100), mediaHeat = (d.mediaHeat + 2).coerceAtMost(100)
                    )
                }
                val secret = state.secretIdentity.copy(exposure = (state.secretIdentity.exposure + 2).coerceAtMost(100))
                result(state.copy(civil = spent, districts = districts, secretIdentity = secret), action, "Le quartier réagit", "Ta présence change progressivement la sécurité, la confiance locale et l'attention portée sur toi.")
            }
            LifeActionType.VISIT_PERSON, LifeActionType.APOLOGIZE, LifeActionType.ASK_HELP,
            LifeActionType.REVEAL_IDENTITY, LifeActionType.DISTANCE_PERSON -> relationshipAction(state.copy(civil = spent), action)
            LifeActionType.MOVE_HOME -> {
                val nextHousing = when (spent.housing) {
                    HousingTier.FAMILY_HOME -> HousingTier.ROOM; HousingTier.ROOM -> HousingTier.STUDIO
                    HousingTier.STUDIO -> HousingTier.APARTMENT; HousingTier.APARTMENT -> HousingTier.HOUSE
                    HousingTier.HOUSE, HousingTier.BASE -> HousingTier.BASE
                }
                val cost = when (nextHousing) {
                    HousingTier.FAMILY_HOME -> 0; HousingTier.ROOM -> 300; HousingTier.STUDIO -> 550
                    HousingTier.APARTMENT -> 850; HousingTier.HOUSE -> 1400; HousingTier.BASE -> 2200
                }
                if (spent.savings < cost && nextHousing != HousingTier.ROOM) return LifeActionResult(state, "Projet trop cher", "Tu n'as pas encore les économies nécessaires pour ce logement.")
                result(state.copy(civil = spent.copy(housing = nextHousing, housingCost = cost, savings = (spent.savings - cost).coerceAtLeast(0))), action, "Tu changes de lieu de vie", "Ton quotidien et ce que les autres peuvent découvrir sur toi changent avec ton logement.")
            }
        }
    }

    private fun relationshipAction(state: LifeSimulationState, action: LifeAction): LifeActionResult {
        val id = action.targetId ?: return LifeActionResult(state, "Personne introuvable", "Cette action demande une personne précise.")
        val target = state.relationshipLives.firstOrNull { it.personId == id }
            ?: return LifeActionResult(state, "Personne introuvable", "Cette personne ne fait plus partie de ta vie actuelle.")
        if (target.availability <= 0) return LifeActionResult(state, "Indisponible", "Cette personne n'a plus de place disponible pour toi cette année.")
        if (action.type == LifeActionType.ASK_HELP && target.closeness < 35) return LifeActionResult(state, "Lien encore fragile", "Vous n'avez pas encore construit assez de confiance pour demander ce type d'aide.")
        if (action.type == LifeActionType.REVEAL_IDENTITY && target.closeness < 30) return LifeActionResult(state, "Trop tôt", "Révéler ton identité à quelqu'un d'aussi peu proche serait un pari énorme.")
        val next = state.relationshipLives.map { rel ->
            if (rel.personId != id) rel else when (action.type) {
                LifeActionType.VISIT_PERSON -> rel.copy(closeness = (rel.closeness + 10).coerceAtMost(100), availability = (rel.availability - 25).coerceAtLeast(0), lastContactTurn = state.calendarYear)
                LifeActionType.APOLOGIZE -> rel.copy(closeness = (rel.closeness + 6).coerceAtMost(100), promises = (rel.promises + "Excuses reçues").takeLast(8), availability = (rel.availability - 15).coerceAtLeast(0), lastContactTurn = state.calendarYear)
                LifeActionType.ASK_HELP -> rel.copy(closeness = (rel.closeness + 3).coerceAtMost(100), promises = (rel.promises + "Aide demandée").takeLast(8), availability = (rel.availability - 30).coerceAtLeast(0), lastContactTurn = state.calendarYear)
                LifeActionType.REVEAL_IDENTITY -> rel.copy(closeness = (rel.closeness + 12).coerceAtMost(100), secretKnowledge = SecretKnowledge.KNOWS, sharedSecrets = (rel.sharedSecrets + "Identité métahumaine").distinct(), availability = (rel.availability - 20).coerceAtLeast(0), lastContactTurn = state.calendarYear)
                LifeActionType.DISTANCE_PERSON -> rel.copy(closeness = (rel.closeness - 35).coerceAtLeast(-40), bond = if (rel.bond == BondStatus.PARTNER) BondStatus.SEPARATED else BondStatus.NONE, availability = 100, lastContactTurn = state.calendarYear)
                else -> rel
            }
        }
        val knownBy = if (action.type == LifeActionType.REVEAL_IDENTITY) state.secretIdentity.knownBy + (id to SecretKnowledge.KNOWS) else state.secretIdentity.knownBy
        val text = when (action.type) {
            LifeActionType.REVEAL_IDENTITY -> "Tu confies quelque chose qui ne pourra plus être repris. Ce niveau de confiance change durablement votre relation."
            LifeActionType.APOLOGIZE -> "Tu affrontes ce qui s'est passé au lieu de laisser le silence décider."
            LifeActionType.ASK_HELP -> "Tu acceptes de ne pas tout porter seul·e, et cette confiance renforce le lien."
            LifeActionType.DISTANCE_PERSON -> "Tu crées volontairement de la distance. Le lien recule réellement au lieu de rester figé dans les statistiques."
            else -> "Tu consacres du temps à cette personne. Votre proximité augmente et peut ouvrir des choix plus intimes plus tard."
        }
        return result(state.copy(relationshipLives = next, secretIdentity = state.secretIdentity.copy(knownBy = knownBy)), action, "Un moment personnel", text)
    }

    private fun initialCloseness(person: DeepRelationship): Int = (((person.trust + person.affection) / 2) - 40).coerceIn(0, 60)

    private fun annualMoments(age: Int): Int = when { age < 12 -> 2; age < 18 -> 3; age < 65 -> 4; else -> 3 }

    private fun result(state: LifeSimulationState, action: LifeAction, headline: String, detail: String): LifeActionResult =
        LifeActionResult(state.copy(actionLog = (state.actionLog + "${state.calendarYear}: ${action.label}").takeLast(80)), headline, detail)
}
