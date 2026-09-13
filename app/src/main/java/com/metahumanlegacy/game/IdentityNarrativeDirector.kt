package com.metahumanlegacy.game

/** Injects identity-management decisions only when accumulated evidence makes them narratively real. */
internal object IdentityNarrativeDirector {
    private const val FLAG = "identity_pressure:contain"

    fun enrich(c: Campaign, deep: DeepLifeState, event: EventNode): EventNode {
        if (!c.powerRevealed || event.kind in setOf("FORMATIVE", "AWAKENING") || event.stakes < 3) return event
        if (event.choices.any { it.flag == FLAG }) return event

        val raw = deep.lifeSimulation ?: return event
        val identity = IdentityPressureDirector.sync(c, deep, raw).secretIdentity
        val highPressure = identity.exposure >= 45 || identity.activeRumors.size >= 2 || identity.evidenceIds.size >= 2
        if (!highPressure) return event

        val threatening = identity.knownBy.values.any { it == SecretKnowledge.THREATENS }
        val choice = Choice(
            label = if (threatening) "Brouiller les pistes avant que quelqu'un parle" else "Détourner l'attention avant d'agir",
            moral = 0,
            prestige = if (threatening) -1 else 0,
            opinion = 0,
            fear = 0,
            power = -1,
            impact = -1,
            risk = if (threatening) 5 else 3,
            approach = "TRUTH",
            stakes = event.stakes,
            sourceCategory = "IDENTITY",
            identityDelta = if (threatening) -3 else -2,
            deferredHook = threatening,
            flag = FLAG
        )

        val text = buildString {
            append(event.text)
            append("\n\nTon identité civile n'est plus hors de cette scène : ")
            append(
                if (threatening) "quelqu'un qui connaît ton secret pourrait transformer les soupçons en arme."
                else "les rumeurs et les preuves accumulées obligent désormais à penser aussi à ce que tu laisses derrière toi."
            )
        }
        return event.copy(text = text, choices = (event.choices + choice).take(7))
    }

    fun isIdentityChoice(choice: Choice): Boolean = choice.flag == FLAG
}
