package com.metahumanlegacy.game

/** Turns identity evidence into persistent, visible and playable pressure in the life simulation. */
internal object IdentityPressureDirector {
    fun sync(c: Campaign, deep: DeepLifeState, state: LifeSimulationState): LifeSimulationState {
        if (!c.powerRevealed) return state

        val knownBy = state.secretIdentity.knownBy.toMutableMap()
        val relationships = state.relationshipLives.map { life ->
            val person = deep.relationships.firstOrNull { it.id == life.personId }
            if (person == null || !person.alive) return@map life

            val priorKnowledge = state.secretIdentity.knownBy[person.id] ?: life.secretKnowledge
            val knowledge = when {
                priorKnowledge == SecretKnowledge.THREATENS -> SecretKnowledge.THREATENS
                priorKnowledge == SecretKnowledge.PROTECTS -> SecretKnowledge.PROTECTS
                person.knowsIdentity && (person.grudge >= 55 || person.trust <= 25) -> SecretKnowledge.THREATENS
                person.knowsIdentity && person.trust >= 70 && person.affection >= 60 -> SecretKnowledge.PROTECTS
                person.knowsIdentity -> SecretKnowledge.KNOWS
                priorKnowledge == SecretKnowledge.SUSPECTS -> SecretKnowledge.SUSPECTS
                else -> SecretKnowledge.UNAWARE
            }
            if (knowledge != SecretKnowledge.UNAWARE) knownBy[person.id] = knowledge
            life.copy(secretKnowledge = knowledge)
        }

        val evidence = deep.identityEvidence.distinctBy { it.id }
        val strength = evidence.sumOf { it.strength }.coerceAtMost(400)
        val kinds = evidence.map { it.kind }.toSet()
        val journalistKnows = deep.relationships.any { it.id == "journalist" && it.knowsIdentity && it.alive }
        val rumors = buildList {
            if (c.identityExposure >= 25 || strength >= 40) {
                add("Des témoins recoupent les heures et les secteurs où tu apparais.")
            }
            if (c.identityExposure >= 45 || "VIDÉO" in kinds || "BLESSURE" in kinds) {
                add("Des images et détails physiques commencent à être comparés entre eux.")
            }
            if (c.identityExposure >= 65 || strength >= 120 || "SIGNATURE_POUVOIR" in kinds) {
                add("Le public cherche désormais un lien concret entre ta vie civile et tes interventions.")
            }
            if (c.identityExposure >= 80 || strength >= 220 || journalistKnows) {
                add("Des noms circulent. Quelqu'un pourrait bientôt transformer les soupçons en histoire publiable.")
            }
        }.distinct().takeLast(4)

        val pressure = maxOf(c.identityExposure, strength / 4).coerceIn(0, 100)
        val districts = state.districts.map { district ->
            if (district.id != "quartier") district else district.copy(
                mediaHeat = maxOf(district.mediaHeat, pressure / 2).coerceIn(0, 100)
            )
        }

        return state.copy(
            relationshipLives = relationships,
            districts = districts,
            secretIdentity = state.secretIdentity.copy(
                exposure = maxOf(state.secretIdentity.exposure, c.identityExposure).coerceIn(0, 100),
                activeRumors = rumors,
                knownBy = knownBy,
                evidenceIds = evidence.map { it.id }.takeLast(40)
            )
        )
    }

    fun knownCount(state: LifeSimulationState): Int = state.secretIdentity.knownBy.values.count {
        it in setOf(SecretKnowledge.KNOWS, SecretKnowledge.PROTECTS, SecretKnowledge.THREATENS)
    }
}
