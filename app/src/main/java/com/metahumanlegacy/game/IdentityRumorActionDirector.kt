package com.metahumanlegacy.game

/**
 * Handles the deliberate use of free time to contain identity rumors without overloading the
 * generic LifeSimulationDirector with media-specific logic.
 */
internal object IdentityRumorActionDirector {
    const val TARGET_ID = "identity_rumor"

    fun available(c: Campaign, state: LifeSimulationState): Boolean =
        c.powerRevealed &&
            state.civil.freeMoments > 0 &&
            (state.secretIdentity.exposure >= 25 || state.secretIdentity.activeRumors.isNotEmpty() || state.secretIdentity.evidenceIds.isNotEmpty())

    fun action(): LifeAction = LifeAction(
        type = LifeActionType.INVESTIGATE,
        targetId = TARGET_ID,
        label = "Contenir les rumeurs sur mon identité"
    )

    fun handles(action: LifeAction): Boolean =
        action.type == LifeActionType.INVESTIGATE && action.targetId == TARGET_ID

    fun perform(c: Campaign, state: LifeSimulationState, action: LifeAction): LifeActionResult? {
        if (!handles(action)) return null
        if (!c.powerRevealed) return LifeActionResult(state, "Rien à contenir", "Ton identité métahumaine n'existe pas encore publiquement.")
        if (state.civil.freeMoments <= 0) return LifeActionResult(state, "Plus de temps", "Cette année est déjà remplie.")
        if (!available(c, state)) return LifeActionResult(state, "Pression faible", "Aucune rumeur assez structurée ne mérite encore d'y sacrifier du temps.")

        val threatCount = state.secretIdentity.knownBy.values.count { it == SecretKnowledge.THREATENS }
        val evidencePressure = state.secretIdentity.evidenceIds.size.coerceAtMost(6)
        val reduction = (8 - threatCount * 2 - evidencePressure / 2).coerceIn(3, 8)
        val heatReduction = (10 - threatCount * 2).coerceIn(4, 10)
        val stressCost = (6 + threatCount * 4).coerceAtMost(18)

        val civil = state.civil.copy(
            freeMoments = state.civil.freeMoments - 1,
            stress = (state.civil.stress + stressCost).coerceAtMost(100)
        )
        val identity = state.secretIdentity.copy(
            exposure = (state.secretIdentity.exposure - reduction).coerceAtLeast(0),
            activeRumors = if (state.secretIdentity.activeRumors.size <= 1) emptyList()
            else state.secretIdentity.activeRumors.drop(1)
        )
        val districts = state.districts.map { district ->
            if (district.id != "quartier") district else district.copy(
                mediaHeat = (district.mediaHeat - heatReduction).coerceAtLeast(0)
            )
        }
        val next = state.copy(
            civil = civil,
            secretIdentity = identity,
            districts = districts,
            actionLog = (state.actionLog + "Identité · rumeurs contenues (-$reduction exposition)").takeLast(80)
        )

        val headline = if (threatCount > 0) "Tu gagnes du temps, pas le silence" else "La piste se refroidit"
        val detail = if (threatCount > 0) {
            "Tu coupes plusieurs recoupements publics, mais quelqu'un qui connaît déjà ton secret reste une menace. Exposition -$reduction, pression média -$heatReduction, stress +$stressCost."
        } else {
            "Tu corriges des horaires, brouilles des habitudes et fais retomber l'attention sans effacer les preuves déjà existantes. Exposition -$reduction, pression média -$heatReduction, stress +$stressCost."
        }
        return LifeActionResult(next, headline, detail)
    }
}
