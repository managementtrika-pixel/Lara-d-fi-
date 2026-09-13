package com.metahumanlegacy.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScopeEscalationDirectorTest {
    @Before
    fun installNarrativeBundle() {
        NarrativeCodec.installAssetParts { path -> File("src/main/assets/$path").readBytes() }
    }

    private fun campaign(influence: Int) = GameEngine.newCampaign(939393L).copy(
        turn = 24,
        powerFamily = "Énergie",
        influence = influence,
        flags = setOf("POWER_REVEALED", "ALIAS_CHOSEN")
    )

    private fun crisis(kind: String = "MAJOR") = EventNode(
        id = "scope_test",
        title = "Une crise éclate",
        text = "Plusieurs réponses sont possibles.",
        choices = listOf(
            Choice("Intervenir immédiatement", risk = 4, approach = "CARE", stakes = 4),
            Choice("Observer avant d'agir", risk = 3, approach = "TRUTH", stakes = 4)
        ),
        category = "CRISE",
        provocation = "Agir",
        stakes = 4,
        kind = kind
    )

    @Test
    fun everyCareerScaleGetsItsOwnResponsibility() {
        val expected = listOf(
            0 to "scope_response_street",
            100 to "scope_response_district",
            200 to "scope_response_city",
            350 to "scope_response_region",
            600 to "scope_response_country",
            950 to "scope_response_world"
        )
        expected.forEach { (influence, flag) ->
            val c = campaign(influence)
            val enriched = ScopeEscalationDirector.enrich(c, crisis())
            assertTrue("scope=${c.scope}", enriched.choices.any { it.flag == flag })
            assertTrue(enriched.text.contains("ÉCHELLE ${c.scope.label.uppercase()}"))
        }
    }

    @Test
    fun cityAndWorldDoNotOfferTheSameResponsibility() {
        val city = ScopeEscalationDirector.enrich(campaign(200), crisis()).choices.last()
        val world = ScopeEscalationDirector.enrich(campaign(950), crisis()).choices.last()
        assertNotEquals(city.label, world.label)
        assertTrue(world.impact > city.impact)
    }

    @Test
    fun formativeAwakeningAndEndingStayUntouched() {
        val c = campaign(950)
        listOf("FORMATIVE", "AWAKENING", "ENDING").forEach { kind ->
            val base = crisis(kind)
            assertEquals(base, ScopeEscalationDirector.enrich(c, base))
        }
    }

    @Test
    fun unrevealedPowerNeverReceivesCareerScaleChoice() {
        val c = campaign(950).copy(flags = emptySet(), powerFamily = "Non révélé")
        val base = crisis()
        assertEquals(base, ScopeEscalationDirector.enrich(c, base))
    }

    @Test
    fun directorIsIdempotent() {
        val c = campaign(600)
        val once = ScopeEscalationDirector.enrich(c, crisis())
        assertEquals(once, ScopeEscalationDirector.enrich(c, once))
    }

    @Test
    fun scopeChoiceDoesNotEvictIdentityOrTechniqueAtSevenChoiceCap() {
        val protected = crisis().copy(choices = listOf(
            Choice("Générique 1"), Choice("Générique 2"), Choice("Générique 3"),
            Choice("Générique 4"), Choice("Générique 5"),
            Choice("Protéger mon identité", flag = "identity_pressure:hide"),
            Choice("Technique", flag = "story_technique:projector_zone")
        ))
        val enriched = ScopeEscalationDirector.enrich(campaign(600), protected)
        assertEquals(7, enriched.choices.size)
        assertTrue(enriched.choices.any { it.flag?.startsWith("identity_pressure:") == true })
        assertTrue(enriched.choices.any { it.flag?.startsWith("story_technique:") == true })
        assertTrue(enriched.choices.any { it.flag == "scope_response_country" })
    }

    @Test
    fun guardPreventsFallbackFromJumpingAboveEarnedScope() {
        val c = campaign(0).copy(turn = 100)
        assertEquals(Scope.STREET, c.scope)
        val highScopeBeat = NarrativeCodec.beats().first {
            it.stage == 1 && it.minAge <= c.age && it.minScope.ordinal > c.scope.ordinal
        }
        val unsafe = NarrativeRepository.byId(highScopeBeat.id, c)!!
        val guarded = NarrativeScopeGuard.enforce(c, unsafe)

        assertNotEquals(unsafe.id, guarded.id)
        val replacementBeat = NarrativeCodec.beats().firstOrNull { it.id == guarded.id }
        assertTrue(replacementBeat == null || replacementBeat.minScope.ordinal <= c.scope.ordinal)
    }
}
