package com.metahumanlegacy.game

/** Player-facing simulation layer for 3.0. Every state here is intended to create an action,
 * restriction or visible consequence rather than another passive stat page. */
internal enum class HousingTier { FAMILY_HOME, ROOM, STUDIO, APARTMENT, HOUSE, BASE }
internal enum class EmploymentStatus { STUDENT, EMPLOYED, FREELANCE, UNEMPLOYED, RETIRED }
internal enum class BondStatus { NONE, DATING, PARTNER, SEPARATED, FAMILY }
internal enum class SecretKnowledge { UNAWARE, SUSPECTS, KNOWS, PROTECTS, THREATENS }

internal data class CivilLifeState(
    val employment: EmploymentStatus = EmploymentStatus.STUDENT,
    val jobTitle: String = "Élève",
    val monthlyIncome: Int = 0,
    val savings: Int = 0,
    val housing: HousingTier = HousingTier.FAMILY_HOME,
    val housingCost: Int = 0,
    val stress: Int = 10,
    val freeMoments: Int = 3,
    val education: Int = 0,
    val careerProgress: Int = 0
)

internal data class RelationshipLifeState(
    val personId: String,
    val bond: BondStatus = BondStatus.NONE,
    val attraction: Int = 0,
    val closeness: Int = 0,
    val availability: Int = 100,
    val secretKnowledge: SecretKnowledge = SecretKnowledge.UNAWARE,
    val sharedSecrets: List<String> = emptyList(),
    val promises: List<String> = emptyList(),
    val lastContactTurn: Int = -1
)

internal data class DistrictLifeState(
    val id: String,
    val safety: Int = 50,
    val damage: Int = 0,
    val localTrust: Int = 0,
    val criminalControl: Int = 20,
    val mediaHeat: Int = 0,
    val unresolvedThreats: List<String> = emptyList()
)

internal data class TechniqueState(
    val id: String,
    val name: String,
    val masteryRequired: Int,
    val unlocked: Boolean = false,
    val proficiency: Int = 0,
    val cooldownTurns: Int = 0,
    val lastUsedTurn: Int = -100
)

internal data class PowerRulesState(
    val range: Int = 1,
    val precision: Int = 10,
    val maxLoad: Int = 1,
    val control: Int = 10,
    val fatigue: Int = 0,
    val overload: Int = 0,
    val techniques: List<TechniqueState> = emptyList()
)

internal data class IdentitySecretState(
    val exposure: Int = 0,
    val activeRumors: List<String> = emptyList(),
    val knownBy: Map<String, SecretKnowledge> = emptyMap(),
    val evidenceIds: List<String> = emptyList()
)

internal data class LifeSimulationState(
    val schemaVersion: Int = 2,
    val civil: CivilLifeState = CivilLifeState(),
    val relationshipLives: List<RelationshipLifeState> = emptyList(),
    val districts: List<DistrictLifeState> = emptyList(),
    val powerRules: PowerRulesState = PowerRulesState(),
    val secretIdentity: IdentitySecretState = IdentitySecretState(),
    val calendarYear: Int = 0,
    val actionLog: List<String> = emptyList()
)

internal enum class LifeActionType {
    WORK, STUDY, REST, MOVE_HOME, TRAIN_POWER, USE_TECHNIQUE, PATROL, INVESTIGATE, CONTAIN_RUMOR,
    VISIT_PERSON, APOLOGIZE, ASK_HELP, REVEAL_IDENTITY, DISTANCE_PERSON
}

internal data class LifeAction(
    val type: LifeActionType,
    val targetId: String? = null,
    val label: String
)

internal data class LifeActionResult(
    val state: LifeSimulationState,
    val headline: String,
    val detail: String
)
