package com.metahumanlegacy.game

internal data class LifeWorldBridgeUpdate(
    val campaign: Campaign,
    val ultimate: UltimateState
)

/**
 * Keeps the free-time life simulation and the authoritative campaign/world state coherent.
 * Life actions are mirrored into the main world using deltas, while narrative/world consequences
 * are reconciled back into life simulation without erasing simulation-only history.
 */
internal object LifeWorldStateBridge {
    fun afterLifeAction(
        campaign: Campaign,
        ultimate: UltimateState,
        before: LifeSimulationState,
        after: LifeSimulationState
    ): LifeWorldBridgeUpdate {
        if (before == after) return LifeWorldBridgeUpdate(campaign, ultimate)

        val controlGain = after.powerRules.control - before.powerRules.control
        val exposureGain = after.secretIdentity.exposure - before.secretIdentity.exposure
        val overloadDelta = after.powerRules.overload - before.powerRules.overload

        val nextCampaign = campaign.copy(
            control = (campaign.control + controlGain).coerceIn(0, 100),
            identityExposure = (campaign.identityExposure + exposureGain).coerceIn(0, 100)
        )

        var nextUltimate = ultimate.copy(
            powerStrain = (ultimate.powerStrain + overloadDelta).coerceIn(0, 100)
        )

        val beforeDistrict = before.districts.firstOrNull { it.id == "quartier" }
        val afterDistrict = after.districts.firstOrNull { it.id == "quartier" }
        if (beforeDistrict != null && afterDistrict != null && beforeDistrict != afterDistrict && nextUltimate.districts.isNotEmpty()) {
            val index = nextUltimate.districts.indexOfFirst { it.name == campaign.district }.let { if (it >= 0) it else 0 }
            val current = nextUltimate.districts[index]
            val safetyDelta = afterDistrict.safety - beforeDistrict.safety
            val crimeControlDelta = afterDistrict.criminalControl - beforeDistrict.criminalControl
            val trustDelta = afterDistrict.localTrust - beforeDistrict.localTrust
            val damageDelta = afterDistrict.damage - beforeDistrict.damage
            val mediaHeatDelta = afterDistrict.mediaHeat - beforeDistrict.mediaHeat

            val crimeDelta = crimeControlDelta - safetyDelta / 2
            val sentimentDelta = trustDelta - mediaHeatDelta / 3
            val reconstructionDelta = if (safetyDelta > 0 && damageDelta <= 0) safetyDelta / 4 else 0
            val nextDistrict = current.copy(
                crime = (current.crime + crimeDelta).coerceIn(0, 100),
                damage = (current.damage + damageDelta).coerceIn(0, 100),
                sentiment = (current.sentiment + sentimentDelta).coerceIn(-100, 100),
                reconstruction = (current.reconstruction + reconstructionDelta).coerceIn(0, 100)
            )
            val districts = nextUltimate.districts.toMutableList().also { it[index] = nextDistrict }
            val conditionDelta = (safetyDelta - damageDelta - crimeControlDelta.coerceAtLeast(0)) / 2
            nextUltimate = nextUltimate.copy(
                districts = districts,
                cityCondition = (nextUltimate.cityCondition + conditionDelta).coerceIn(0, 100),
                mediaFrame = when {
                    nextCampaign.identityExposure >= 75 -> "Identité au cœur des spéculations"
                    mediaHeatDelta >= 4 && nextUltimate.mediaFrame == "Inconnu" && nextCampaign.powerRevealed -> "Présence métahumaine suivie"
                    else -> nextUltimate.mediaFrame
                }
            )
        }

        return LifeWorldBridgeUpdate(nextCampaign, nextUltimate)
    }

    /**
     * Reconciles consequences produced by authored story/world systems back into the life layer.
     * This is intentionally absolute for facts the main world owns (crime/damage/exposure), while
     * simulation-only fields such as promises, availability and technique proficiency are preserved.
     */
    fun syncLifeFromWorld(
        campaign: Campaign,
        ultimate: UltimateState,
        life: LifeSimulationState
    ): LifeSimulationState {
        val worldDistrict = ultimate.districts.firstOrNull { it.name == campaign.district }
            ?: ultimate.districts.firstOrNull()

        val districts = if (worldDistrict == null) life.districts else {
            val targetSafety = (
                100 - worldDistrict.crime - worldDistrict.damage / 2 + worldDistrict.reconstruction / 2
            ).coerceIn(0, 100)
            val targetTrust = worldDistrict.sentiment.coerceIn(0, 100)
            val targetHeat = maxOf(
                life.districts.firstOrNull { it.id == "quartier" }?.mediaHeat ?: 0,
                campaign.identityExposure / 2
            ).coerceIn(0, 100)
            val existing = life.districts.firstOrNull { it.id == "quartier" }
                ?: DistrictLifeState("quartier")
            val syncedHome = existing.copy(
                safety = targetSafety,
                damage = worldDistrict.damage.coerceIn(0, 100),
                localTrust = targetTrust,
                criminalControl = worldDistrict.crime.coerceIn(0, 100),
                mediaHeat = targetHeat
            )
            if (life.districts.any { it.id == "quartier" }) {
                life.districts.map { if (it.id == "quartier") syncedHome else it }
            } else {
                listOf(syncedHome) + life.districts
            }
        }

        return life.copy(
            powerRules = life.powerRules.copy(
                control = maxOf(life.powerRules.control, campaign.control).coerceIn(0, 100),
                overload = maxOf(life.powerRules.overload, ultimate.powerStrain).coerceIn(0, 100)
            ),
            secretIdentity = life.secretIdentity.copy(
                exposure = maxOf(life.secretIdentity.exposure, campaign.identityExposure).coerceIn(0, 100)
            ),
            districts = districts
        )
    }
}
