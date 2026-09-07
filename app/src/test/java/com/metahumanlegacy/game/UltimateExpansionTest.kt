package com.metahumanlegacy.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UltimateExpansionTest {
    @Before
    fun installNarrativeBundle() {
        NarrativeCodec.installAssetParts { path -> File("src/main/assets/$path").readBytes() }
    }

    @Test
    fun formativeDecadeRemainsTheOnlyPowerBuilder() {
        val seed = 840084L
        var c = GameEngine.newCampaign(seed)
        var state = UltimateStore.fallback(c)
        var annual = AnnualActionState.fresh(c)

        repeat(10) { index ->
            assertEquals(8 + index, c.age)
            val authored = GameEngine.event(c)
            val ultimate = UltimateGameEngine.event(c, state, annual)
            assertEquals("FORMATIVE", ultimate.kind)
            assertEquals(authored.choices.map { it.label }, ultimate.choices.map { it.label })
            assertFalse(c.powerRevealed)

            val choice = ultimate.choices[index % ultimate.choices.size]
            val result = UltimateGameEngine.resolve(c, state, ultimate, choice)
            c = result.campaign
            state = result.state
            annual = annual.synced(c)
        }

        assertEquals(18, c.age)
        assertTrue(c.powerResolved)
        assertFalse(c.powerRevealed)
        assertEquals("AWAKENING", UltimateGameEngine.event(c, state, annual).kind)
    }

    @Test
    fun visualAndCityPersonalizationCannotAlterResolvedPower() {
        val blueprint = CharacterBlueprint(
            "Nora", "Vale", "elle", "Vesper", "Les Docks", "Classe moyenne",
            "Justice", "Technologie", "Curieux"
        )
        val seed = 771177L
        var a = GameEngine.newCampaign(seed, blueprint)
        var b = GameEngine.newCampaign(seed, blueprint)
        var ua = UltimateStore.create(a, UltimateCatalog.randomDraft(1L, blueprint).copy(cityArchetype = "Ville côtière", hair = "Tresses", skinTone = "Foncé"))
        var ub = UltimateStore.create(b, UltimateCatalog.randomDraft(2L, blueprint).copy(cityArchetype = "Mégalopole technologique", hair = "Rasé", skinTone = "Clair"))
        var aa = AnnualActionState.fresh(a)
        var ab = AnnualActionState.fresh(b)

        repeat(10) { index ->
            val ea = UltimateGameEngine.event(a, ua, aa)
            val eb = UltimateGameEngine.event(b, ub, ab)
            val choiceIndex = index % minOf(ea.choices.size, eb.choices.size)
            val ra = UltimateGameEngine.resolve(a, ua, ea, ea.choices[choiceIndex])
            val rb = UltimateGameEngine.resolve(b, ub, eb, eb.choices[choiceIndex])
            a = ra.campaign; ua = ra.state; aa = aa.synced(a)
            b = rb.campaign; ub = rb.state; ab = ab.synced(b)
        }

        assertEquals(a.powerFamily, b.powerFamily)
        assertEquals(a.affinityScores, b.affinityScores)
        assertEquals(a.expressionScores, b.expressionScores)
        assertEquals(a.costScores, b.costScores)
        assertNotEquals(ua.cityArchetype, ub.cityArchetype)
        assertNotEquals(ua.hair, ub.hair)
    }

    @Test
    fun annualInterludesNeverAdvanceTheYear() {
        val c = GameEngine.newCampaign(31337L).copy(
            turn = 24,
            powerFamily = "Énergie",
            alias = "Arc",
            flags = setOf("POWER_REVEALED", "ALIAS_CHOSEN", "deep:v1")
        )
        val state = UltimateStore.fallback(c).copy(powerStrain = 62)
        val annual = AnnualActionState.fresh(c)
        val card = UltimateGameEngine.annualActions(c, state, annual).first { it.id == "ultimate_recovery" }
        val result = AnnualActionEngine.perform(c, annual, card)!!
        val afterState = UltimateGameEngine.afterAnnualAction(result.campaign, state, result.state, card)

        assertEquals(c.turn, result.campaign.turn)
        assertEquals(c.age, result.campaign.age)
        assertEquals(1, result.state.used)
        assertTrue(afterState.powerStrain < state.powerStrain)
    }

    @Test
    fun learnedAnnualSkillsCreateContextualStoryChoices() {
        val c = GameEngine.newCampaign(9090L).copy(
            turn = 28,
            powerFamily = "Gravité",
            alias = "Anchor",
            control = 58,
            flags = setOf("POWER_REVEALED", "ALIAS_CHOSEN", "deep:v1")
        )
        val state = UltimateStore.fallback(c)
        val annual = AnnualActionState(c.seed, c.turn, rescue = 48, investigation = 50, presence = 45, discipline = 54)
        val base = EventNode(
            id = "ULTIMATE_CRISIS",
            title = "Un complexe s'effondre",
            text = "Des personnes sont coincées tandis qu'une piste concernant l'origine de l'accident apparaît.",
            choices = listOf(
                Choice("Forcer un passage", approach = "ASCEND", power = 2, risk = 7, stakes = 4),
                Choice("Évacuer la zone", approach = "CARE", risk = 4, stakes = 4)
            ),
            category = "CRISE_ENQUETE",
            provocation = "EFFONDREMENT",
            stakes = 4,
            kind = "MAJOR"
        )
        val enriched = UltimateDirector.enrich(c, state, annual, base)

        assertTrue(enriched.choices.size > base.choices.size)
        assertTrue(enriched.choices.any { it.flag == "ultimate_skill_rescue" })
        assertTrue(enriched.choices.any { it.flag == "ultimate_skill_investigation" })
        assertTrue(enriched.choices.any { it.flag == "ultimate_skill_discipline" })
    }

    @Test
    fun majorChoicesChangeRelationshipsCityPowerAndBiography() {
        val before = GameEngine.newCampaign(123987L).copy(
            turn = 32,
            powerFamily = "Énergie",
            alias = "Pulse",
            power = 62,
            control = 58,
            prestige = 48,
            opinion = 35,
            influence = 220,
            flags = setOf("POWER_REVEALED", "ALIAS_CHOSEN", "deep:v1")
        )
        val state = UltimateStore.fallback(before).copy(powerStrain = 25)
        val event = EventNode(
            id = "CITY_BREAK",
            title = "Le viaduc cède",
            text = "Une attaque laisse le quartier au bord de l'effondrement.",
            choices = listOf(Choice("Encaisser l'impact et protéger les civils", moral = 2, power = 2, risk = 7, approach = "CARE", stakes = 4)),
            category = "CRISE_COMBAT",
            provocation = "ATTAQUE",
            stakes = 4,
            kind = "MAJOR"
        )
        val core = GameRules.apply(before, event, event.choices.first())
        val result = UltimateDirector.afterChoice(before, core, state, event, event.choices.first())

        assertTrue(result.state.memories.isNotEmpty())
        assertTrue(result.state.powerStrain > state.powerStrain)
        assertNotEquals(state.districts, result.state.districts)
        assertNotEquals(state.relations, result.state.relations)
        assertTrue(result.state.costumeEra >= state.costumeEra)
    }

    @Test
    fun legacySummaryUsesTheActualCareerWorld() {
        val c = GameEngine.newCampaign(4444L).copy(
            turn = 156,
            powerFamily = "Matière",
            alias = "Mosaic",
            prestige = 78,
            opinion = 71,
            influence = 940,
            flags = setOf("POWER_REVEALED", "ALIAS_CHOSEN", "deep:v1")
        )
        val base = UltimateStore.fallback(c)
        val state = base.copy(
            heroPresentation = "Sobre",
            costumePalette = "Ivoire / or",
            nemesis = "Marek",
            techniques = listOf("Forge Aegis", "Trame Zéro"),
            cases = listOf(UltimateCase("one", "Dossier Meridian", stage = 4, evidence = 88, solved = true)),
            cityCondition = 61
        )
        val summary = UltimateDirector.legacySummary(c, state)

        assertTrue(summary.contains("Marek"))
        assertTrue(summary.contains("Forge Aegis"))
        assertTrue(summary.contains(state.cityArchetype))
        assertTrue(summary.length > 120)
    }
}
