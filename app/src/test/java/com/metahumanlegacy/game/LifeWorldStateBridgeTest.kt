package com.metahumanlegacy.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeWorldStateBridgeTest {
    private fun campaign() = GameEngine.newCampaign(808080L).copy(
        district = "Centre",
        turn = 24,
        control = 40,
        identityExposure = 20,
        flags = setOf("POWER_REVEALED", "ALIAS_CHOSEN")
    )

    private fun ultimate(c: Campaign) = UltimateStore.fallback(c).copy(
        cityCondition = 60,
        powerStrain = 15,
        districts = listOf(UltimateDistrict(name = c.district, sentiment = 5, crime = 45, damage = 8))
    )

    private fun life(
        control: Int = 40,
        overload: Int = 10,
        exposure: Int = 10,
        safety: Int = 50,
        crime: Int = 20,
        trust: Int = 0,
        heat: Int = 0
    ) = LifeSimulationState(
        powerRules = PowerRulesState(control = control, overload = overload),
        secretIdentity = IdentitySecretState(exposure = exposure),
        districts = listOf(DistrictLifeState("quartier", safety = safety, criminalControl = crime, localTrust = trust, mediaHeat = heat))
    )

    @Test
    fun trainingControlReachesAuthoritativeCampaign() {
        val c = campaign()
        val update = LifeWorldStateBridge.afterLifeAction(c, ultimate(c), life(), life(control = 46))
        assertEquals(46, update.campaign.control)
    }

    @Test
    fun identityExposureReachesAuthoritativeCampaign() {
        val c = campaign()
        val update = LifeWorldStateBridge.afterLifeAction(c, ultimate(c), life(), life(exposure = 16))
        assertEquals(26, update.campaign.identityExposure)
    }

    @Test
    fun patrolImprovesMainDistrictInsteadOfOnlyLifeSubscreen() {
        val c = campaign()
        val before = life()
        val after = life(safety = 54, crime = 17, trust = 2, heat = 2)
        val update = LifeWorldStateBridge.afterLifeAction(c, ultimate(c), before, after)
        val district = update.ultimate.districts.first()

        assertTrue(district.crime < 45)
        assertTrue(district.sentiment > 5)
        assertTrue(update.ultimate.cityCondition > 60)
    }

    @Test
    fun overloadDeltaCanRiseAndFallInMainWorld() {
        val c = campaign()
        val u = ultimate(c)
        val raised = LifeWorldStateBridge.afterLifeAction(c, u, life(overload = 10), life(overload = 18)).ultimate
        assertEquals(23, raised.powerStrain)
        val recovered = LifeWorldStateBridge.afterLifeAction(c, raised, life(overload = 18), life(overload = 8)).ultimate
        assertEquals(13, recovered.powerStrain)
    }

    @Test
    fun unchangedLifeStateIsIdempotent() {
        val c = campaign()
        val u = ultimate(c)
        val same = life()
        val update = LifeWorldStateBridge.afterLifeAction(c, u, same, same)
        assertEquals(c, update.campaign)
        assertEquals(u, update.ultimate)
    }
}
