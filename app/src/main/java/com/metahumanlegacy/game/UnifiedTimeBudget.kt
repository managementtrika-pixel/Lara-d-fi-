package com.metahumanlegacy.game

/** Single guard used by life-simulation and career UI while legacy saves converge to the 3-slot model. */
internal object UnifiedTimeBudget {
    fun remaining(life: LifeSimulationState, annual: AnnualActionState): Int =
        minOf(life.civil.freeMoments, annual.remaining, ANNUAL_ACTION_LIMIT).coerceAtLeast(0)

    fun hasTime(life: LifeSimulationState, annual: AnnualActionState): Boolean =
        remaining(life, annual) > 0
}
