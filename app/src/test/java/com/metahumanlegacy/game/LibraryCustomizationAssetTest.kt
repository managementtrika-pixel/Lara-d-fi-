package com.metahumanlegacy.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryCustomizationAssetTest {
    @Test
    fun cityPresetsOnlyUseSupportedCityOptions() {
        assertTrue(LibraryCustomizationCatalog.cityPresets.isNotEmpty())
        LibraryCustomizationCatalog.cityPresets.forEach { preset ->
            assertTrue(preset.cityArchetype in UltimateCatalog.cityArchetypes)
            assertTrue(preset.climate in UltimateCatalog.climates)
            assertTrue(preset.architecture in UltimateCatalog.architectures)
            assertTrue(preset.cityMood in UltimateCatalog.cityMoods)
            assertTrue(preset.atlasIndex in 0 until LibraryCustomizationCatalog.CITY_COLUMNS * LibraryCustomizationCatalog.CITY_ROWS)
        }
    }

    @Test
    fun costumePresetsAreCatalogSafe() {
        assertTrue(LibraryCustomizationCatalog.costumePresets.isNotEmpty())
        LibraryCustomizationCatalog.costumePresets.forEach { preset ->
            assertTrue(preset.presentation in UltimateCatalog.heroPresentations)
            assertTrue(preset.palette in UltimateCatalog.costumePalettes)
            assertTrue(preset.mask in UltimateCatalog.maskStyles)
            assertTrue(preset.emblem in UltimateCatalog.emblems)
            assertTrue(preset.minimumEra >= 1)
            assertTrue(preset.atlasIndex in 0 until LibraryCustomizationCatalog.COSTUME_COLUMNS * LibraryCustomizationCatalog.COSTUME_ROWS)
        }
    }

    @Test
    fun proceduralCityVariantsStayStableAndUnique() {
        val presets = LibraryCustomizationCatalog.cityPresets
        assertEquals(10, presets.size)
        assertEquals((0..9).toSet(), presets.map { it.atlasIndex }.toSet())
    }

    @Test
    fun proceduralCostumeVariantsStayStableAndUnique() {
        val presets = LibraryCustomizationCatalog.costumePresets
        assertEquals(8, presets.size)
        assertEquals((0..7).toSet(), presets.map { it.atlasIndex }.toSet())
    }

    @Test
    fun advancedCostumesStayBehindCareerEraGates() {
        val start = LibraryCustomizationCatalog.costumePresets.filter { it.minimumEra <= 1 }
        val advanced = LibraryCustomizationCatalog.costumePresets.filter { it.minimumEra >= 3 }
        assertTrue(start.map { it.name }.containsAll(listOf("Prototype", "Première identité")))
        assertTrue(advanced.isNotEmpty())
        assertTrue(advanced.none { it.minimumEra <= 1 })
    }

    @Test
    fun applyingProceduralPresetsStillChangesGameplayFields() {
        val blueprint = GameEngine.randomBlueprint(31313L)
        val draft = UltimateCreationDraft(blueprint = blueprint)
        val cityPreset = LibraryCustomizationCatalog.cityPresets.last()
        val cityApplied = cityPreset.apply(draft)
        assertTrue(cityPreset.matches(cityApplied))

        val campaign = GameEngine.newCampaign(31313L, blueprint)
        val state = UltimateStore.create(campaign, draft)
        val costumePreset = LibraryCustomizationCatalog.costumePresets.last()
        val costumeApplied = costumePreset.apply(state)
        assertTrue(costumePreset.matches(costumeApplied))
    }
}
