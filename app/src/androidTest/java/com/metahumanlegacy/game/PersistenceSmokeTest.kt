package com.metahumanlegacy.game

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersistenceSmokeTest {
    @Test
    fun activeCampaignUltimateStateAndAnnualActionsRoundTrip() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        clearCampaignV4(context)

        val seed = 8_181_817L
        val blueprint = GameEngine.randomBlueprint(seed)
        val draft = UltimateCatalog.randomDraft(seed, blueprint)
        val campaign = GameEngine.newCampaign(seed, blueprint)
        val ultimate = UltimateStore.create(campaign, draft)
        val annual = AnnualActionState.fresh(campaign)

        saveCampaignV4(context, campaign)
        UltimateStore.save(context, ultimate)
        AnnualActionPersistence.save(context, annual)

        val loadedCampaign = loadCampaignV4(context)
        assertNotNull(loadedCampaign)
        assertEquals(seed, loadedCampaign!!.seed)
        assertEquals(8, loadedCampaign.age)

        val loadedUltimate = UltimateStore.load(context, loadedCampaign)
        assertEquals(seed, loadedUltimate.seed)
        assertEquals(ultimate.bodyBuild, loadedUltimate.bodyBuild)
        assertEquals(ultimate.skinTone, loadedUltimate.skinTone)

        val loadedAnnual = AnnualActionPersistence.load(context, loadedCampaign)
        assertEquals(annual.turn, loadedAnnual.turn)
        assertEquals(annual.used, loadedAnnual.used)

        UltimateStore.clear(context, seed)
        AnnualActionPersistence.clear(context, seed)
        clearCampaignV4(context)
    }
}
