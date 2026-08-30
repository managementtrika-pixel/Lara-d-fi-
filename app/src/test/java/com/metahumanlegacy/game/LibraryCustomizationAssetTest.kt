package com.metahumanlegacy.game

import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryCustomizationAssetTest {

    @Test
    fun facePresetsOnlyUseSupportedStableOptions() {
        assertTrue(LibraryCustomizationCatalog.facePresets.isNotEmpty())
        assertTrue(LibraryCustomizationCatalog.FACE_COLUMNS == 8)
        assertTrue(LibraryCustomizationCatalog.FACE_ROWS == 6)
        assertTrue(LibraryCustomizationCatalog.facePresets.size == 48)
        assertTrue(LibraryCustomizationCatalog.facePresets.map { it.atlasIndex }.distinct().size == 48)
        LibraryCustomizationCatalog.facePresets.forEach { preset ->
            assertTrue(preset.skinTone in UltimateCatalog.skinTones)
            assertTrue(preset.faceShape in UltimateCatalog.faceShapes)
            assertTrue(preset.hair in UltimateCatalog.hairs)
            assertTrue(preset.hairColor in UltimateCatalog.hairColors)
            assertTrue(preset.facialHair in UltimateCatalog.facialHairs)
            assertTrue(preset.eyes in UltimateCatalog.eyes)
            assertTrue(preset.civilianStyle in UltimateCatalog.civilianStyles)
            assertTrue(preset.atlasIndex in 0 until LibraryCustomizationCatalog.FACE_COLUMNS * LibraryCustomizationCatalog.FACE_ROWS)
        }
    }

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

    @Test
    fun illustratedFaceSelectionUsesOneExactAtlasCell() {
        val blueprint = CharacterBlueprint(
            firstName = "Test", lastName = "Avatar", pronouns = "iel",
            city = GameEngine.cities.first(), district = GameEngine.districts.first(),
            socialBackground = GameEngine.socialBackgrounds.first(), motivation = GameEngine.motivations.first(),
            civilianPath = GameEngine.civilianPaths.first(), temperament = GameEngine.temperaments.first()
        )
        val draft = UltimateCreationDraft(blueprint = blueprint, hair = "Boucles", hairColor = "Roux")
        val preset = LibraryCustomizationCatalog.facePresets[7]
        val applied = preset.apply(draft)
        assertTrue(applied.libraryFaceIndex == preset.atlasIndex)
        assertTrue(applied.hair == "Boucles")
        assertTrue(applied.hairColor == "Roux")
        assertTrue(applied.skinTone == draft.skinTone)
        assertTrue(applied.faceShape == draft.faceShape)
        assertTrue(preset.matches(applied))
    }


    @Test
    fun illustratedFaceCatalogCoversWholeSheet() {
        assertTrue(LibraryCustomizationCatalog.facePresets.map { it.atlasIndex }.sorted() == (0 until 48).toList())
    }
}
