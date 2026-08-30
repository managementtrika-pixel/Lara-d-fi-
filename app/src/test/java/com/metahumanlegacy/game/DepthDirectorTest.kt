package com.metahumanlegacy.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DepthDirectorTest {
    @Before
    fun installNarrativeBundle() {
        NarrativeCodec.installAssetParts { path -> File("src/main/assets/$path").readBytes() }
    }

    @Test
    fun formativeDecadeStillUsesOnlyOriginalHiddenPowerVectors() {
        var c = GameEngine.newCampaign(775577L)
        repeat(10) {
            val event = GameEngine.event(c)
            assertEquals("FORMATIVE", event.kind)
            assertFalse(c.powerRevealed)
            val beforeAffinity = c.affinityScores
            val beforeExpression = c.expressionScores
            val beforeCost = c.costScores
            val choice = event.choices[it % event.choices.size]
            val resolved = GameEngine.resolve(c, event, choice).campaign

            // Depth memory may be added, but the vector change itself remains exactly the authored
            // choice payload handled by GameRules.
            choice.affinityDelta.forEach { key ->
                assertEquals((beforeAffinity[key] ?: 0) + 1, resolved.affinityScores[key])
            }
            choice.expressionDelta.forEach { key ->
                assertEquals((beforeExpression[key] ?: 0) + 1, resolved.expressionScores[key])
            }
            choice.costDelta.forEach { key ->
                assertEquals((beforeCost[key] ?: 0) + 1, resolved.costScores[key])
            }
            assertTrue(resolved.flags.any { flag -> flag.startsWith("deep:memory=") })
            c = resolved
        }
        assertTrue(c.powerResolved)
        assertFalse(c.powerRevealed)
        assertEquals("AWAKENING", GameEngine.event(c).kind)
    }

    @Test
    fun learnedSkillsUnlockContextualOptionsWithoutReplacingAuthoredChoices() {
        val c = GameEngine.newCampaign(42L).copy(
            turn = 30,
            powerFamily = "Énergie",
            flags = setOf(
                "POWER_REVEALED", "ALIAS_CHOSEN", "deep:v1",
                "deep:skill_rescue=40", "deep:skill_investigation=55",
                "deep:skill_presence=38", "deep:skill_discipline=45"
            )
        )
        val base = EventNode(
            id = "DEPTH_CRISIS",
            title = "Une tour vacille",
            text = "La structure menace de céder pendant qu'une foule reste coincée.",
            choices = listOf(
                Choice("Évacuer étage par étage", approach = "CARE", stakes = 3, risk = 5),
                Choice("Stabiliser la structure", approach = "ORDER", stakes = 3, risk = 4)
            ),
            category = "CRISE",
            provocation = "URGENCE",
            stakes = 3,
            kind = "MAJOR"
        )

        val enriched = DepthDirector.enrichEvent(c, base)
        assertEquals(base.choices.size, 2)
        assertTrue(enriched.choices.size > base.choices.size)
        assertTrue(enriched.choices.any { it.label.contains("secours", ignoreCase = true) })
        assertTrue(enriched.text.contains("informations", ignoreCase = true))
        assertEquals(enriched, DepthDirector.enrichEvent(c, base))
    }

    @Test
    fun consequencesCreatePersistentMemoryRelationsAndStoryPressure() {
        val before = GameEngine.newCampaign(9001L).copy(
            turn = 25,
            powerFamily = "Gravité",
            flags = setOf("POWER_REVEALED", "ALIAS_CHOSEN", "deep:v1"),
            health = 90,
            control = 38
        )
        val event = EventNode(
            id = "RIVAL_BREAKPOINT",
            title = "Le rival revient",
            text = "Il te pousse à agir devant des témoins.",
            choices = listOf(
                Choice(
                    label = "Promettre de protéger les témoins et l'affronter",
                    moral = 2,
                    prestige = 2,
                    fear = 1,
                    power = 2,
                    risk = 7,
                    approach = "CARE",
                    stakes = 3,
                    relationDelta = -2,
                    deferredHook = true
                )
            ),
            category = "RIVAL",
            provocation = "CONFRONTATION",
            stakes = 3,
            threadId = "RIVAL_CORE",
            threadStage = 2,
            kind = "MAJOR"
        )
        val rawAfter = GameRules.apply(before, event, event.choices.first())
        val deep = DepthDirector.afterChoice(before, rawAfter, event, event.choices.first())
        val flags = deep.campaign.flags

        assertTrue(flags.any { it.startsWith("deep:memory=") })
        assertTrue(flags.any { it.startsWith("deep:promise=") })
        assertTrue(flags.any { it.startsWith("deep:nemesis_") })
        assertTrue(flags.any { it.startsWith("deep:rel_rival_") })
        assertTrue(flags.any { it.startsWith("deep:clock_") })
        assertTrue(flags.any { it.startsWith("deep:strain=") })
        assertNotEquals(before.flags, flags)
    }

    @Test
    fun annualActionsFeedSkillsIntoMainStoryWithoutAdvancingTheYear() {
        val c = GameEngine.newCampaign(31337L).copy(
            turn = 24,
            powerFamily = "Énergie",
            flags = setOf("POWER_REVEALED", "ALIAS_CHOSEN", "deep:v1")
        )
        val state = AnnualActionState(
            seed = c.seed,
            turn = c.turn,
            used = 1,
            rescue = 44,
            investigation = 32,
            presence = 21,
            discipline = 37
        )
        val card = AnnualActionCard(
            id = "test_rescue",
            title = "Intervention test",
            description = "",
            category = AnnualActionCategory.INTERVENTION,
            iconKey = "alt_01",
            focus = "Secours",
            outcome = ""
        )
        val after = DepthDirector.afterAnnualAction(c, state, card)
        assertEquals(c.turn, after.turn)
        assertTrue(after.flags.contains("deep:skill_rescue=44"))
        assertTrue(after.flags.contains("deep:skill_investigation=32"))
        assertTrue(after.flags.any { it.startsWith("deep:interlude=") })
    }

    @Test
    fun legacyUsesCareerSpecificDepthInsteadOfOnlyAlignmentAndScope() {
        val base = GameEngine.newCampaign(5150L).copy(
            turn = 156,
            powerFamily = "Matière",
            alias = "Mosaic",
            influence = 650,
            prestige = 70,
            opinion = 72,
            familyBond = 82,
            flags = setOf(
                "POWER_REVEALED", "ALIAS_CHOSEN",
                "deep:code_care=12", "deep:code_order=2",
                "deep:rel_family_trust=88", "deep:rel_family_affection=91",
                "deep:promise_kept=60,FAMILY", "deep:promise_kept=80,DUTY", "deep:promise_kept=100,FAMILY",
                "deep:technique=70,MATIERE_CARE_SIGNATURE",
                "deep:media_frame=ICÔNE POPULAIRE", "deep:legal=COLLABORATEUR AUTORISÉ",
                "deep:district_sentiment=74"
            )
        )
        val plain = GameRules.legacyTitle(base)
        val deepTitle = GameEngine.legacyTitle(base)
        assertTrue(deepTitle.startsWith(plain))
        assertNotEquals(plain, deepTitle)
        assertTrue(GameEngine.legacySummary(base).isNotBlank())
        assertTrue(GameEngine.legacyScore(base) >= GameRules.legacyScore(base))
    }
}
