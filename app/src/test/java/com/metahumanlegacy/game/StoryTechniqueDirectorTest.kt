package com.metahumanlegacy.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryTechniqueDirectorTest {
    private fun campaign(): Campaign = GameEngine.newCampaign(626262L).copy(
        turn = 24,
        powerFamily = "Énergie",
        control = 58,
        flags = setOf("POWER_REVEALED", "ALIAS_CHOSEN")
    )

    private fun deepWithTechnique(c: Campaign, unlocked: Boolean = true): DeepLifeState {
        val technique = TechniqueState(
            id = "projector_zone",
            name = "Zone contrôlée",
            masteryRequired = 40,
            unlocked = unlocked,
            proficiency = 52
        )
        return DeepLifeState(
            seed = c.seed,
            powerEvolution = PowerEvolution(PowerArchitecture.PROJECTOR, c.powerFamily, mastery = c.control),
            lifeSimulation = LifeSimulationState(
                powerRules = PowerRulesState(control = c.control, precision = 50, techniques = listOf(technique))
            )
        )
    }

    private fun crisis(kind: String = "MAJOR") = EventNode(
        id = "story_crisis",
        title = "Une tour menace de céder",
        text = "La structure travaille et plusieurs personnes sont encore à l'intérieur.",
        choices = listOf(Choice("Évacuer le périmètre", moral = 1, risk = 3, approach = "CARE")),
        category = "CATASTROPHE",
        provocation = "Agir",
        stakes = 4,
        kind = kind
    )

    @Test
    fun learnedUnlockedTechniqueAppearsByNameInCrisis() {
        val c = campaign()
        val enriched = StoryTechniqueDirector.enrich(c, deepWithTechnique(c), crisis())
        val techniqueChoice = enriched.choices.firstOrNull { StoryTechniqueDirector.techniqueId(it) == "projector_zone" }

        assertNotNull(techniqueChoice)
        assertTrue(techniqueChoice!!.label.contains("Zone contrôlée"))
        assertTrue(enriched.text.contains("Zone contrôlée"))
    }

    @Test
    fun rawControlDoesNotInventLockedTechnique() {
        val c = campaign().copy(control = 90)
        val enriched = StoryTechniqueDirector.enrich(c, deepWithTechnique(c, unlocked = false), crisis())
        assertFalse(enriched.choices.any { StoryTechniqueDirector.techniqueId(it) != null })
    }

    @Test
    fun awakeningAndFormativeScenesNeverReceiveCareerTechnique() {
        val c = campaign()
        val deep = deepWithTechnique(c)
        assertEquals(crisis("AWAKENING"), StoryTechniqueDirector.enrich(c, deep, crisis("AWAKENING")))
        assertEquals(crisis("FORMATIVE"), StoryTechniqueDirector.enrich(c, deep, crisis("FORMATIVE")))
    }

    @Test
    fun identityAndTechniqueChoicesCoexistAtSevenChoiceCap() {
        val c = campaign()
        val identity = Choice(
            "Détourner l'attention avant d'agir",
            risk = 3,
            approach = "TRUTH",
            identityDelta = -2,
            flag = "identity_pressure:contain"
        )
        val crowded = crisis().copy(
            choices = (1..6).map { Choice("Choix générique $it", risk = it.coerceAtMost(7)) } + identity
        )
        val enriched = StoryTechniqueDirector.enrich(c, deepWithTechnique(c), crowded)

        assertEquals(7, enriched.choices.size)
        assertTrue(enriched.choices.any { it.flag == "identity_pressure:contain" })
        assertTrue(enriched.choices.any { StoryTechniqueDirector.techniqueId(it) == "projector_zone" })
    }

    @Test
    fun usingStoryTechniqueBuildsProficiencyAndCostsFatigue() {
        val c = campaign()
        val before = deepWithTechnique(c)
        val choice = StoryTechniqueDirector.enrich(c, before, crisis()).choices
            .first { StoryTechniqueDirector.techniqueId(it) == "projector_zone" }
        val after = StoryTechniqueDirector.applyUse(c, before, choice)
        val beforeRules = before.lifeSimulation!!.powerRules
        val afterRules = after.lifeSimulation!!.powerRules
        val beforeTechnique = beforeRules.techniques.first()
        val afterTechnique = afterRules.techniques.first()

        assertTrue(afterTechnique.proficiency > beforeTechnique.proficiency)
        assertTrue(afterRules.fatigue > beforeRules.fatigue)
        assertTrue(after.lifeSimulation!!.secretIdentity.exposure > before.lifeSimulation!!.secretIdentity.exposure)
        assertTrue(after.lifeSimulation!!.actionLog.last().contains("Zone contrôlée"))
    }

    @Test
    fun highTierTechniqueEntersCooldownAfterStoryUse() {
        val c = campaign()
        val technique = TechniqueState("projector_signature", "Décharge signature", 70, unlocked = true, proficiency = 80)
        val deep = deepWithTechnique(c).copy(
            lifeSimulation = deepWithTechnique(c).lifeSimulation!!.copy(
                powerRules = PowerRulesState(control = 80, techniques = listOf(technique))
            )
        )
        val choice = StoryTechniqueDirector.enrich(c, deep, crisis()).choices
            .first { StoryTechniqueDirector.techniqueId(it) == technique.id }
        val after = StoryTechniqueDirector.applyUse(c, deep, choice)

        assertEquals(1, after.lifeSimulation!!.powerRules.techniques.first().cooldownTurns)
    }
}
