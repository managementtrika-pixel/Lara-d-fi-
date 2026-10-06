package com.metahumanlegacy.game

/** Rules shared by the final UI and its saved session. */
internal object FinalGameRules {
    fun canChoose(current: Campaign?, rendered: Campaign, pending: String?): Boolean =
        current != null && current.seed == rendered.seed && current.turn == rendered.turn &&
            !current.finished && !current.needsAlias && pending == null

    fun canAct(c: Campaign, pending: String?): Boolean =
        !c.finished && !c.needsAlias && pending == null && (c.turn != 10 || c.powerRevealed)

    fun synchronize(c: Campaign, rawAnnual: AnnualActionState, rawDeep: DeepLifeState): Pair<AnnualActionState, DeepLifeState> {
        val annual = rawAnnual.synced(c)
        val life = LifeSimulationDirector.synced(c, rawDeep,
            rawDeep.lifeSimulation ?: LifeSimulationDirector.bootstrap(c, rawDeep))
        val remaining = minOf(annual.remaining, life.civil.freeMoments.coerceIn(0, ANNUAL_ACTION_LIMIT))
        return annual.copy(used = ANNUAL_ACTION_LIMIT - remaining) to
            rawDeep.copy(lifeSimulation = life.copy(civil = life.civil.copy(freeMoments = remaining)))
    }

    fun event(c: Campaign, u: UltimateState, a: AnnualActionState, d: DeepLifeState): EventNode {
        val base = UltimateGameEngine.event(c, u, a)
        if (!c.powerRevealed || c.turn <= 10) return base
        return StoryTechniqueDirector.enrich(c, d,
            IdentityNarrativeDirector.enrich(c, d, LifeWorldNarrativeDirector.enrich(c, d, base)))
    }
}
