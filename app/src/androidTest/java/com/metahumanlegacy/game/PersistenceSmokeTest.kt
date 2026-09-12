package com.metahumanlegacy.game

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersistenceSmokeTest {
    @Test
    fun activeCampaignUltimateStateAnnualActionsAndDeepLifeRoundTrip() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        clearCampaignV4(context)

        val seed = 8_181_817L
        val blueprint = GameEngine.randomBlueprint(seed)
        val draft = UltimateCatalog.randomDraft(seed, blueprint).copy(
            bodyBuild = "Robuste",
            stature = "Grande",
            skinTone = "Foncé",
            faceShape = "Carré",
            hair = "Tresses",
            hairColor = "Noir",
            facialHair = "Aucune",
            eyes = "Verts",
            civilianStyle = "Créatif",
            accessory = "Lunettes"
        )
        val campaign = GameEngine.newCampaign(seed, blueprint)
        val ultimate = UltimateStore.create(campaign, draft)
        val annual = AnnualActionState.fresh(campaign)
        val baseDeep = DeepLifeDirector.bootstrap(campaign, ultimate)
        val deep = baseDeep.copy(
            memories = baseDeep.memories + CharacterMemory("m1", 0, 8, "friend", "ORIGIN", "Une promesse d'enfance", "ATTACHEMENT", 8, setOf("SECRET")),
            perception = baseDeep.perception.copy(district = 22, criminalFear = 41),
            injuries = listOf(PersistentInjury("i1", "épaule", 6, "Le viaduc", 31, true, 45)),
            identityEvidence = listOf(IdentityEvidence("e1", "VIDÉO", 35, "journalist", 20, "Une silhouette concorde.")),
            opportunities = listOf(Opportunity("o1", "Voir un proche", "RELATION", 3, 4, "friend")),
            personality = baseDeep.personality + ("LOYAL" to 37)
        )

        saveCampaignV4(context, campaign)
        UltimateStore.save(context, ultimate)
        AnnualActionPersistence.save(context, annual)
        DeepLifePersistence.save(context, deep)

        val loadedCampaign = loadCampaignV4(context)
        assertNotNull(loadedCampaign)
        assertEquals(seed, loadedCampaign!!.seed)
        assertEquals(8, loadedCampaign.age)

        val loadedUltimate = UltimateStore.load(context, loadedCampaign)
        assertEquals(seed, loadedUltimate.seed)
        assertEquals(ultimate.bodyBuild, loadedUltimate.bodyBuild)
        assertEquals(ultimate.stature, loadedUltimate.stature)
        assertEquals(ultimate.skinTone, loadedUltimate.skinTone)
        assertEquals(ultimate.faceShape, loadedUltimate.faceShape)
        assertEquals(ultimate.hair, loadedUltimate.hair)
        assertEquals(ultimate.hairColor, loadedUltimate.hairColor)
        assertEquals(ultimate.facialHair, loadedUltimate.facialHair)
        assertEquals(ultimate.eyes, loadedUltimate.eyes)
        assertEquals(ultimate.civilianStyle, loadedUltimate.civilianStyle)
        assertEquals(ultimate.accessory, loadedUltimate.accessory)

        val loadedAnnual = AnnualActionPersistence.load(context, loadedCampaign)
        assertEquals(annual.turn, loadedAnnual.turn)
        assertEquals(annual.used, loadedAnnual.used)

        val loadedDeep = DeepLifePersistence.load(context, loadedCampaign, loadedUltimate)
        assertEquals(seed, loadedDeep.seed)
        assertEquals(1, loadedDeep.schemaVersion)
        assertTrue(loadedDeep.memories.any { it.id == "m1" && it.weight == 8 })
        assertEquals(22, loadedDeep.perception.district)
        assertEquals(41, loadedDeep.perception.criminalFear)
        assertEquals("épaule", loadedDeep.injuries.single().bodyPart)
        assertTrue(loadedDeep.injuries.single().chronic)
        assertEquals("VIDÉO", loadedDeep.identityEvidence.single().kind)
        assertEquals("Voir un proche", loadedDeep.opportunities.single().title)
        assertEquals(37, loadedDeep.personality["LOYAL"])
        assertTrue(loadedDeep.relationships.isNotEmpty())

        UltimateStore.clear(context, seed)
        AnnualActionPersistence.clear(context, seed)
        DeepLifePersistence.clear(context, seed)
        clearCampaignV4(context)
    }
}
