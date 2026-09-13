package com.metahumanlegacy.game

internal data class LifeWorldBridgeUpdate(
    val campaign: Campaign,
    val ultimate: UltimateState
)

/**
 * Mirrors consequences from the free-time life simulation into the authoritative campaign/world
 * state. The bridge is delta-based so persisting the same LifeSimulationState twice cannot stack
 * the same consequence again.
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
}
