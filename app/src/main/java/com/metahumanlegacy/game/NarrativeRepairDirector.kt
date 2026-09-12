package com.metahumanlegacy.game

/**
 * Final safety net for exhausted authored careers.
 * The repository can legitimately run out of causal arcs during very long lives; in that case
 * we must not resurrect an arc that is already marked complete just to fill a turn.
 */
internal object NarrativeRepairDirector {
    fun repair(c: Campaign, event: EventNode): EventNode {
        val thread = event.threadId
        val resurrected = thread != null && "${thread}_COMPLETE" in c.flags
        return if (resurrected) lifeInterlude(c) else event
    }

    private fun lifeInterlude(c: Campaign): EventNode {
        val variants = listOf(
            Triple("UNE SEMAINE SANS CATASTROPHE", "Pour une fois, aucune alerte ne décide de ton agenda. Le vide te force à regarder ce que ta vie est devenue quand personne ne réclame ton pouvoir.", "VIE"),
            Triple("CEUX QUI ONT CONTINUÉ SANS TOI", "Des gens que tu as connus ont avancé, vieilli, déménagé ou cessé de t'attendre. Tu disposes d'un peu de temps avant que le monde recommence à crier.", "RELATION"),
            Triple("LE QUARTIER UN MATIN ORDINAIRE", "Tu traverses un endroit que tes anciennes interventions ont changé. Certains dégâts ont disparu, certaines habitudes sont restées.", "CIVIL"),
            Triple("TON CORPS AVANT LE COSTUME", "Avant de repartir, un geste banal te rappelle les années accumulées. L'expérience apprend à compenser beaucoup de choses, pas à les effacer.", "SANTÉ"),
            Triple("UNE DEMANDE QUI N'A RIEN D'HÉROÏQUE", "Quelqu'un te demande du temps, pas une intervention. Dire oui ne sauvera pas la ville. Dire non ne fera pas la une. C'est précisément pour ça que la décision compte.", "RELATION"),
            Triple("CE QUE TON NOM EST DEVENU", "Ton alias circule dans des conversations auxquelles tu n'assistes pas. Certains y voient une protection, d'autres une menace, d'autres encore une époque qui commence à passer.", "MÉDIAS"),
            Triple("APRÈS LA VICTOIRE", "Une crise ancienne est terminée. Ce qui reste, ce sont les conséquences lentes : réparations, rancunes, habitudes, dettes et gens qui ont appris à vivre autrement.", "HÉRITAGE"),
            Triple("QUI RÉPONDRA APRÈS TOI ?", "Plus les années passent, moins la vraie question est de savoir si tu peux encore intervenir. Elle devient : qui saura agir quand tu décideras enfin de ne pas le faire ?", "HÉRITAGE")
        )
        val index = positiveMod(mix(c.seed, c.turn * 1777L + c.age), variants.size)
        val (title, text, category) = variants[index]
        val stakes = if (c.age >= 60) 2 else 1
        return EventNode(
            id = "LIFE_INTERLUDE_${index}_${c.turn}",
            title = title,
            text = text,
            choices = listOf(
                Choice("Donner ce temps à quelqu'un qui compte", moral = 1, relationDelta = 2, approach = "CARE", stakes = stakes, sourceCategory = category, deferredHook = true),
                Choice("Mettre de l'ordre dans ce qui dépend encore de toi", prestige = 1, approach = "ORDER", stakes = stakes, sourceCategory = category),
                Choice("Regarder ce qui a réellement changé et pourquoi", opinion = 1, approach = "TRUTH", stakes = stakes, sourceCategory = category, deferredHook = true),
                Choice("Préparer la prochaine étape de ton héritage", prestige = 1, impact = 1, risk = if (c.age >= 65) 2 else 1, approach = "ASCEND", stakes = stakes, sourceCategory = category)
            ),
            category = category,
            provocation = "VIE QUI CONTINUE",
            stakes = stakes,
            threadId = null,
            threadStage = 0,
            kind = "QUIET"
        )
    }
}
