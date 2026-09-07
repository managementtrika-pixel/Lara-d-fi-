package com.metahumanlegacy.game

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
    fun costumePresetsAreAwakeningOnlyAndCatalogSafe() {
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
    fun onlyCoherentCityCellsAreExposed() {
        // The curated city atlas has 12 cells, but two non-urban fantasy/cosmic cells are deliberately
        // not exposed as city presets. This guards against future accidental inclusion.
        assertTrue(LibraryCustomizationCatalog.cityPresets.size == 10)
        assertTrue(LibraryCustomizationCatalog.cityPresets.all { it.atlasIndex in 0..9 })
    }

    @Test
    fun advancedCostumesStayBehindCareerEraGates() {
        val start = LibraryCustomizationCatalog.costumePresets.filter { it.minimumEra <= 1 }
        val advanced = LibraryCustomizationCatalog.costumePresets.filter { it.minimumEra >= 3 }
        assertTrue(start.map { it.name }.containsAll(listOf("Prototype", "Première identité")))
        assertTrue(advanced.isNotEmpty())
        assertTrue(advanced.none { it.minimumEra <= 1 })
    }





}
