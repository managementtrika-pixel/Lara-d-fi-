package com.metahumanlegacy.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PowerTechniqueSimulationTest {
    private fun poweredCampaign(control: Int = 30): Campaign = GameEngine.newCampaign(515151L).copy(
        turn = 12,
        powerFamily = "Énergie",
        weakness = "Fatigue extrême",
        control = control,
        flags = setOf("POWER_REVEALED", "ALIAS_CHOSEN")
    )

    @Test
    fun revealedPowerSeedsArchitectureSpecificTechniques() {
        val c = poweredCampaign(30)
        val state = LifeSimulationDirector.bootstrap(c, DeepLifeState(seed = c.seed))

        assertEquals(4, state.powerRules.techniques.size)
        assertTrue(state.powerRules.techniques.any { it.name == "Tir focalisé" && it.unlocked })
        assertTrue(LifeSimulationDirector.availableActions(c, state).any { it.type == LifeActionType.USE_TECHNIQUE })
    }

    @Test
    fun usingTechniqueHasWorldCostAndBuildsProficiency() {
        val c = poweredCampaign(30)
        val state = LifeSimulationDirector.bootstrap(c, DeepLifeState(seed = c.seed))
        val technique = state.powerRules.techniques.first { it.unlocked }
        val beforeDistrict = state.districts.first { it.id == "quartier" }
        val result = LifeSimulationDirector.perform(
            c,
            state,
            LifeAction(LifeActionType.USE_TECHNIQUE, technique.id, "Utiliser ${technique.name}")
        )
        val afterTechnique = result.state.powerRules.techniques.first { it.id == technique.id }
        val afterDistrict = result.state.districts.first { it.id == "quartier" }

        assertEquals(state.civil.freeMoments - 1, result.state.civil.freeMoments)
        assertTrue(afterTechnique.proficiency > technique.proficiency)
        assertTrue(result.state.powerRules.fatigue > state.powerRules.fatigue)
        assertTrue(result.state.secretIdentity.exposure > state.secretIdentity.exposure)
        assertTrue(afterDistrict.safety > beforeDistrict.safety)
        assertTrue(afterDistrict.criminalControl < beforeDistrict.criminalControl)
    }

    @Test
    fun trainingCanUnlockNextTechniqueInsteadOfOnlyAddingStats() {
        val c = poweredCampaign(39)
        val seeded = LifePowerTechniqueCatalog.seeded(c)
        val rules = PowerRulesState(control = 39, precision = 20, techniques = seeded)
        val (trained, message) = LifePowerTechniqueCatalog.train(c, rules)

        assertTrue(trained.control >= 40)
        assertTrue(trained.techniques.first { it.name == "Zone contrôlée" }.unlocked)
        assertNotNull(message)
        assertTrue(message!!.contains("Zone contrôlée"))
    }

    @Test
    fun higherTierTechniqueCreatesCooldownAndRestClearsIt() {
        val c = poweredCampaign(60)
        val state = LifeSimulationDirector.bootstrap(c, DeepLifeState(seed = c.seed)).copy(
            civil = CivilLifeState(freeMoments = 3)
        )
        val technique = state.powerRules.techniques.first { it.unlocked && it.masteryRequired >= 40 }
        val used = LifeSimulationDirector.perform(
            c,
            state,
            LifeAction(LifeActionType.USE_TECHNIQUE, technique.id, "Technique")
        ).state
        val afterUse = used.powerRules.techniques.first { it.id == technique.id }

        assertTrue(afterUse.cooldownTurns > 0)
        val rested = LifeSimulationDirector.perform(c, used, LifeAction(LifeActionType.REST, label = "Récupérer")).state
        assertEquals(0, rested.powerRules.techniques.first { it.id == technique.id }.cooldownTurns)
    }

    @Test
    fun unlockedLifeTechniquesFeedDeepPowerEvolution() {
        val c = poweredCampaign(45)
        val deep = DeepLifeState(
            seed = c.seed,
            powerEvolution = PowerEvolution(
                architecture = PowerArchitecture.PROJECTOR,
                manifestation = c.powerFamily,
                mastery = 30
            )
        )
        val simulation = LifeSimulationDirector.bootstrap(c, deep)
        val merged = LifeSimulationDirector.mergedIntoDeep(deep, simulation)

        assertNotNull(merged.powerEvolution)
        assertTrue(merged.powerEvolution!!.unlockedTechniques.any { it.contains("Tir focalisé") })
        assertTrue(merged.powerEvolution!!.mastery >= simulation.powerRules.control)
    }
}
