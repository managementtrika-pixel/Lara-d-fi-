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
        powerRules = PowerRulesState(
            control = control,
            overload = overload,
            techniques = listOf(TechniqueState("projector_zone", "Zone contrôlée", 40, unlocked = true, proficiency = 61))
        ),
        secretIdentity = IdentitySecretState(exposure = exposure),
        relationshipLives = listOf(RelationshipLifeState("friend", closeness = 52, promises = listOf("Rester présent"))),
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

    @Test
    fun storyCrimeAndDamageFlowBackIntoLifeSimulation() {
        val c = campaign()
        val world = ultimate(c).copy(
            districts = listOf(UltimateDistrict(name = c.district, sentiment = -12, crime = 68, damage = 34, reconstruction = 4))
        )
        val synced = LifeWorldStateBridge.syncLifeFromWorld(c, world, life(safety = 80, crime = 8, trust = 60))
        val district = synced.districts.first { it.id == "quartier" }

        assertEquals(68, district.criminalControl)
        assertEquals(34, district.damage)
        assertEquals(0, district.localTrust)
        assertTrue(district.safety < 40)
    }

    @Test
    fun authoritativeControlExposureAndStrainFlowBackIntoLifeSimulation() {
        val c = campaign().copy(control = 73, identityExposure = 66)
        val u = ultimate(c).copy(powerStrain = 58)
        val synced = LifeWorldStateBridge.syncLifeFromWorld(c, u, life(control = 45, exposure = 22, overload = 11))

        assertEquals(73, synced.powerRules.control)
        assertEquals(66, synced.secretIdentity.exposure)
        assertEquals(58, synced.powerRules.overload)
    }

    @Test
    fun worldReconciliationPreservesSimulationOnlyProgress() {
        val c = campaign()
        val before = life()
        val synced = LifeWorldStateBridge.syncLifeFromWorld(c, ultimate(c), before)

        assertEquals(61, synced.powerRules.techniques.first().proficiency)
        assertEquals(52, synced.relationshipLives.first().closeness)
        assertEquals(listOf("Rester présent"), synced.relationshipLives.first().promises)
    }

    @Test
    fun worldReconciliationNeverDowngradesHigherLifeMasteryOrExposure() {
        val c = campaign().copy(control = 42, identityExposure = 18)
        val u = ultimate(c).copy(powerStrain = 12)
        val synced = LifeWorldStateBridge.syncLifeFromWorld(c, u, life(control = 70, exposure = 55, overload = 48))

        assertEquals(70, synced.powerRules.control)
        assertEquals(55, synced.secretIdentity.exposure)
        assertEquals(48, synced.powerRules.overload)
    }

    @Test
    fun preAwakeningLifeHistoryIsNeverOverwrittenByFutureWorldState() {
        val formative = GameEngine.newCampaign(808080L).copy(turn = 0, flags = emptySet())
        val before = life(safety = 67, crime = 11, trust = 22, exposure = 19)
        val world = ultimate(formative).copy(
            districts = listOf(UltimateDistrict(name = formative.district, sentiment = -60, crime = 90, damage = 70))
        )

        assertEquals(before, LifeWorldStateBridge.syncLifeFromWorld(formative, world, before))
    }
}
