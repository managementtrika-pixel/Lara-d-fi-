package com.metahumanlegacy.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RelationshipMigrationTest {
    @Test
    fun legacyLifeSimulationReceivesClosenessFromExistingDeepRelationships() {
        val c = GameEngine.newCampaign(9090L).copy(turn = 16)
        val deep = DeepLifeState(
            seed = c.seed,
            relationships = listOf(
                DeepRelationship("friend", "Nia", "Ami·e", trust = 80, affection = 80)
            )
        )
        val legacy = LifeSimulationState(
            schemaVersion = 1,
            calendarYear = c.age,
            relationshipLives = listOf(RelationshipLifeState("friend", closeness = 0))
        )

        val migrated = LifeSimulationDirector.synced(c, deep, legacy)

        assertEquals(2, migrated.schemaVersion)
        assertTrue(migrated.relationshipLives.first().closeness >= 40)
    }

    @Test
    fun migratedClosenessIsOnlySeededOnceAndNeverOverwritesPlayerHistory() {
        val c = GameEngine.newCampaign(9191L).copy(turn = 16)
        val deep = DeepLifeState(
            seed = c.seed,
            relationships = listOf(
                DeepRelationship("friend", "Nia", "Ami·e", trust = 90, affection = 90)
            )
        )
        val current = LifeSimulationState(
            schemaVersion = 2,
            calendarYear = c.age,
            relationshipLives = listOf(RelationshipLifeState("friend", closeness = 12))
        )

        val synced = LifeSimulationDirector.synced(c, deep, current)

        assertEquals(12, synced.relationshipLives.first().closeness)
    }
}
