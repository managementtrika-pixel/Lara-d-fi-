package com.metahumanlegacy.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormativeVariationDirectorTest {
    private fun campaign(seed: Long, turn: Int = 0, background: String = "Classe moyenne") = Campaign(
        seed = seed,
        name = "Test",
        modifier = "Première génération",
        city = "Vesper",
        district = "Centre",
        socialBackground = background,
        temperament = "Curieux",
        turn = turn
    )

    @Test
    fun formativeVariationIsDeterministicForSameLife() {
        val c = campaign(44L, turn = 4, background = "Milieu scientifique")
        val base = NarrativeRepository.event(c)
        val first = FormativeVariationDirector.enrich(c, base)
        val second = FormativeVariationDirector.enrich(c, base)

        assertEquals(first.title, second.title)
        assertEquals(first.text, second.text)
        assertEquals(first.choices.map { it.label }, second.choices.map { it.label })
    }

    @Test
    fun variationNeverChangesHiddenChoiceMechanics() {
        val c = campaign(991L, turn = 8, background = "Quartier populaire")
        val base = NarrativeRepository.event(c)
        val varied = FormativeVariationDirector.enrich(c, base)

        assertEquals(base.choices.size, varied.choices.size)
        base.choices.zip(varied.choices).forEach { (before, after) ->
            assertEquals(before.copy(label = after.label), after)
        }
    }

    @Test
    fun multipleLivesProduceMultipleFormativePresentations() {
        val titles = (1L..60L).map { seed ->
            val c = campaign(seed, turn = 2, background = if (seed % 2L == 0L) "Foyer instable" else "Famille très présente")
            FormativeVariationDirector.enrich(c, NarrativeRepository.event(c)).title
        }.toSet()

        assertTrue("Expected replayable formative titles, got $titles", titles.size >= 3)
    }

    @Test
    fun nonFormativeEventsAreUntouched() {
        val c = campaign(77L, turn = 10)
        val base = NarrativeRepository.event(c)
        assertEquals(base, FormativeVariationDirector.enrich(c, base))
    }
}
