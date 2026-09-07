package com.metahumanlegacy.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReleaseReadinessTest {
    @Before
    fun installNarrativeBundle() {
        NarrativeCodec.installAssetParts { path -> File("src/main/assets/" + path).readBytes() }
    }

    @Test
    fun authoredNarrativeGraphIsCompleteAndInternallyConsistent() {
        val prologue = NarrativeCodec.prologue()
        val awakening = NarrativeCodec.awakening()
        val foundation = NarrativeCodec.foundation()
        val beats = NarrativeCodec.beats()
        val endings = NarrativeCodec.endings()

        assertEquals(10, prologue.size)
        assertEquals((8..17).toList(), prologue.map { it.age })
        assertEquals(18, awakening.age)
        assertEquals(5, foundation.size)
        assertEquals(250, beats.size)
        assertEquals(50, beats.map { it.arc }.toSet().size)
        assertEquals(1000, beats.sumOf { it.choices.size })
        assertEquals(200, endings.values.sumOf { it.size })

        val allIds = prologue.map { it.id } + awakening.id + foundation.map { it.id } + beats.map { it.id }
        assertEquals("Every authored node id must be unique", allIds.size, allIds.toSet().size)

        val expectedRoutes = setOf("CARE", "ORDER", "TRUTH", "ASCEND")
        beats.groupBy { it.arc }.forEach { entry ->
            val arc = entry.key
            val arcBeats = entry.value
            assertEquals(arc + " must contain five authored stages", setOf(1, 2, 3, 4, 5), arcBeats.map { it.stage }.toSet())
            assertTrue(arc + " must have four endings", endings[arc]?.keys == expectedRoutes)
            arcBeats.forEach { beat ->
                assertTrue(beat.id + " age window must be valid", beat.minAge >= 18 && beat.maxAge >= beat.minAge)
                assertEquals(beat.id + " must expose all four route families", expectedRoutes, beat.choices.map { it.approach }.toSet())
                assertEquals(beat.id + " must have four decisions", 4, beat.choices.size)
            }
        }

        assertEquals(1064, GameEngine.constatCount())
    }

    @Test
    fun childhoodExactlySpansEightToSeventeenAndAwakensAtEighteen() {
        var c = GameEngine.newCampaign(801718L)
        repeat(10) { index ->
            assertEquals(8 + index, c.age)
            assertFalse(c.powerRevealed)
            val event = GameEngine.event(c)
            assertEquals("FORMATIVE", event.kind)
            assertEquals(index + 1, event.threadStage)
            c = GameEngine.resolve(c, event, event.choices[index % event.choices.size]).campaign
        }

        assertEquals(18, c.age)
        assertTrue(c.powerResolved)
        assertFalse(c.powerRevealed)

        val awakening = GameEngine.event(c)
        assertEquals("AWAKENING", awakening.kind)
        c = GameEngine.resolve(c, awakening, awakening.choices.first()).campaign

        assertEquals(18, c.age)
        assertTrue(c.powerRevealed)
        assertTrue(c.needsAlias)
    }

    @Test
    fun contrastingRoutesSurviveLongDeterministicLivesWithoutDeadlock() {
        val routes = listOf("CARE", "ORDER", "TRUTH", "ASCEND")
        var completedRuns = 0
        val powers = linkedSetOf<String>()

        routes.forEachIndexed { routeIndex, route ->
            repeat(6) { seedIndex ->
                var c = GameEngine.newCampaign(10_000L + routeIndex * 1_000L + seedIndex)
                var guard = 0
                var previousTurn = -1
                var previousAge = 7

                while (!c.finished && guard < 230) {
                    assertTrue("turn must never go backwards", c.turn >= previousTurn)
                    assertTrue("age must never go backwards", c.age >= previousAge)
                    assertStateBounds(c)

                    val event = GameEngine.event(c)
                    assertTrue("event " + event.id + " must always have a decision", event.choices.isNotEmpty())

                    val choice = when {
                        event.kind == "FORMATIVE" -> event.choices[(guard + seedIndex + routeIndex) % event.choices.size]
                        else -> event.choices.firstOrNull { it.approach == route }
                            ?: event.choices[(guard + routeIndex) % event.choices.size]
                    }

                    previousTurn = c.turn
                    previousAge = c.age
                    c = GameEngine.resolve(c, event, choice).campaign

                    if (c.needsAlias) c = GameEngine.setAlias(c, "Legacy-" + (routeIndex + 1) + "-" + seedIndex)
                    guard++
                }

                assertTrue("simulation must terminate by the safety guard", c.finished)
                assertStateBounds(c)
                assertTrue(c.timeline.size <= 180)
                if (c.health > 0) assertTrue(c.turn >= 196)
                if (c.powerResolved) powers += c.powerFamily
                completedRuns++
            }
        }

        assertEquals(24, completedRuns)
        assertTrue("the power resolver should produce varied destinies", powers.size >= 6)
    }

    @Test
    fun everyConfiguredPowerAndWeaknessHasAPlayableRuntimeProfile() {
        val powers = PowerResolver.powerCatalog()
        val weaknesses = PowerResolver.weaknessCatalog()
        assertTrue("At least the promised 26 distinct power outcomes must exist", powers.size >= 26)
        assertTrue("Weakness catalog must remain meaningfully varied", weaknesses.size >= 8)

        powers.forEach { power ->
            val profile = powerVisualProfile(power)
            assertTrue(power + " must have a non-empty visual profile label", profile.iconKey.isNotBlank())
        }

        repeat(160) { seedIndex ->
            var c = GameEngine.newCampaign(50_000L + seedIndex)
            repeat(10) { turn ->
                val event = GameEngine.event(c)
                c = GameEngine.resolve(c, event, event.choices[(seedIndex + turn) % event.choices.size]).campaign
            }
            assertTrue(c.powerFamily in powers)
            assertTrue(c.weakness in weaknesses)
            assertTrue(c.powerSignature.isNotBlank())
            assertTrue(c.powerRevealText.isNotBlank())
            assertTrue(c.powerCostText.isNotBlank())
            assertTrue(c.power in 24..58)
            assertTrue(c.control in 18..55)
        }
    }

    @Test
    fun scopeThresholdsRemainOrderedAndReachWorld() {
        val base = GameEngine.newCampaign(777L).copy(
            powerFamily = "Énergie",
            flags = setOf("POWER_REVEALED", "ALIAS_CHOSEN")
        )
        assertEquals(Scope.STREET, base.copy(influence = 0).scope)
        assertEquals(Scope.DISTRICT, base.copy(influence = 75).scope)
        assertEquals(Scope.CITY, base.copy(influence = 180).scope)
        assertEquals(Scope.REGION, base.copy(influence = 340).scope)
        assertEquals(Scope.COUNTRY, base.copy(influence = 560).scope)
        assertEquals(Scope.WORLD, base.copy(influence = 900).scope)
    }

    @Test
    fun canonicalChildhoodEventsAreAgeAppropriateAndFullyInteractive() {
        var c = GameEngine.newCampaign(817181L)
        val expectedTitles = listOf(
            "LE SAC DANS LA COUR",
            "UNE PLACE À TABLE",
            "LE DÉFI DU TOIT",
            "CE QUE TU AS VU",
            "LA PORTE FERMÉE",
            "LE GROUPE",
            "LE MESSAGE QUI TOURNE",
            "APRÈS LES COURS",
            "LA NUIT DU QUARTIER",
            "CE QUE TU VEUX DEVENIR"
        )

        repeat(10) { index ->
            val event = GameEngine.event(c)
            assertEquals(8 + index, c.age)
            assertEquals(expectedTitles[index], event.title)
            assertEquals("FORMATIVE", event.kind)
            assertEquals(4, event.choices.size)
            assertTrue(event.text.isNotBlank())
            assertTrue(event.choices.all { it.label.isNotBlank() })
            c = GameEngine.resolve(c, event, event.choices[index % 4]).campaign
        }

        val awakening = GameEngine.event(c)
        assertEquals(18, c.age)
        assertEquals("LA PREMIÈRE MANIFESTATION", awakening.title)
        assertTrue(awakening.text.contains("18 ans"))
        assertTrue(awakening.text.contains("pas un pouvoir choisi", ignoreCase = true))
        assertEquals(setOf("CARE", "ORDER", "TRUTH", "ASCEND"), awakening.choices.map { it.approach }.toSet())
    }

    @Test
    fun legacyV4ChronologyMigrationPreservesProgressAndShiftsAgeReferences() {
        val old = GameEngine.newCampaign(303030L).copy(
            turn = 10,
            powerFamily = "Énergie",
            weakness = "Surcharge",
            flags = setOf("POWER_REVEALED", "deep:memory=evt,28,Care"),
            timeline = listOf("28 ans — Ancienne scène", "↳ souvenir sans âge")
        )

        val migrated = migrateLegacyV4Chronology(old)

        assertEquals(old.turn, migrated.turn)
        assertEquals(18, migrated.age)
        assertTrue("MIGRATED_V4_TO_CHILDHOOD_CANON" in migrated.flags)
        assertTrue("CHRONOLOGY_8_TO_18" in migrated.flags)
        assertTrue("deep:memory=evt,18,Care" in migrated.flags)
        assertTrue(migrated.timeline.any { it.startsWith("18 ans") })
        assertTrue(migrated.timeline.any { it.contains("8 à 17 ans") })
    }

    @Test
    fun pixelIdentityIdIsDeterministicAndLegacyArchiveKeepsCoreIdentity() {
        val c = GameEngine.newCampaign(919191L)
        val s = UltimateStore.fallback(c)
        val a = stablePixelIdentityId(c, s)
        val b = stablePixelIdentityId(c, s)
        assertEquals(a, b)
        assertTrue(a.startsWith("ID-"))

        val finished = c.copy(
            turn = 196,
            alias = "Vector",
            powerFamily = "Énergie",
            flags = setOf("POWER_REVEALED", "ALIAS_CHOSEN")
        )
        val record = LegacyRecord.from(finished, s)
        val decoded = LegacyRecord.decode(record.encode())

        assertEquals(record.identityId, decoded.identityId)
        assertEquals(record.powerFamily, decoded.powerFamily)
        assertEquals(record.finalAge, decoded.finalAge)
        assertEquals(record.bodyBuild, decoded.bodyBuild)
        assertEquals(record.skinTone, decoded.skinTone)
        assertEquals(record.hair, decoded.hair)
        assertEquals(record.alignment, decoded.alignment)
    }

    @Test
    fun agingMilestonesAndMortalityRemainCoherent() {
        val base = GameEngine.newCampaign(606060L)
        assertEquals(8, base.copy(turn = 0).age)
        assertEquals(18, base.copy(turn = 10).age)
        assertEquals(20, base.copy(turn = 16).age)
        assertEquals(30, base.copy(turn = 56).age)
        assertEquals(40, base.copy(turn = 96).age)
        assertEquals(50, base.copy(turn = 136).age)
        assertEquals(60, base.copy(turn = 176).age)
        assertEquals(65, base.copy(turn = 196).age)
        assertTrue(base.copy(turn = 196).finished)
        assertTrue(base.copy(health = 0).finished)
        assertFalse(base.copy(turn = 195, health = 1).finished)
    }

    @Test
    fun creatorDraftStartsWithAgeSafePixelIdentity() {
        repeat(40) { index ->
            val seed = 700_000L + index
            val blueprint = GameEngine.randomBlueprint(seed)
            val draft = UltimateCatalog.randomDraft(seed, blueprint)
            assertEquals("Aucune", draft.facialHair)
            assertEquals(-1, draft.libraryFaceIndex)
            val campaign = GameEngine.newCampaign(seed, draft.blueprint)
            val state = UltimateStore.create(campaign, draft)
            assertEquals(8, campaign.age)
            assertEquals("Aucune", state.facialHair)
            assertEquals(-1, state.libraryFaceIndex)
        }
    }

    @Test
    fun endOfLifeClassificationCoversRetirementDeathAndSacrifice() {
        val base = GameEngine.newCampaign(808080L).copy(
            powerFamily = "Énergie",
            flags = setOf("POWER_REVEALED", "ALIAS_CHOSEN"),
            prestige = 50
        )
        assertEquals("Retraite", legacyEndingKind(base.copy(turn = 196, health = 100)))
        assertEquals("Mort en activité", legacyEndingKind(base.copy(health = 0, lastApproach = "ORDER")))
        assertEquals("Sacrifice", legacyEndingKind(base.copy(health = 0, lastApproach = "CARE", prestige = 60)))
        assertEquals(
            "Victoire puis retraite",
            legacyEndingKind(base.copy(turn = 196, influence = 950, prestige = 80, morality = 60))
        )
    }

    @Test
    fun systemicAlignmentDistinguishesFourCareerProfiles() {
        val base = GameEngine.newCampaign(414141L)
        assertEquals("Neutre", base.copy(morality = 0, opinion = 0, fear = 0, civilianCasualties = 0).alignmentLabel)
        assertEquals("Héros", base.copy(morality = 60, opinion = 40, fear = 10, civilianCasualties = 0).alignmentLabel)
        assertEquals("Anti-héros", base.copy(morality = 15, opinion = -20, fear = 50, governmentStanding = -40).alignmentLabel)
        assertEquals("Vilain", base.copy(morality = -60, opinion = -50, fear = 80, civilianCasualties = 10).alignmentLabel)
    }

    @Test
    fun pixelStatureChangesBodyProportionsAcrossAges() {
        assertTrue(pixelLegHeight(8, "Petite") < pixelLegHeight(8, "Grande"))
        assertTrue(pixelLegHeight(16, "Petite") < pixelLegHeight(16, "Grande"))
        assertTrue(pixelLegHeight(30, "Petite") < pixelLegHeight(30, "Grande"))
        assertEquals(4, pixelLegHeight(8, "Petite"))
        assertEquals(7, pixelLegHeight(30, "Grande"))
    }

    @Test
    fun pixelAgeTiersProgressThroughAdultLife() {
        assertEquals(0, pixelAgeTier(8))
        assertEquals(1, pixelAgeTier(20))
        assertEquals(2, pixelAgeTier(40))
        assertEquals(3, pixelAgeTier(55))
        assertEquals(4, pixelAgeTier(65))
    }

    private fun assertStateBounds(c: Campaign) {
        assertTrue(c.morality in -100..100)
        assertTrue(c.opinion in -100..100)
        assertTrue(c.fear in 0..100)
        assertTrue(c.power in 0..100)
        assertTrue(c.control in 0..100)
        assertTrue(c.health in 0..100)
        assertTrue(c.identityExposure in 0..100)
        assertTrue(c.familyBond in 0..100)
        assertTrue(c.scope in Scope.entries)
    }
}
