package com.metahumanlegacy.game

/**
 * V2 domain foundation. These models deliberately live beside Campaign/UltimateState first:
 * migrations can be introduced incrementally without invalidating 1.2 saves.
 */
internal data class CharacterMemory(
    val id: String,
    val turn: Int,
    val age: Int,
    val personId: String? = null,
    val eventId: String,
    val summary: String,
    val emotion: String = "",
    val weight: Int = 1,
    val tags: Set<String> = emptySet()
)

internal data class PersonCore(
    val values: Set<String> = emptySet(),
    val fears: Set<String> = emptySet(),
    val ambitions: Set<String> = emptySet(),
    val boundaries: Set<String> = emptySet(),
    val secrets: Set<String> = emptySet()
)

internal enum class RelationshipPhase {
    STRANGER, ACQUAINTANCE, FRIEND, CLOSE, TRUSTED, HURT, DISTANT, RIVAL, MENTOR, EQUAL, PROTEGE, SUCCESSOR
}

internal data class DeepRelationship(
    val id: String,
    val name: String,
    val role: String,
    val core: PersonCore = PersonCore(),
    val phase: RelationshipPhase = RelationshipPhase.ACQUAINTANCE,
    val trust: Int = 50,
    val affection: Int = 50,
    val fear: Int = 0,
    val admiration: Int = 0,
    val grudge: Int = 0,
    val dependence: Int = 0,
    val knowsIdentity: Boolean = false,
    val alive: Boolean = true,
    val memories: List<CharacterMemory> = emptyList()
)

internal data class AudiencePerception(
    val district: Int = 0,
    val city: Int = 0,
    val national: Int = 0,
    val government: Int = 0,
    val police: Int = 0,
    val civilians: Int = 0,
    val metahumans: Int = 0,
    val youth: Int = 0,
    val criminalFear: Int = 0,
    val civilianFear: Int = 0,
    val governmentFear: Int = 0,
    val metahumanFear: Int = 0
)

internal data class IdentityEvidence(
    val id: String,
    val kind: String,
    val strength: Int,
    val holderId: String,
    val discoveredTurn: Int,
    val description: String
)

internal data class PersistentInjury(
    val id: String,
    val bodyPart: String,
    val severity: Int,
    val originEvent: String,
    val originAge: Int,
    val chronic: Boolean = false,
    val recovery: Int = 0
)

internal data class DeferredConsequence(
    val id: String,
    val sourceEvent: String,
    val earliestTurn: Int,
    val latestTurn: Int,
    val triggerTags: Set<String> = emptySet(),
    val payload: String,
    val resolved: Boolean = false
)

internal data class DramaState(
    val tension: Int = 20,
    val recoveryNeed: Int = 0,
    val personalPressure: Int = 0,
    val worldPressure: Int = 0,
    val relationshipPressure: Int = 0,
    val recentMajorEvents: List<String> = emptyList()
)

internal data class Opportunity(
    val id: String,
    val title: String,
    val category: String,
    val expiresTurn: Int,
    val urgency: Int,
    val personId: String? = null,
    val district: String? = null,
    val ignoredPayload: String = ""
)

internal enum class PowerArchitecture {
    PROJECTOR, MENTAL, BODY, MOBILITY, MATTER, TECH, OCCULT, COSMIC, ADAPTIVE
}

internal data class PowerEvolution(
    val architecture: PowerArchitecture,
    val manifestation: String,
    val branch: String = "Primaire",
    val mastery: Int = 0,
    val strain: Int = 0,
    val unlockedTechniques: List<String> = emptyList(),
    val mutations: List<String> = emptyList()
)

internal data class DeepLifeState(
    val schemaVersion: Int = 1,
    val seed: Long,
    val memories: List<CharacterMemory> = emptyList(),
    val relationships: List<DeepRelationship> = emptyList(),
    val perception: AudiencePerception = AudiencePerception(),
    val identityEvidence: List<IdentityEvidence> = emptyList(),
    val injuries: List<PersistentInjury> = emptyList(),
    val deferred: List<DeferredConsequence> = emptyList(),
    val opportunities: List<Opportunity> = emptyList(),
    val drama: DramaState = DramaState(),
    val powerEvolution: PowerEvolution? = null,
    val personality: Map<String, Int> = emptyMap()
)
