package com.metahumanlegacy.game

import org.junit.Assert.assertEquals
import org.junit.Test

class AvatarContinuityTest {
    private fun blueprint() = CharacterBlueprint(
        firstName = "Nova",
        lastName = "Vesper",
        pronouns = "elle",
        city = "Vesper",
        district = "Centre",
        socialBackground = "Classe moyenne",
        motivation = "Protéger les miens",
        civilianPath = "Vie ordinaire",
        temperament = "Prudent"
    )

    @Test
    fun creatorAppearanceSurvivesCampaignCreationExactly() {
        val draft = UltimateCreationDraft(
            blueprint = blueprint(),
            bodyBuild = "Fin",
            stature = "Grande",
            skinTone = "Mat",
            faceShape = "Anguleux",
            hair = "Tresses",
            hairColor = "Roux",
            facialHair = "Aucune",
            eyes = "Verts",
            civilianStyle = "Créatif",
            accessory = "Lunettes",
            libraryFaceIndex = 7
        )
        val c = GameEngine.newCampaign(737373L, draft.blueprint)
        val state = UltimateStore.create(c, draft)

        assertEquals(draft.appearanceFingerprint(), state.appearanceFingerprint())
    }

    @Test
    fun differentNarrativeSeedDoesNotRerollCreatedAppearance() {
        val draft = UltimateCreationDraft(
            blueprint = blueprint(),
            bodyBuild = "Robuste",
            stature = "Petite",
            skinTone = "Foncé",
            faceShape = "Carré",
            hair = "Boucles",
            hairColor = "Noir",
            eyes = "Très sombres",
            civilianStyle = "Street sobre",
            accessory = "Chaîne",
            libraryFaceIndex = 3
        )
        val first = UltimateStore.create(GameEngine.newCampaign(111L, draft.blueprint), draft)
        val second = UltimateStore.create(GameEngine.newCampaign(999999L, draft.blueprint), draft)

        assertEquals(first.appearanceFingerprint(), second.appearanceFingerprint())
        assertEquals(draft.appearanceFingerprint(), first.appearanceFingerprint())
    }

    @Test
    fun costumeEvolutionDoesNotMutateCivilianPixelIdentity() {
        val draft = UltimateCreationDraft(
            blueprint = blueprint(),
            faceShape = "Fin",
            hair = "Undercut",
            hairColor = "Blond",
            eyes = "Bleus",
            civilianStyle = "Vintage",
            accessory = "Montre",
            libraryFaceIndex = 5
        )
        val c = GameEngine.newCampaign(12345L, draft.blueprint)
        val initial = UltimateStore.create(c, draft)
        val hero = initial.copy(
            heroPresentation = "Intimidant",
            costumePalette = "Noir / argent",
            maskStyle = "Masque intégral",
            costumeEra = 4
        )

        assertEquals(initial.appearanceFingerprint(), hero.appearanceFingerprint())
    }
}
