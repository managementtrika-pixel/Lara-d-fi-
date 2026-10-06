package com.metahumanlegacy.game

/** The same transition is exercised by the UI and complete-career regression tests. */
internal object FinalGameRuntime {
    fun resolve(raw: FinalSession, event: EventNode, choice: Choice): FinalSession? {
        val s = FinalSessionPersistence.normalized(raw)
        val c = s.campaign
        if (!FinalGameRules.canChoose(c, c, s.outcome)) return null
        val expected = FinalGameRules.event(c, s.ultimate, s.annual, s.deep)
        if (event != expected || choice !in expected.choices) return null
        val result = UltimateGameEngine.resolve(c, s.ultimate, event, choice)
        val deepUpdate = DeepLifeRuntime.afterChoice(c, result.campaign, event, choice, s.deep)
        val worldUpdate = DeepWorldDirector.afterChoice(result.campaign, result.state, deepUpdate.state, event, choice)
        val generation = GenerationalDirector.afterChoice(worldUpdate.campaign, worldUpdate.ultimate, worldUpdate.deep, event, choice)
        val revealed = DeepLifeDirector.revealPower(generation.campaign, generation.deep)
        val life = LifeSimulationDirector.synced(generation.campaign, revealed,
            revealed.lifeSimulation ?: LifeSimulationDirector.bootstrap(generation.campaign, revealed))
        val deep = LifeSimulationDirector.mergedIntoDeep(revealed, life)
        val outcome = buildString {
            append(result.outcome)
            if (deepUpdate.echo.isNotBlank()) append("\n\nTRACE DE VIE\n${deepUpdate.echo}")
            if (worldUpdate.echo.isNotBlank()) append("\n\nMONDE QUI RÉAGIT\n${worldUpdate.echo}")
            if (generation.echo.isNotBlank()) append("\n\nPASSAGE DE RELAIS\n${generation.echo}")
        }
        return FinalSessionPersistence.normalized(FinalSession(generation.campaign, generation.ultimate,
            s.annual.synced(generation.campaign), deep, outcome))
    }
}
